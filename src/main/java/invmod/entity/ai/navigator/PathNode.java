package invmod.entity.ai.navigator;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/** A node of an invader path: the block the mob's feet are in, plus the action needed to get there. */
public final class PathNode {
    public final int x;
    public final int y;
    public final int z;
    public final PathAction action;
    int heapIndex = -1;
    float totalCost;
    float estimate;
    float score;
    boolean closed;
    @Nullable PathNode previous;

    public PathNode(int x, int y, int z, PathAction action) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.action = action;
    }

    public BlockPos pos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public float distanceTo(PathNode other) {
        return this.distanceTo(other.x, other.y, other.z);
    }

    public float distanceTo(double ox, double oy, double oz) {
        double dx = ox - this.x;
        double dy = oy - this.y;
        double dz = oz - this.z;
        return Mth.sqrt((float) (dx * dx + dy * dy + dz * dz));
    }

    public boolean samePos(PathNode other) {
        return this.x == other.x && this.y == other.y && this.z == other.z;
    }

    public boolean inHeap() {
        return this.heapIndex >= 0;
    }

    public @Nullable PathNode previous() {
        return this.previous;
    }

    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ") " + this.action;
    }
}
