package invmod.client.render;

import invmod.client.ModLayers;
import invmod.entity.IMZombie;
import invmod.entity.IMZombiePigman;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/** Zombies and zombie pigmen with the original mod's textures on the classic 64x32 biped. */
public class IMZombieRenderer extends AbstractZombieRenderer<IMZombie, IMZombieRenderer.State, ZombieModel<IMZombieRenderer.State>> {
    private static final Identifier[] ZOMBIE_TEXTURES = {
            ModLayers.texture("zombie_old"), ModLayers.texture("zombiet1a"), ModLayers.texture("zombiet2"),
            ModLayers.texture("pigzombie64x32"), ModLayers.texture("zombiet2a"), ModLayers.texture("zombietar"),
            ModLayers.texture("zombiet3")};
    private static final Identifier[] PIGMAN_TEXTURES = {
            ModLayers.texture("pigzombie64x32"), ModLayers.texture("pigzombie64x32"), ModLayers.texture("zombiepigmant3")};

    public static class State extends ZombieRenderState {
        public Identifier texture = ZOMBIE_TEXTURES[0];
    }

    public IMZombieRenderer(EntityRendererProvider.Context context) {
        super(context,
                new ZombieModel<>(context.bakeLayer(ModLayers.BIPED_64X32)),
                new BabyZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
                ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), ZombieModel::new),
                ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, context.getModelSet(), BabyZombieModel::new));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(IMZombie entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Identifier[] textures = entity instanceof IMZombiePigman ? PIGMAN_TEXTURES : ZOMBIE_TEXTURES;
        int id = entity.getTextureId();
        state.texture = textures[id >= 0 && id < textures.length ? id : 0];
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return state.texture;
    }
}
