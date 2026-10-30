package invmod.client;

import invmod.Invasion;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class ModLayers {
    /** The classic 64x32 humanoid used by all the original mod's biped textures. */
    public static final ModelLayerLocation BIPED_64X32 = layer("biped_64x32");
    public static final ModelLayerLocation IMP = layer("imp");
    public static final ModelLayerLocation THROWER = layer("thrower");

    private ModLayers() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Invasion.MODID, name), "main");
    }

    public static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(Invasion.MODID, "textures/" + name + ".png");
    }
}
