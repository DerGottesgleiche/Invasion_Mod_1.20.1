package invmod.entity.ai.navigator;

import invmod.entity.Invader;
import invmod.entity.Terrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * The invader movement rules from the original EntityIMMob / EntityIMPigEngy: which blocks an invader can
 * stand in, step up on, fall down to, climb, swim, dig through and (for engineers) build ladders, ladder
 * towers, bridges and scaffolds into, and what each step costs. Digging costs the time it takes, so an
 * invader goes for the weakest point of a wall rather than straight through it.
 */
public class MobPathing implements Pathfindable {
    /** Result of {@link #collide}: what fills the space a mob would occupy at a position. */
    public static final int BLOCKED = 0;
    public static final int CLEAR = 1;
    public static final int DIGGABLE = 2;
    public static final int FENCE_BELOW = 3;
    public static final int LIQUID = -1;
    public static final int HAZARD = -2;

    private static final int MAX_FALL = 8;
    /** Walking one block takes about this many ticks; dig times are converted to walking blocks with it. */
    private static final float TICKS_PER_BLOCK = 5.0F;

    protected final Mob mob;
    protected final Invader invader;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected @Nullable BlockPos goal;

    public MobPathing(Mob mob) {
        this.mob = mob;
        this.invader = (Invader) mob;
        this.sizeX = Math.max(1, (int) (mob.getBbWidth() + 1.0F));
        this.sizeY = Math.max(1, (int) (mob.getBbHeight() + 1.0F));
        this.sizeZ = this.sizeX;
    }

    public void setGoal(@Nullable BlockPos goal) {
        this.goal = goal;
    }

    public int sizeX() {
        return this.sizeX;
    }

    public int sizeZ() {
        return this.sizeZ;
    }

    public int sizeY() {
        return this.sizeY;
    }

    protected boolean canDig() {
        return this.invader.terrainSkill() != Invader.TerrainSkill.NONE && this.invader.canDigNow();
    }

    protected boolean canBuild() {
        return this.invader.terrainSkill() == Invader.TerrainSkill.BUILD_LADDERS;
    }

    // ------------------------------------------------------------------ block rules

    public boolean avoids(BlockState state) {
        return state.getBlock() instanceof BaseFireBlock || state.is(Blocks.LAVA) || state.getBlock() instanceof CactusBlock
                || state.getBlock() instanceof MagmaBlock || state.getBlock() instanceof SweetBerryBushBlock
                || state.is(Blocks.POWDER_SNOW);
    }

    /** Open space for a mob: air, anything without collision, and climbables (a mob moves inside a ladder block). */
    public static boolean isPassable(TerrainView terrain, BlockPos pos, BlockState state) {
        return state.isAir() || state.is(BlockTags.CLIMBABLE) || state.getCollisionShape(terrain.level(), pos).isEmpty();
    }

    /** Whether this invader may dig the block, including the original zombies' rule against digging too steeply up. */
    public boolean isDiggable(TerrainView terrain, BlockPos pos) {
        if (!this.canDig() || !Terrain.isDestructible(terrain.level(), pos)) {
            return false;
        }
        if (this.goal != null) {
            int dY = this.goal.getY() - pos.getY();
            if (dY > 0) {
                double dX = this.goal.getX() - pos.getX();
                double dZ = this.goal.getZ() - pos.getZ();
                double dXZ = Math.sqrt(dX * dX + dZ * dZ) + 1.0E-5;
                if ((dY + 8) / dXZ > 2.144) {
                    return false;
                }
            }
        }
        return true;
    }

