package invmod.registry;

import invmod.Invasion;
import invmod.block.NexusBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Invasion.MODID);

    public static final Supplier<BlockEntityType<NexusBlockEntity>> NEXUS = BLOCK_ENTITIES.register("nexus",
            () -> new BlockEntityType<>(NexusBlockEntity::new, ModBlocks.NEXUS.get()));

    private ModBlockEntities() {
    }
}
