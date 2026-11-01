package invmod.registry;

import invmod.Invasion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Invasion.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BIGZOMBIE = register("bigzombie1");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME = register("chime1");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGG_HATCH = register("egghatch1");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL = register("fireball1");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUMBLE = register("rumble1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCRAPE = register("scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAP = register("zap");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Invasion.MODID, name)));
    }
}
