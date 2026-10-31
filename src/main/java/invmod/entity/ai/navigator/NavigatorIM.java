package invmod.entity.ai.navigator;

import invmod.block.NexusBlockEntity;
import invmod.entity.Invader;
import invmod.entity.ai.AttackerAI;
import invmod.entity.terrain.TaskListener;
import invmod.entity.terrain.TerrainBuilder;
import invmod.entity.terrain.TerrainDigger;
import invmod.entity.terrain.TerrainModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Follows an invader path (original NavigatorIM / NavigatorEngy): walks from node to node, takes shortcuts
 * where the way is clear, digs out nodes that are blocked, climbs ladders, and lets engineers build what the
 * next node needs while holding still.
 */
public class NavigatorIM implements TaskListener {
    private static final int NODE_LIMIT = 1600;
    private final Mob mob;
    private final Invader invader;
    private final MobPathing pathing;
    private final Pathfinder pathfinder = new Pathfinder();
    private final TerrainModifier modifier;
    private final TerrainDigger digger;
    private final @Nullable TerrainBuilder builder;
    private @Nullable Path path;
    private double speed = 1.0;
    private int ticksStuck;
    private double lastDistance;
    private boolean waiting;
    private boolean nodeActionDone = true;
    private int lastResult;
    private @Nullable Vec3 holdPos;
    private float lastPathDistanceToTarget;

    public NavigatorIM(Mob mob) {
        this.mob = mob;
        this.invader = (Invader) mob;
        boolean engineer = this.invader.terrainSkill() == Invader.TerrainSkill.BUILD_LADDERS;
        this.pathing = engineer ? new EngineerPathing(mob) : new MobPathing(mob);
        this.modifier = new TerrainModifier(mob, engineer ? 2.8F : 2.0F);
        this.digger = new TerrainDigger(mob, this.modifier);
        this.builder = engineer ? new TerrainBuilder(mob, this.modifier, this.invader.buildMaterial(), this.invader.buildMaterialCost()) : null;
    }

    public @Nullable TerrainBuilder builder() {
        return this.builder;
    }

    public MobPathing pathing() {
        return this.pathing;
    }

    // ------------------------------------------------------------------ path requests

    private @Nullable AttackerAI attackerAI() {
        NexusBlockEntity nexus = this.invader.getNexus();
        return nexus == null ? null : nexus.getAttackerAI();
    }

    /** Plans a path to the target and starts following it. Returns false when not even a partial path exists. */
    public boolean moveTo(BlockPos target, float targetRadius, double speed) {
        BlockPos start = this.startPos();
        this.pathing.setGoal(target);
        float range = 12.0F + (float) Math.sqrt(start.distSqr(target));
        Path newPath = this.pathfinder.createPath(this.pathing, new TerrainView(this.mob.level(), this.attackerAI()), start,
                target, targetRadius, range, NODE_LIMIT);
        if (newPath == null) {
            this.clearPath();
            this.lastPathDistanceToTarget = (float) Math.sqrt(start.distSqr(target));
            return false;
        }
        this.setPath(newPath, speed);
        return true;
    }

    private BlockPos startPos() {
        if (this.pathing.sizeX() <= 1) {
            return BlockPos.containing(this.mob.getX(), this.mob.getBoundingBox().minY + 0.5, this.mob.getZ());
        }
        return BlockPos.containing(this.mob.getBoundingBox().minX, this.mob.getBoundingBox().minY + 0.5,
                this.mob.getBoundingBox().minZ);
    }

    public void setPath(Path newPath, double speed) {
        this.modifier.cancel();
        this.resetStatus();
        this.path = newPath;
        this.speed = speed;
        this.ticksStuck = 0;
        this.lastPathDistanceToTarget = newPath.distanceEndToTarget();
        newPath.setIndex(newPath.length() > 1 ? 1 : 0);
        this.onNodeReached();
        this.lastDistance = this.distanceToActiveNode();
    }

    public void clearPath() {
        this.path = null;
        this.modifier.cancel();
        this.resetStatus();
    }

    public boolean noPath() {
        return this.path == null || this.path.isFinished();
    }

    public @Nullable Path getPath() {
        return this.path;
    }

    public int stuckTime() {
        return this.ticksStuck;
    }

    public boolean isWaitingForTask() {
        return this.waiting;
    }

    /** How far the last planned path ended from its target (large: the nexus is out of reach for now). */
    public float lastPathDistanceToTarget() {
        return this.lastPathDistanceToTarget;
    }

