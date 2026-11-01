package invmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import invmod.client.ModLayers;
import invmod.client.model.ImpModel;
import invmod.client.model.ThrowerModel;
import invmod.entity.IMImp;
import invmod.entity.IMPigEngy;
import invmod.entity.IMSkeleton;
import invmod.entity.IMSpider;
import invmod.entity.IMThrower;
import invmod.entity.IMWolf;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.wolf.Wolf;

/** The renderers that only swap a texture (or add a scale) on an existing model. */
public final class SimpleRenderers {
    private SimpleRenderers() {
    }

    public static class Skeleton extends SkeletonRenderer {
        private static final Identifier TEXTURE = ModLayers.texture("skeleton");

        public Skeleton(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(SkeletonRenderState state) {
            return TEXTURE;
        }
    }

    public static class Creeper extends CreeperRenderer {
        private static final Identifier TEXTURE = ModLayers.texture("creeper");

        public Creeper(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public Identifier getTextureLocation(CreeperRenderState state) {
            return TEXTURE;
        }
    }

    public static class NexusWolf extends WolfRenderer {
        private static final Identifier TEXTURE = ModLayers.texture("wolf_tame_nexus");

        public NexusWolf(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void extractRenderState(Wolf entity, WolfRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            if (entity instanceof IMWolf) {
                state.texture = TEXTURE;
            }
        }
    }

    public static class SpiderState extends LivingEntityRenderState {
        public Identifier texture = VANILLA_SPIDER;
    }

    private static final Identifier VANILLA_SPIDER = Identifier.withDefaultNamespace("textures/entity/spider/spider.png");

    public static class Spider extends SpiderRenderer<IMSpider> {
        private static final Identifier JUMPING = ModLayers.texture("spidert2");
        private static final Identifier MOTHER = ModLayers.texture("spidert2b");

        public Spider(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new SpiderState();
        }

        @Override
        public void extractRenderState(IMSpider entity, LivingEntityRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            if (state instanceof SpiderState spider) {
                spider.texture = entity.isJumpingSpider() ? JUMPING : entity.isMotherSpider() ? MOTHER : VANILLA_SPIDER;
            }
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return state instanceof SpiderState spider ? spider.texture : VANILLA_SPIDER;
        }
    }

    public static class PigEngyState extends HumanoidRenderState {
        public boolean tier2;
    }

    public static class PigEngy extends HumanoidMobRenderer<IMPigEngy, PigEngyState, HumanoidModel<PigEngyState>> {
        private static final Identifier T1 = ModLayers.texture("pigengt1");
        private static final Identifier T2 = ModLayers.texture("pigengt2");

        public PigEngy(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModLayers.BIPED_64X32)), 0.5F);
        }

        @Override
        public PigEngyState createRenderState() {
            return new PigEngyState();
        }

        @Override
        public void extractRenderState(IMPigEngy entity, PigEngyState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.tier2 = entity.getTextureId() == 1;
        }

        @Override
        public Identifier getTextureLocation(PigEngyState state) {
            return state.tier2 ? T2 : T1;
        }
    }

    public static class Imp extends MobRenderer<IMImp, LivingEntityRenderState, ImpModel> {
        private static final Identifier TEXTURE = ModLayers.texture("imp");

        public Imp(EntityRendererProvider.Context context) {
            super(context, new ImpModel(context.bakeLayer(ModLayers.IMP)), 0.3F);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return TEXTURE;
        }
    }

    public static class ThrowerState extends LivingEntityRenderState {
        public boolean tier2;
    }

    public static class Thrower extends MobRenderer<IMThrower, ThrowerState, ThrowerModel> {
        private static final Identifier T1 = ModLayers.texture("throwert1");
        private static final Identifier T2 = ModLayers.texture("throwert2");

        public Thrower(EntityRendererProvider.Context context) {
            super(context, new ThrowerModel(context.bakeLayer(ModLayers.THROWER)), 1.0F);
        }

        @Override
        public ThrowerState createRenderState() {
            return new ThrowerState();
        }

        @Override
        public void extractRenderState(IMThrower entity, ThrowerState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.tier2 = entity.getTextureId() == 1;
        }

        @Override
        protected void scale(ThrowerState state, PoseStack poseStack) {
            poseStack.scale(2.4F, 2.8F, 2.4F);
        }

        @Override
        public Identifier getTextureLocation(ThrowerState state) {
            return state.tier2 ? T2 : T1;
        }
    }
}
