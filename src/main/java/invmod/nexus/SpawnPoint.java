package invmod.nexus;

import invmod.util.IPolarAngle;
import net.minecraft.core.BlockPos;

public record SpawnPoint(BlockPos pos, int angle, SpawnType type) implements IPolarAngle {
    @Override
    public int getAngle() {
        return this.angle;
    }
}