    public String debugString() {
        if (this.path == null) {
            return "no path (last end distance " + this.lastPathDistanceToTarget + ")";
        }
        return "node " + this.path.index() + "/" + this.path.length() + " " + this.path.current() + " end=" + this.path.last()
                + " waiting=" + this.waiting + " actionDone=" + this.nodeActionDone + " stuck=" + this.ticksStuck
                + " busy=" + this.modifier.isBusy();
    }

    // ------------------------------------------------------------------ following

    public void tick() {
        this.modifier.tick();
        if (this.waiting) {
            if (this.holdPos != null) {
                this.hold();
            }
            return;
        }
        this.releaseHold();
        if (this.noPath()) {
            return;
        }
        PathNode node = this.path.current();
        if (node == null) {
            return;
        }

        if (!this.nodeActionDone) {
            if (!this.handleNodeAction(node)) {
                this.clearPath();
            }
            return;
        }

        double distance = this.distanceToActiveNode();
        if (this.lastDistance - distance > 0.01) {
            this.lastDistance = distance;
            this.ticksStuck = Math.max(0, this.ticksStuck - 1);
        } else {
            this.ticksStuck++;
        }

        int before = this.path.index();
        this.advance();
        if (this.noPath()) {
            return;
        }
        if (this.path.index() != before) {
            this.ticksStuck = 0;
            this.onNodeReached();
            this.lastDistance = this.distanceToActiveNode();
            if (!this.nodeActionDone) {
                return;
            }
        }
        node = this.path.current();

        if (!this.isClear(node)) {
            // The next node is blocked: dig it out (the original onPathBlocked).
            if (this.invader.terrainSkill() != Invader.TerrainSkill.NONE && this.invader.canDigNow()
                    && this.digger.askClearPosition(node.pos(), this.pathing.sizeX(), this.pathing.sizeY(), this, 1.0F)) {
                this.waiting = true;
                this.holdPos = this.mob.position();
                return;
            }
        }
        this.moveToward(node);
    }

    private void onNodeReached() {
        PathNode node = this.path == null ? null : this.path.current();
        this.nodeActionDone = node == null || node.action == PathAction.NONE || node.action == PathAction.SWIM
                || (node.action.isLadder() && this.mob.level().getBlockState(node.pos()).is(Blocks.LADDER));
    }

    /** Engineers build what a node needs; diggers dig down. Returns false to give up the path. */
    private boolean handleNodeAction(PathNode node) {
        if (this.lastResult != TaskListener.SUCCESS) {
            this.lastResult = TaskListener.SUCCESS;
            return false;
        }
        boolean asked = false;
        if (node.action == PathAction.DIG) {
            asked = this.digger.askClearPosition(node.pos(), this.pathing.sizeX(), this.pathing.sizeY(), this, 1.0F);
        } else if (this.builder != null) {
            if (node.action.isLadder()) {
                asked = this.builder.askBuildLadder(node.pos(), node.action.wallSide(), this);
            } else if (node.action.isLadderTower()) {
                asked = this.builder.askBuildLadderTower(node.pos(), node.action.wallSide(), 1, this);
            } else if (node.action == PathAction.BRIDGE) {
                asked = this.builder.askBuildBridge(node.pos(), this);
            } else if (node.action == PathAction.SCAFFOLD_UP) {
                asked = this.builder.askBuildScaffoldLayer(node.pos(), this.attackerAI(), this);
            }
        }
        this.nodeActionDone = true;
        if (asked) {
            this.waiting = true;
            this.holdPos = this.mob.position();
        }
        return true;
    }

    @Override
    public void onTaskFinished(int result) {
        this.waiting = false;
        this.lastResult = result;
        if (result != TaskListener.SUCCESS) {
            this.clearPath();
        }
    }

