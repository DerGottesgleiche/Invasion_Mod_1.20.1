package invmod.registry;

import invmod.Invasion;
import invmod.block.NexusBlock;
import invmod.block.TrapBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Invasion.MODID);

    public static final DeferredBlock<NexusBlock> NEXUS = BLOCKS.registerBlock("blocknexus", NexusBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 6000000.0F).sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(NexusBlock.ACTIVE) ? 10 : 0)
                    .pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<TrapBlock> TRAP = BLOCKS.registerBlock("trap", TrapBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.POPPED));

    private ModBlocks() {
    }
}