    /** What fills the mob-sized box at this position (the original getCollide). */
    public int collide(TerrainView terrain, BlockPos pos) {
        boolean diggable = false;
        boolean liquid = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = pos.getX(); x < pos.getX() + this.sizeX; x++) {
            for (int y = pos.getY(); y < pos.getY() + this.sizeY; y++) {
                for (int z = pos.getZ(); z < pos.getZ() + this.sizeZ; z++) {
                    p.set(x, y, z);
                    BlockState state = terrain.state(p);
                    if (state.isAir()) {
                        continue;
                    }
                    FluidState fluid = state.getFluidState();
                    if (this.avoids(state)) {
                        return HAZARD;
                    }
                    if (!fluid.isEmpty() && (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER))) {
                        liquid = true;
                    } else if (!isPassable(terrain, p, state)) {
                        if (this.isDiggable(terrain, p.immutable())) {
                            diggable = true;
                        } else {
                            return BLOCKED;
                        }
                    }
                }
            }
        }
        if (diggable) {
            return DIGGABLE;
        }
        return liquid ? LIQUID : CLEAR;
    }

    /** Whether there is solid, safe ground under the mob-sized box at this position. */
    public boolean canStandOn(TerrainView terrain, BlockPos feet) {
        boolean solid = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = feet.getX(); x < feet.getX() + this.sizeX; x++) {
            for (int z = feet.getZ(); z < feet.getZ() + this.sizeZ; z++) {
                p.set(x, feet.getY() - 1, z);
                BlockState state = terrain.state(p);
                if (state.isAir()) {
                    continue;
                }
                if (this.avoids(state)) {
                    return false;
                }
                if (!isPassable(terrain, p, state) || state.getBlock() instanceof LadderBlock) {
                    solid = true;
                }
            }
        }
        return solid;
    }

    public boolean isStandable(TerrainView terrain, BlockPos feet) {
        int c = this.collide(terrain, feet);
        return (c == CLEAR || c == DIGGABLE) && this.canStandOn(terrain, feet);
    }

    /** Whether a solid block touches the side of the mob-sized box at this height (something to climb). */
    protected boolean isAdjacentSolid(TerrainView terrain, BlockPos pos) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = pos.getX() - 1; x <= pos.getX() + this.sizeX; x++) {
            for (int z = pos.getZ() - 1; z <= pos.getZ() + this.sizeZ; z++) {
                boolean edgeX = x == pos.getX() - 1 || x == pos.getX() + this.sizeX;
                boolean edgeZ = z == pos.getZ() - 1 || z == pos.getZ() + this.sizeZ;
                if (edgeX == edgeZ) {
                    continue;
                }
                p.set(x, pos.getY(), z);
                if (!isPassable(terrain, p, terrain.state(p))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isFence(BlockState state) {
        return state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock || state.is(BlockTags.FENCE_GATES);
    }

    // ------------------------------------------------------------------ node options

    @Override
    public void addPathOptions(TerrainView terrain, PathNode node, Pathfinder pathfinder) {
        if (node.y <= terrain.level().getMinY() || node.y >= terrain.level().getMaxY()) {
            return;
        }
        BlockPos pos = node.pos();
        this.addVerticalOptions(terrain, node, pathfinder);
        if (node.action == PathAction.DIG && !this.canStandOn(terrain, pos)) {
            return;
        }
        this.addWalkOptions(terrain, node, pathfinder);
    }

    /** Steps to the four neighbouring columns: up by the jump height, level, or down a drop (the original loop). */
    public void addWalkOptions(TerrainView terrain, PathNode node, Pathfinder pathfinder) {
        BlockPos pos = node.pos();

        int height = this.invader.jumpHeight();
        for (int i = 1; i <= height; i++) {
            int c = this.collide(terrain, pos.above(i));
            if (c == BLOCKED || c == HAZARD) {
                height = i - 1;
                break;
            }
        }
        if (node.action.isLadder() || node.action.isLadderTower()) {
            // Leaving a ladder sideways only works at its top, onto the wall.
            height = Math.min(height, 1);
        }

        for (Direction dir : PathAction.SIDES) {
            int nx = node.x + dir.getStepX();
            int nz = node.z + dir.getStepZ();
            for (int y = node.y + height; y >= node.y - MAX_FALL; y--) {
                BlockPos next = new BlockPos(nx, y, nz);
                int c = this.collide(terrain, next);
                if (c == BLOCKED || c == HAZARD || (c == DIGGABLE && !this.canDig())) {
                    if (y < node.y) {
                        break;
                    }
                    continue;
                }
                if (c == LIQUID && this.invader.canSwim()) {
                    pathfinder.addOption(next, PathAction.SWIM);
                    if (y <= node.y) {
                        break;
                    }
                    continue;
                }
                if (this.canStandOn(terrain, next) || (this.invader.canClimbWalls() && this.isAdjacentSolid(terrain, next))) {
                    pathfinder.addOption(next, PathAction.NONE);
                    if (y <= node.y) {
                        break;
                    }
                } else if (y < node.y && c != CLEAR) {
                    break;
                }
            }
        }
    }

    protected void addVerticalOptions(TerrainView terrain, PathNode node, Pathfinder pathfinder) {
        BlockPos pos = node.pos();
        BlockPos above = pos.above();
        BlockPos below = pos.below();
        int collideAbove = this.collide(terrain, above);
        if (collideAbove > BLOCKED) {
            BlockState aboveState = terrain.state(above);
            BlockState here = terrain.state(pos);
            BlockState ladder = aboveState.getBlock() instanceof LadderBlock ? aboveState
                    : here.getBlock() instanceof LadderBlock ? here : null;
            if (ladder != null && collideAbove == CLEAR) {
                PathAction action = PathAction.ladderAgainst(ladder.getValue(LadderBlock.FACING).getOpposite());
                pathfinder.addOption(above, action);
            } else if (this.invader.canClimbWalls() && collideAbove == CLEAR && this.isAdjacentSolid(terrain, above)) {
                pathfinder.addOption(above, PathAction.NONE);
            }
        }
        if (this.canDig() && this.invader.canDigDown() && this.collide(terrain, below) == DIGGABLE) {
            pathfinder.addOption(below, PathAction.DIG);
        }
        if (this.invader.canSwim() && this.collide(terrain, above) == LIQUID) {
            pathfinder.addOption(above, PathAction.SWIM);
        }
    }

    // ------------------------------------------------------------------ costs

    /** Ticks needed to clear the mob-sized box at the node, converted to walking blocks. */
    protected float digCost(TerrainView terrain, PathNode node) {
        float ticks = 0.0F;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = node.x; x < node.x + this.sizeX; x++) {
            for (int y = node.y; y < node.y + this.sizeY; y++) {
                for (int z = node.z; z < node.z + this.sizeZ; z++) {
                    p.set(x, y, z);
                    BlockState state = terrain.state(p);
                    if (!isPassable(terrain, p, state)) {
                        ticks += Terrain.digTicks(terrain.level(), p.immutable(), this.invader.digSpeed());
                    }
                }
            }
        }
        return ticks / TICKS_PER_BLOCK;
    }

    @Override
    public float pathCost(PathNode from, PathNode to, TerrainView terrain) {
        float distance = from.distanceTo(to);
        float multiplier = 1.0F + terrain.density(to.x, to.y, to.z) * 3.0F;
        if (to.action == PathAction.SWIM) {
            boolean submerged = to.y <= from.y && !terrain.state(to.x, to.y + 1, to.z).isAir();
            return distance * 1.3F * multiplier * (submerged ? 3.0F : 1.0F);
        }
        if (to.action.isLadder() && terrain.state(to.x, to.y, to.z).getBlock() instanceof LadderBlock) {
            return distance * 0.7F * multiplier;
        }
        float dig = 0.0F;
        if (this.collide(terrain, to.pos()) == DIGGABLE) {
            dig = this.digCost(terrain, to);
            if (to.y > from.y) {
                multiplier += 2.0F;
            }
        }
        return distance * multiplier + dig;
    }
}