    /** Moves the path index past nodes we have reached, and skips ahead along straight, clear stretches. */
    private void advance() {
        Vec3 pos = this.mob.position();
        int feetY = Mth.floor(this.mob.getBoundingBox().minY + 0.5);
        double reach = Math.max(0.5, this.mob.getBbWidth() * 0.5);
        while (!this.path.isFinished()) {
            PathNode node = this.path.current();
            Vec3 center = this.nodeCenter(node);
            double dx = center.x - pos.x;
            double dz = center.z - pos.z;
            boolean horizontal = dx * dx + dz * dz < reach * reach;
            boolean vertical = node.y == feetY || (node.y < feetY && this.mob.onGround()) || (node.y <= feetY && node.action.isLadder());
            if (horizontal && vertical) {
                this.path.setIndex(this.path.index() + 1);
                if (!this.path.isFinished() && this.path.current().action != PathAction.NONE) {
                    return;
                }
            } else {
                break;
            }
        }
        if (this.path.isFinished() || !this.mob.onGround()) {
            return;
        }
        // Shortcut: aim for the furthest of the next few plain nodes on this level that we can walk to directly.
        int best = this.path.index();
        for (int i = this.path.index() + 1; i < Math.min(this.path.length(), this.path.index() + 6); i++) {
            PathNode n = this.path.node(i);
            if (n.action != PathAction.NONE || n.y != feetY) {
                break;
            }
            if (this.isDirect(pos, this.nodeCenter(n))) {
                best = i;
            }
        }
        if (best != this.path.index()) {
            this.path.setIndex(best);
        }
    }

    private boolean isDirect(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.horizontalDistance();
        if (len < 1.0E-4) {
            return true;
        }
        // Rays along both edges of the mob as well as its centre, at foot and head height.
        double half = this.mob.getBbWidth() * 0.5 + 0.05;
        Vec3 side = new Vec3(-d.z / len * half, 0, d.x / len * half);
        double[] heights = {0.3, this.mob.getBbHeight() - 0.2};
        for (Vec3 offset : new Vec3[] {Vec3.ZERO, side, side.reverse()}) {
            for (double h : heights) {
                Vec3 a = from.add(offset).add(0, h, 0);
                Vec3 b = to.add(offset).add(0, h, 0);
                if (this.mob.level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob))
                        .getType() != HitResult.Type.MISS) {
                    return false;
                }
            }
        }
        // There must be ground all along the way.
        int steps = (int) Math.ceil(d.horizontalDistance());
        for (int s = 1; s <= steps; s++) {
            Vec3 p = from.add(d.scale((double) s / steps));
            BlockPos below = BlockPos.containing(p.x, from.y - 0.5, p.z);
            if (MobPathing.isPassable(new TerrainView(this.mob.level(), null), below, this.mob.level().getBlockState(below))) {
                return false;
            }
        }
        return true;
    }

    private Vec3 nodeCenter(PathNode node) {
        return new Vec3(node.x + this.pathing.sizeX() * 0.5, node.y, node.z + this.pathing.sizeZ() * 0.5);
    }

    private boolean isClear(PathNode node) {
        TerrainView view = new TerrainView(this.mob.level(), null);
        int c = this.pathing.collide(view, node.pos());
        if (c == MobPathing.DIGGABLE || c == MobPathing.BLOCKED) {
            return false;
        }
        if (node.y > Mth.floor(this.mob.getY())) {
            BlockPos head = this.mob.blockPosition().above(this.pathing.sizeY());
            return MobPathing.isPassable(view, head, this.mob.level().getBlockState(head));
        }
        return true;
    }

    private void moveToward(PathNode node) {
        Vec3 target = this.nodeCenter(node);
        if (node.action.isLadder() || node.action.isLadderTower()) {
            // Push into the wall the ladder hangs on: that is what makes a mob climb.
            Direction wall = node.action.wallSide();
            target = target.add(wall.getStepX() * 0.8, 0, wall.getStepZ() * 0.8);
        } else if (node.y > Mth.floor(this.mob.getY()) && this.invader.canClimbWalls()) {
            target = target.add(0, 1, 0);
        }
        this.mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, this.speed);
    }

    private double distanceToActiveNode() {
        PathNode node = this.path == null ? null : this.path.current();
        if (node == null) {
            return 0.0;
        }
        return Math.sqrt(this.mob.distanceToSqr(this.nodeCenter(node)));
    }

    // ------------------------------------------------------------------ holding still while working

    private void hold() {
        if (this.mob.onClimbable()) {
            // Hang on the ladder while building: no sliding down.
            this.mob.setNoGravity(true);
            this.mob.setDeltaMovement(0, 0, 0);
        } else {
            this.mob.getMoveControl().setWantedPosition(this.holdPos.x, this.holdPos.y, this.holdPos.z, this.speed);
        }
    }

    private void releaseHold() {
        if (this.mob.isNoGravity() && this.holdPos != null) {
            this.mob.setNoGravity(false);
        }
        this.holdPos = null;
    }

    private void resetStatus() {
        if (this.mob.isNoGravity()) {
            this.mob.setNoGravity(false);
        }
        this.waiting = false;
        this.holdPos = null;
        this.nodeActionDone = true;
        this.lastResult = TaskListener.SUCCESS;
    }
}
