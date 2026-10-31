package invmod.entity.terrain;

import invmod.entity.Invader;
import invmod.entity.Terrain;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/** Turns "clear this path node" into timed dig entries for the terrain modifier (original TerrainDigger). */
public class TerrainDigger {
    private final Mob mob;
    private final TerrainModifier modifier;

    public TerrainDigger(Mob mob, TerrainModifier modifier) {
        this.mob = mob;
        this.modifier = modifier;
    }

    /**
     * The blocks to remove so the mob fits at {@code feet}: the box itself and, when going up, the block above
     * the mob's head so it can jump. Ordered top first going up, bottom first otherwise.
     */
    public List<BlockPos> removalOrder(BlockPos feet, int sizeX, int sizeY) {
        List<BlockPos> order = new ArrayList<>();
        boolean up = feet.getY() > Mth.floor(this.mob.getY());
        if (up) {
            order.add(this.mob.blockPosition().above(sizeY));
            for (int y = sizeY - 1; y >= 0; y--) {
                addLayer(order, feet.above(y), sizeX);
            }
        } else {
            for (int y = 0; y < sizeY; y++) {
                addLayer(order, feet.above(y), sizeX);
            }
        }
        return order;
    }

    private static void addLayer(List<BlockPos> order, BlockPos corner, int size) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                order.add(corner.offset(x, 0, z));
            }
        }
    }

    public boolean askClearPosition(BlockPos feet, int sizeX, int sizeY, TaskListener listener, float costMultiplier) {
        List<ModifyBlockEntry> entries = new ArrayList<>();
        float speed = ((Invader) this.mob).digSpeed();
        for (BlockPos pos : this.removalOrder(feet, sizeX, sizeY)) {
            if (!Terrain.isPassable(this.mob.level(), pos, this.mob.level().getBlockState(pos))) {
                if (!Terrain.isDestructible(this.mob.level(), pos)) {
                    return false;
                }
                entries.add(new ModifyBlockEntry(pos, null, (int) (costMultiplier * Terrain.digTicks(this.mob.level(), pos, speed))));
            }
        }
        return this.modifier.requestTask(entries, listener);
    }
}
