package invmod.entity.ai;

import invmod.block.NexusBlockEntity;
import invmod.entity.Invader;
import invmod.entity.Terrain;
import invmod.registry.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Walks a nexus-bound invader to its nexus. When the way is blocked it does what the original mod's
 * invaders did: diggers tunnel through the wall, pigmen engineers build ladders up it, and everyone else
 * pushes on towards the nexus (climbing any ladder they find on the way).
 */
public class GoToNexusGoal extends Goal {
    private static final int STUCK_CHECK_INTERVAL = 20;
    private final Mob mob;
    private final Invader invader;
    private final double speed;
    private int repathTimer;
    private int stuckTicks;
    private Vec3 lastPos = Vec3.ZERO;
    private int checkTimer;
    private @Nullable BlockPos digPos;
    private int digProgress;
    private int digTime;
    private int scrapeTimer;
    private int buildTimer;

    public GoToNexusGoal(Mob mob, double speed) {
        this.mob = mob;
        this.invader = (Invader) mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        BlockPos nexus = this.invader.getNexusPos();
        if (nexus == null) {
            return false;
        }
        if (this.mob.level().isLoaded(nexus) && !(this.mob.level().getBlockEntity(nexus) instanceof NexusBlockEntity)) {
            // The nexus is gone: from now on this is an ordinary hostile mob.
            this.invader.invaderData().setNexusPos(null);
            return false;
        }
        return this.mob.getTarget() == null && this.distanceToNexus() > 2.0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        // Spawn points are ~50 blocks out; let the pathfinder plan that far.
        this.mob.getNavigation().setRequiredPathLength(96.0F);
        this.repathTimer = 0;
        this.stuckTicks = 0;
        this.lastPos = this.mob.position();
    }

    @Override
    public void stop() {
        this.cancelDig();
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        BlockPos nexus = this.invader.getNexusPos();
        if (nexus == null) {
            return;
        }
        Vec3 nexusCenter = Vec3.atCenterOf(nexus);
        this.mob.getLookControl().setLookAt(nexusCenter.x, nexusCenter.y, nexusCenter.z, 10.0F, this.mob.getMaxHeadXRot());

        if (this.digPos != null) {
            this.tickDig();
            return;
        }

        if (++this.checkTimer >= STUCK_CHECK_INTERVAL) {
            this.checkTimer = 0;
            if (this.mob.position().distanceToSqr(this.lastPos) < 0.5) {
                this.stuckTicks += STUCK_CHECK_INTERVAL;
            } else {
                this.stuckTicks = 0;
            }
            this.lastPos = this.mob.position();
        }

        boolean pathFails = false;
        if (--this.repathTimer <= 0) {
            this.repathTimer = 30 + this.mob.getRandom().nextInt(20);
            Path path = this.mob.getNavigation().createPath(nexus, 1);
            if (path != null) {
                this.mob.getNavigation().moveTo(path, this.speed);
                pathFails = !path.canReach();
            } else {
                pathFails = true;
            }
        }

        boolean stuck = this.stuckTicks >= 40 || (pathFails && this.mob.getNavigation().isDone());
        if (!stuck && !this.mob.getNavigation().isDone()) {
            return;
        }

        // The way is blocked: deal with the terrain.
        switch (this.invader.terrainSkill()) {
            case DIG -> {
                BlockPos target = this.findBlockToDig(nexus);
                if (target != null) {
                    this.startDig(target);
                    return;
                }
            }
            case BUILD_LADDERS -> {
                if (this.tryBuildLadder(nexus)) {
                    return;
                }
            }
            default -> {
            }
        }
        this.pushTowards(nexusCenter);
    }

    private double distanceToNexus() {
        BlockPos nexus = this.invader.getNexusPos();
        return nexus == null ? 0 : Math.sqrt(this.mob.distanceToSqr(Vec3.atCenterOf(nexus)));
    }

    /** Walks straight at the target; climbs ladders and jumps single blocks on the way. */
    private void pushTowards(Vec3 target) {
        Vec3 pos = this.mob.position();
        Vec3 dir = new Vec3(target.x - pos.x, 0, target.z - pos.z);
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        dir = dir.normalize();
        this.mob.getNavigation().stop();
        this.mob.getMoveControl().setWantedPosition(pos.x + dir.x * 2.0, pos.y, pos.z + dir.z * 2.0, this.speed);
        if (this.mob.horizontalCollision && this.mob.onGround()) {
            this.mob.getJumpControl().jump();
        }
    }

