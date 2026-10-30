package invmod.nexus;

import invmod.entity.Invader;
import invmod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Turns an {@link EntityConstruct} from a wave definition into a live invader. */
public final class MobBuilder {
    private MobBuilder() {
    }

    public static EntityType<? extends Mob> typeFor(IMEntityType type) {
        return switch (type) {
            case ZOMBIE -> ModEntities.ZOMBIE.get();
            case ZOMBIEPIGMAN -> ModEntities.ZOMBIE_PIGMAN.get();
            case SPIDER -> ModEntities.SPIDER.get();
            case SKELETON -> ModEntities.SKELETON.get();
            case PIG_ENGINEER -> ModEntities.PIG_ENGY.get();
            case THROWER -> ModEntities.THROWER.get();
            case IMP -> ModEntities.IMP.get();
            case CREEPER -> ModEntities.CREEPER.get();
        };
    }

    public static @Nullable Mob createMob(EntityConstruct construct, Level level, @Nullable BlockPos nexusPos) {
        Mob mob = typeFor(construct.mobType()).create(level, EntitySpawnReason.EVENT);
        if (mob instanceof Invader invader) {
            invader.invaderData().setNexusPos(nexusPos);
            invader.setupInvader(construct.tier(), construct.flavour(), construct.texture());
            mob.setPersistenceRequired();
            return mob;
        }
        return null;
    }
}
