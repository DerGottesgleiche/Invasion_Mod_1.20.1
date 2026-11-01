package invmod.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A ladder column the attacker AI wants the engineers to build up to a high nexus: a pillar of blocks on one
 * side with a ladder on it, and platform rings every few layers (original Scaffold).
 */
public class Scaffold {
    private BlockPos pos;
    private int targetHeight;
    private Direction orientation = Direction.EAST;
    private int[] platforms = new int[0];
    private float initialCompletion = 0.01F;
    private float percentCompleted;
    private float percentIntact;

    public Scaffold(BlockPos pos, int height) {
        this.pos = pos;
        this.targetHeight = height;
        this.calcPlatforms();
    }

    public BlockPos pos() {
        return this.pos;
    }

    public int targetHeight() {
        return this.targetHeight;
    }

    public Direction orientation() {
        return this.orientation;
    }

    public void setOrientation(Direction orientation) {
        this.orientation = orientation;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public void setHeight(int height) {
        this.targetHeight = height;
        this.calcPlatforms();
    }

    public boolean contains(int x, int y, int z) {
        return this.pos.getX() == x && this.pos.getZ() == z && y >= this.pos.getY() && y <= this.pos.getY() + this.targetHeight;
    }

    public boolean isLayerPlatform(int height) {
        if (height == this.targetHeight - 1) {
            return true;
        }
        for (int p : this.platforms) {
            if (p == height) {
                return true;
            }
        }
        return false;
    }

    public float percentIntact() {
        return this.percentIntact;
    }

    public float percentCompleted() {
        return this.percentCompleted;
    }

    public void setInitialIntegrity(Level level) {
        this.initialCompletion = Math.max(0.01F, this.evaluateIntegrity(level));
    }

    public void updateStatus(Level level) {
        this.percentIntact = (this.evaluateIntegrity(level) - this.initialCompletion) / (1.0F - this.initialCompletion);
        this.percentCompleted = Math.max(this.percentCompleted, this.percentIntact);
    }

    private void calcPlatforms() {
        int spanning = this.targetHeight < 16 ? this.targetHeight / 4 - 1 : this.targetHeight / 5 - 1;
        if (spanning <= 0) {
            this.platforms = new int[0];
            return;
        }
        int avgSpace = this.targetHeight / (spanning + 1);
        this.platforms = new int[spanning];
        for (int i = 0; i < spanning; i++) {
            this.platforms[i] = avgSpace * (i + 1) - 1;
        }
    }

    private float evaluateIntegrity(Level level) {
        int main = 0;
        int ladders = 0;
        int platformBlocks = 0;
        for (int i = 0; i < this.targetHeight; i++) {
            BlockPos layer = this.pos.above(i);
            BlockPos side = layer.relative(this.orientation);
            if (level.getBlockState(side).isCollisionShapeFullBlock(level, side)) {
                main++;
            }
            if (level.getBlockState(layer).is(Blocks.LADDER)) {
                ladders++;
            }
            if (this.isLayerPlatform(i)) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos ring = layer.offset(dx, 0, dz);
                        if ((dx != 0 || dz != 0) && level.getBlockState(ring).isFaceSturdy(level, ring, Direction.UP)) {
                            platformBlocks++;
                        }
                    }
                }
            }
        }
        float mainPercent = this.targetHeight > 0 ? (float) main / this.targetHeight : 0.0F;
        float ladderPercent = this.targetHeight > 0 ? (float) ladders / this.targetHeight : 0.0F;
        return 0.7F * (0.7F * mainPercent + 0.3F * ladderPercent) + 0.3F * platformBlocks / ((this.platforms.length + 1) * 8.0F);
    }

    public void save(ValueOutput output) {
        output.putLong("Pos", this.pos.asLong());
        output.putInt("Height", this.targetHeight);
        output.putInt("Orientation", this.orientation.get3DDataValue());
        output.putFloat("Initial", this.initialCompletion);
        output.putFloat("Completed", this.percentCompleted);
    }

    public static Scaffold load(ValueInput input) {
        Scaffold scaffold = new Scaffold(BlockPos.of(input.getLongOr("Pos", 0L)), input.getIntOr("Height", 4));
        Direction dir = Direction.from3DDataValue(input.getIntOr("Orientation", 5));
        scaffold.orientation = dir.getAxis().isHorizontal() ? dir : Direction.EAST;
        scaffold.initialCompletion = input.getFloatOr("Initial", 0.01F);
        scaffold.percentCompleted = input.getFloatOr("Completed", 0.0F);
        return scaffold;
    }
}
