package invmod.entity;

import invmod.Config;
import invmod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/** Block strength and destructibility rules for invaders, following the original mod. */
public final class Terrain {
    /** Blocks at least this explosion-resistant (obsidian and up) are treated as indestructible. */
    public static final float MAX_DIGGABLE_RESISTANCE = 1000.0F;

    private Terrain() {
    }

    /** The original formula: explosion resistance, +10% for every neighbour made of the same block. */
    public static float blockStrength(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        int bonus = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(block)) {
                bonus++;
            }
        }
        return block.getExplosionResistance() * (1.0F + bonus * 0.1F);
    }

    public static boolean isObstacle(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isDestructible(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(ModBlocks.NEXUS.get()) || state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof LadderBlock || hasLadderAttached(level, pos)) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F || state.getBlock().getExplosionResistance() >= MAX_DIGGABLE_RESISTANCE) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean hasLadderAttached(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            if (neighbour.getBlock() instanceof LadderBlock && neighbour.getValue(LadderBlock.FACING) == direction) {
                return true;
            }
        }
        return false;
    }

    /** Ticks needed to dig a block: strength * 20, as in the original TerrainDigger. */
    public static int digTicks(Level level, BlockPos pos, float digSpeed) {
        return Math.max(5, Math.round(blockStrength(level, pos) * 20.0F / Math.max(0.1F, digSpeed)));
    }

    public static void removeBlock(Level level, BlockPos pos) {
        level.destroyBlock(pos, Config.DROP_DESTRUCTED_BLOCKS.get());
    }
}
