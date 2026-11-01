package invmod;

import com.mojang.logging.LogUtils;
import invmod.block.NexusBlockEntity;
import invmod.command.InvasionCommand;
import invmod.entity.IMCreeper;
import invmod.entity.IMImp;
import invmod.entity.IMPigEngy;
import invmod.entity.IMSkeleton;
import invmod.entity.IMSpider;
import invmod.entity.IMThrower;
import invmod.entity.IMWolf;
import invmod.entity.IMZombie;
import invmod.entity.Invader;
import invmod.registry.ModBlockEntities;
import invmod.registry.ModBlocks;
import invmod.registry.ModEntities;
import invmod.registry.ModItems;
import invmod.registry.ModMenus;
import invmod.registry.ModSounds;
import invmod.registry.ModTabs;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.slf4j.Logger;

@Mod(Invasion.MODID)
public class Invasion {
    public static final String MODID = "mod_invasion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Invasion(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModTabs.TABS.register(modBus);
        modBus.addListener(Invasion::registerAttributes);
        container.registerConfig(ModConfig.Type.LOCAL, Config.SPEC);

        invmod.gametest.InvasionGameTests.register(modBus);

        NeoForge.EVENT_BUS.addListener(Invasion::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(Invasion::onRegisterCommands);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ZOMBIE.get(), IMZombie.createAttributes().build());
        event.put(ModEntities.ZOMBIE_PIGMAN.get(), IMZombie.createAttributes().build());
        event.put(ModEntities.SKELETON.get(), IMSkeleton.createAttributes().build());
        event.put(ModEntities.SPIDER.get(), IMSpider.createAttributes().build());
        event.put(ModEntities.CREEPER.get(), IMCreeper.createAttributes().build());
        event.put(ModEntities.PIG_ENGY.get(), IMPigEngy.createAttributes().build());
        event.put(ModEntities.THROWER.get(), IMThrower.createAttributes().build());
        event.put(ModEntities.IMP.get(), IMImp.createAttributes().build());
        event.put(ModEntities.WOLF.get(), IMWolf.createAttributes().build());
    }

    /** Every invader that dies counts towards stabilising its nexus' rift. */
    private static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && mob instanceof Invader invader && !mob.level().isClientSide()) {
            NexusBlockEntity nexus = invader.getNexus();
            if (nexus != null) {
                nexus.registerMobDied();
            }
        }
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        InvasionCommand.register(event.getDispatcher());
    }
}
