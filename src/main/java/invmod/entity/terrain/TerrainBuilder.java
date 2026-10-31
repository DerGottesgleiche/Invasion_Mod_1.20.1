package invmod.entity.terrain;

import invmod.entity.ai.AttackerAI;
import invmod.entity.ai.Scaffold;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The pigman engineer's building (original TerrainBuilder): ladders, ladder towers, bridges and scaffold
 * layers, each block taking its cost in ticks divided by the build rate (helpers nearby speed it up).
 */
public class TerrainBuilder {
    public static final float LADDER_COST = 25.0F;
    public static final float PLANKS_COST = 45.0F;
    public static final float COBBLE_COST = 65.0F;

    private final Mob mob;
    private final TerrainModifier modifier;
    private final BlockState material;
    private final float blockCost;
    private float buildRate = 1.0F;

    public TerrainBuilder(Mob mob, TerrainModifier modifier, BlockState material, float blockCost) {
        this.mob = mob;
        this.modifier = modifier;
        this.material = material;
        this.blockCost = blockCost;
    }

    public void setBuildRate(float rate) {
        this.buildRate = rate;
    }

    private int cost(float base) {
        return (int) (base / this.buildRate);
    }

    private static BlockState ladder(Direction wall) {
        return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, wall.getOpposite());
    }

    private boolean isFull(Level level, BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    private boolean canPlace(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() || state.is(Blocks.LADDER);
    }

    /** Ladder at the node and, when there is ground two below, at the block under it too. */
    public boolean askBuildLadder(BlockPos pos, Direction wall, TaskListener asker) {
        Level level = this.mob.level();
        if (!this.modifier.isReadyFor(asker)) {
            return false;
        }
        List<ModifyBlockEntry> list = new ArrayList<>();
        BlockState ladder = ladder(wall);
        BlockPos below = pos.below();
        if (!level.getBlockState(below).is(Blocks.LADDER) && this.canPlace(level, below) && this.isFull(level, below.relative(wall))) {
            list.add(new ModifyBlockEntry(below, ladder, this.cost(LADDER_COST)));
        }
        if (!level.getBlockState(pos).is(Blocks.LADDER)) {
            if (!this.canPlace(level, pos) || !this.isFull(level, pos.relative(wall))) {
                return false;
            }
            list.add(new ModifyBlockEntry(pos, ladder, this.cost(LADDER_COST)));
        }
        return this.modifier.requestTask(list, asker);
    }

    /** Puts a block on the wall side first, then hangs the ladder on it. */
    public boolean askBuildLadderTower(BlockPos pos, Direction wall, int layers, TaskListener asker) {
        Level level = this.mob.level();
        if (!this.modifier.isReadyFor(asker)) {
            return false;
        }
        List<ModifyBlockEntry> list = new ArrayList<>();
        BlockState ladder = ladder(wall);
        BlockPos below = pos.below();
        if (!this.isFull(level, below.relative(wall))) {
            list.add(new ModifyBlockEntry(below.relative(wall), this.material, this.cost(this.blockCost)));
        }
        if (!level.getBlockState(below).is(Blocks.LADDER) && this.canPlace(level, below)) {
            list.add(new ModifyBlockEntry(below, ladder, this.cost(LADDER_COST)));
        }
        for (int i = 0; i < layers; i++) {
            BlockPos side = pos.above(i).relative(wall);
            if (!this.isFull(level, side)) {
                list.add(new ModifyBlockEntry(side, this.material, this.cost(this.blockCost)));
            }
            if (!level.getBlockState(pos.above(i)).is(Blocks.LADDER)) {
                list.add(new ModifyBlockEntry(pos.above(i), ladder, this.cost(LADDER_COST)));
            }
        }
        return this.modifier.requestTask(list, asker);
    }

    public boolean askBuildBridge(BlockPos pos, TaskListener asker) {
        Level level = this.mob.level();
        if (!this.modifier.isReadyFor(asker) || !level.getBlockState(pos.below()).canBeReplaced()) {
            return false;
        }
        return this.modifier.requestTask(List.of(new ModifyBlockEntry(pos.below(), this.material, this.cost(this.blockCost))), asker);
    }

    /** One layer of a planned scaffold: the pillar block, the ladder on it, and the platform ring where planned. */
    public boolean askBuildScaffoldLayer(BlockPos pos, @Nullable AttackerAI ai, TaskListener asker) {
        Level level = this.mob.level();
        if (ai == null || !this.modifier.isReadyFor(asker)) {
            return false;
        }
        Scaffold scaffold = ai.scaffoldAt(pos.getX(), pos.getY(), pos.getZ());
        if (scaffold == null) {
            return false;
        }
        int height = pos.getY() - scaffold.pos().getY();
        Direction side = scaffold.orientation();
        List<ModifyBlockEntry> list = new ArrayList<>();
        if (height == 1) {
            if (!this.isFull(level, pos.below().relative(side))) {
                list.add(new ModifyBlockEntry(pos.below().relative(side), this.material, this.cost(this.blockCost)));
            }
            if (this.canPlace(level, pos.below()) && !level.getBlockState(pos.below()).is(Blocks.LADDER)) {
                list.add(new ModifyBlockEntry(pos.below(), ladder(side), this.cost(LADDER_COST)));
            }
        }
        if (!this.isFull(level, pos.relative(side))) {
            list.add(new ModifyBlockEntry(pos.relative(side), this.material, this.cost(this.blockCost)));
        }
        if (!level.getBlockState(pos).is(Blocks.LADDER)) {
            list.add(new ModifyBlockEntry(pos, ladder(side), this.cost(LADDER_COST)));
        }
        if (scaffold.isLayerPlatform(height)) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos ring = pos.offset(dx, 0, dz);
                    if ((dx != 0 || dz != 0) && !ring.equals(pos.relative(side)) && !this.isFull(level, ring)) {
                        list.add(new ModifyBlockEntry(ring, this.material, this.cost(this.blockCost)));
                    }
                }
            }
        }
        return this.modifier.requestTask(list, asker);
    }
}
