package invmod.entity.ai.navigator;

import invmod.entity.ai.AttackerAI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The world as the pathfinder sees it: block states (unloaded chunks read as bedrock, so planning never
 * loads chunks) plus the nexus' extra data layer, which holds mob density and planned scaffolds, like the
 * original TerrainDataLayer.
 */
public final class TerrainView {
    private static final BlockState UNLOADED = Blocks.BEDROCK.defaultBlockState();
    private final Level level;
    private final @Nullable AttackerAI attackerAI;
    private final @Nullable BlockPos blockedScaffold;

    public TerrainView(Level level, @Nullable AttackerAI attackerAI) {
        this(level, attackerAI, null);
    }

    public TerrainView(Level level, @Nullable AttackerAI attackerAI, @Nullable BlockPos blockedScaffold) {
        this.level = level;
        this.attackerAI = attackerAI;
        this.blockedScaffold = blockedScaffold;
    }

    public Level level() {
        return this.level;
    }

    public BlockState state(BlockPos pos) {
        return this.level.isLoaded(pos) ? this.level.getBlockState(pos) : UNLOADED;
    }

    public BlockState state(int x, int y, int z) {
        return this.state(new BlockPos(x, y, z));
    }

    /** How many invaders stand at this block (0-7). Crowded spots are more expensive, spreading the horde. */
    public int density(int x, int y, int z) {
        return this.attackerAI == null ? 0 : this.attackerAI.densityAt(x, y, z);
    }

    /** Whether the nexus has planned a scaffold column through this block. */
    public boolean isScaffold(int x, int y, int z) {
        if (this.attackerAI == null) {
            return false;
        }
        if (this.blockedScaffold != null && this.blockedScaffold.getX() == x && this.blockedScaffold.getZ() == z) {
            return false;
        }
        return this.attackerAI.scaffoldAt(x, y, z) != null;
    }

    public @Nullable AttackerAI attackerAI() {
        return this.attackerAI;
    }
}
