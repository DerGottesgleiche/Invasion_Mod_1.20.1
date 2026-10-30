package invmod.client;

import invmod.Invasion;
import invmod.client.model.ImpModel;
import invmod.client.model.ThrowerModel;
import invmod.client.render.IMZombieRenderer;
import invmod.client.render.SimpleRenderers;
import invmod.registry.ModEntities;
import invmod.registry.ModMenus;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = Invasion.MODID, dist = Dist.CLIENT)
public class InvasionClient {
    public InvasionClient(IEventBus modBus) {
        modBus.addListener(InvasionClient::registerLayers);
        modBus.addListener(InvasionClient::registerRenderers);
        modBus.addListener(InvasionClient::registerScreens);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModLayers.BIPED_64X32,
                () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32));
        event.registerLayerDefinition(ModLayers.IMP, ImpModel::createBodyLayer);
        event.registerLayerDefinition(ModLayers.THROWER, ThrowerModel::createBodyLayer);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ZOMBIE.get(), IMZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.ZOMBIE_PIGMAN.get(), IMZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.SKELETON.get(), SimpleRenderers.Skeleton::new);
        event.registerEntityRenderer(ModEntities.SPIDER.get(), SimpleRenderers.Spider::new);
        event.registerEntityRenderer(ModEntities.CREEPER.get(), SimpleRenderers.Creeper::new);
        event.registerEntityRenderer(ModEntities.PIG_ENGY.get(), SimpleRenderers.PigEngy::new);
        event.registerEntityRenderer(ModEntities.THROWER.get(), SimpleRenderers.Thrower::new);
        event.registerEntityRenderer(ModEntities.IMP.get(), SimpleRenderers.Imp::new);
        event.registerEntityRenderer(ModEntities.WOLF.get(), SimpleRenderers.NexusWolf::new);
        event.registerEntityRenderer(ModEntities.BOULDER.get(), ThrownItemRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.NEXUS.get(), NexusScreen::new);
    }
}