    private Direction directionTo(BlockPos nexus) {
        Vec3 pos = this.mob.position();
        double dx = nexus.getX() + 0.5 - pos.x;
        double dz = nexus.getZ() + 0.5 - pos.z;
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    private Direction secondaryDirectionTo(BlockPos nexus) {
        Vec3 pos = this.mob.position();
        double dx = nexus.getX() + 0.5 - pos.x;
        double dz = nexus.getZ() + 0.5 - pos.z;
        return Math.abs(dx) > Math.abs(dz) ? (dz > 0 ? Direction.SOUTH : Direction.NORTH)
                : (dx > 0 ? Direction.EAST : Direction.WEST);
    }

    private @Nullable BlockPos findBlockToDig(BlockPos nexus) {
        Level level = this.mob.level();
        BlockPos feet = this.mob.blockPosition();
        int height = Math.max(2, (int) Math.ceil(this.mob.getBbHeight()));
        int dy = nexus.getY() - feet.getY();
        double horizontal = Math.sqrt(Math.pow(nexus.getX() + 0.5 - this.mob.getX(), 2)
                + Math.pow(nexus.getZ() + 0.5 - this.mob.getZ(), 2));

        List<BlockPos> candidates = new ArrayList<>();
        if (horizontal < 1.5 && dy < 0) {
            candidates.add(feet.below());
        }
        for (Direction dir : new Direction[] {this.directionTo(nexus), this.secondaryDirectionTo(nexus)}) {
            BlockPos front = feet.relative(dir);
            if (dy > 0) {
                // Leave the front block as a step: clear the space above it and above our own head.
                candidates.add(feet.above(height));
                for (int i = 1; i <= height; i++) {
                    candidates.add(front.above(i));
                }
            } else {
                for (int i = 0; i < height; i++) {
                    candidates.add(front.above(i));
                }
                if (dy < 0) {
                    candidates.add(front.below());
                }
            }
        }
        for (BlockPos pos : candidates) {
            if (level.getBlockState(pos).is(invmod.registry.ModBlocks.NEXUS.get())) {
                return null;
            }
            if (Terrain.isObstacle(level, pos)) {
                return Terrain.isDestructible(level, pos) ? pos : null;
            }
        }
        return null;
    }

    private void startDig(BlockPos pos) {
        this.digPos = pos;
        this.digProgress = 0;
        this.digTime = Terrain.digTicks(this.mob.level(), pos, this.invader.digSpeed());
        this.mob.getNavigation().stop();
    }

    private void tickDig() {
        Level level = this.mob.level();
        BlockPos pos = this.digPos;
        if (pos == null) {
            return;
        }
        if (!Terrain.isObstacle(level, pos) || this.mob.distanceToSqr(Vec3.atCenterOf(pos)) > 9.0) {
            this.cancelDig();
            return;
        }
        this.mob.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (this.mob.tickCount % 10 == 0) {
            this.mob.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        }
        if (--this.scrapeTimer <= 0) {
            this.scrapeTimer = 45 + this.mob.getRandom().nextInt(20);
            this.mob.playSound(ModSounds.SCRAPE.get(), 0.85F, 1.0F / (this.mob.getRandom().nextFloat() * 0.5F + 1.0F));
        }
        this.digProgress++;
        level.destroyBlockProgress(this.mob.getId(), pos, (int) (10.0F * this.digProgress / this.digTime));
        if (this.digProgress >= this.digTime) {
            level.destroyBlockProgress(this.mob.getId(), pos, -1);
            Terrain.removeBlock(level, pos);
            this.digPos = null;
            this.stuckTicks = 0;
            this.repathTimer = 0;
        }
    }

    private void cancelDig() {
        if (this.digPos != null) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.digPos, -1);
            this.digPos = null;
        }
    }

    /** Pigman engineers place a ladder column against the wall in front of them. */
    private boolean tryBuildLadder(BlockPos nexus) {
        Level level = this.mob.level();
        Direction dir = this.directionTo(nexus);
        BlockPos feet = this.mob.blockPosition();
        if (!Terrain.isObstacle(level, feet.relative(dir))) {
            dir = this.secondaryDirectionTo(nexus);
            if (!Terrain.isObstacle(level, feet.relative(dir))) {
                return false;
            }
        }
        if (--this.buildTimer > 0) {
            this.pushTowards(Vec3.atCenterOf(feet.relative(dir)));
            return true;
        }
        this.buildTimer = 15;
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, dir.getOpposite());
        for (int i = 0; i < 12; i++) {
            BlockPos pos = feet.above(i);
            BlockPos wall = pos.relative(dir);
            BlockState current = level.getBlockState(pos);
            if (current.is(Blocks.LADDER)) {
                continue;
            }
            if (!Terrain.isObstacle(level, wall)) {
                // Reached the top of the wall: climb it.
                break;
            }
            if (!current.canBeReplaced() || !ladder.canSurvive(level, pos)) {
                return false;
            }
            level.setBlockAndUpdate(pos, ladder);
            this.mob.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            this.mob.playSound(SoundEvents.LADDER_PLACE, 1.0F, 1.0F);
            return true;
        }
        this.pushTowards(Vec3.atCenterOf(feet.relative(dir)));
        return true;
    }
}
