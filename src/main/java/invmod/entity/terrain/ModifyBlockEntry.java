package invmod.entity.terrain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** One planned block change: dig it out ({@code newState == null}) or place {@code newState}; takes {@code cost} ticks. */
public record ModifyBlockEntry(BlockPos pos, @Nullable BlockState newState, int cost) {
}
