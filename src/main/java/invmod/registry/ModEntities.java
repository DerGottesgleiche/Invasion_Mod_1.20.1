package invmod.registry;

import invmod.Invasion;
import invmod.entity.Boulder;
import invmod.entity.IMCreeper;
import invmod.entity.IMImp;
import invmod.entity.IMPigEngy;
import invmod.entity.IMSkeleton;
import invmod.entity.IMSpider;
import invmod.entity.IMThrower;
import invmod.entity.IMWolf;
import invmod.entity.IMZombie;
import invmod.entity.IMZombiePigman;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Invasion.MODID);

    public static final Supplier<EntityType<IMZombie>> ZOMBIE = ENTITIES.registerEntityType("im_zombie",
            IMZombie::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(10));
    public static final Supplier<EntityType<IMZombiePigman>> ZOMBIE_PIGMAN = ENTITIES.registerEntityType("im_zombie_pigman",
            IMZombiePigman::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).eyeHeight(1.74F).fireImmune().clientTrackingRange(10));
    public static final Supplier<EntityType<IMSkeleton>> SKELETON = ENTITIES.registerEntityType("im_skeleton",
            IMSkeleton::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(10));
    public static final Supplier<EntityType<IMSpider>> SPIDER = ENTITIES.registerEntityType("im_spider",
            IMSpider::new, MobCategory.MONSTER, b -> b.sized(1.4F, 0.9F).eyeHeight(0.65F).clientTrackingRange(10));
    public static final Supplier<EntityType<IMCreeper>> CREEPER = ENTITIES.registerEntityType("im_creeper",
            IMCreeper::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.7F).clientTrackingRange(10));
    public static final Supplier<EntityType<IMPigEngy>> PIG_ENGY = ENTITIES.registerEntityType("im_pig_engy",
            IMPigEngy::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).eyeHeight(1.74F).fireImmune().clientTrackingRange(10));
    public static final Supplier<EntityType<IMThrower>> THROWER = ENTITIES.registerEntityType("im_thrower",
            IMThrower::new, MobCategory.MONSTER, b -> b.sized(1.5F, 1.95F).eyeHeight(1.6F).clientTrackingRange(10));
    public static final Supplier<EntityType<IMImp>> IMP = ENTITIES.registerEntityType("im_imp",
            IMImp::new, MobCategory.MONSTER, b -> b.sized(0.5F, 1.0F).eyeHeight(0.8F).fireImmune().clientTrackingRange(10));
    public static final Supplier<EntityType<IMWolf>> WOLF = ENTITIES.registerEntityType("im_wolf",
            IMWolf::new, MobCategory.CREATURE, b -> b.sized(0.6F, 0.85F).eyeHeight(0.68F).clientTrackingRange(10));
    public static final Supplier<EntityType<Boulder>> BOULDER = ENTITIES.registerEntityType("im_boulder",
            Boulder::new, MobCategory.MISC, b -> b.sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(4));

    private ModEntities() {
    }
}
