package invmod.entity;

import invmod.block.NexusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * Implemented by every Invasion mob. An invader is either bound to a nexus (spawned by a wave and
 * attacking it) or independent (spawn egg), in which case it behaves like an ordinary hostile mob.
 */
public interface Invader {
    /** What an invader may do to terrain on its way to the nexus. */
    enum TerrainSkill {
        NONE, DIG, BUILD_LADDERS
    }

    InvaderData invaderData();

    default Mob asMob() {
        return (Mob) this;
    }

    default @Nullable BlockPos getNexusPos() {
        return this.invaderData().getNexusPos();
    }

    default boolean isNexusBound() {
        return this.invaderData().getNexusPos() != null;
    }

    default @Nullable NexusBlockEntity getNexus() {
        BlockPos pos = this.getNexusPos();
        if (pos == null || !this.asMob().level().isLoaded(pos)) {
            return null;
        }
        return this.asMob().level().getBlockEntity(pos) instanceof NexusBlockEntity nexus ? nexus : null;
    }

    /** Called by the wave spawner after construction; sets tier/flavour/texture and applies stats. */
    void setupInvader(int tier, int flavour, int texture);

    default int getTier() {
        return this.invaderData().getTier(this.asMob());
    }

    default int getFlavour() {
        return this.invaderData().getFlavour(this.asMob());
    }

    default int getTextureId() {
        return this.invaderData().getTexture(this.asMob());
    }

    default TerrainSkill terrainSkill() {
        return TerrainSkill.NONE;
    }

    /** Multiplier on how fast this mob digs. 1 is a normal zombie. */
    default float digSpeed() {
        return 1.0F;
    }

    /** Damage dealt to the nexus per hit. */
    default int nexusDamage() {
        return 2;
    }

    default String speciesName() {
        return this.asMob().getType().getDescription().getString();
    }
}
