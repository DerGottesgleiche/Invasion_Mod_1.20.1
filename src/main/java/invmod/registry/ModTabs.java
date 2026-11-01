package invmod.registry;

import invmod.Invasion;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Invasion.MODID);

    public static final Supplier<CreativeModeTab> INVASION = TABS.register("invasion", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mod_invasion"))
            .icon(() -> new ItemStack(ModItems.STRONG_CATALYST.get()))
            .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    private ModTabs() {
    }
}
