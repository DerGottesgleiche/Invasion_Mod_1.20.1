package invmod.registry;

import invmod.Invasion;
import invmod.block.NexusMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Invasion.MODID);

    public static final Supplier<MenuType<NexusMenu>> NEXUS = MENUS.register("nexus",
            () -> new MenuType<>(NexusMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }
}
