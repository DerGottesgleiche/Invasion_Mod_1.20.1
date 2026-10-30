package invmod.registry;

import invmod.Invasion;
import invmod.block.TrapBlock;
import invmod.item.InfusedSwordItem;
import invmod.item.ProbeItem;
import invmod.item.SearingBowItem;
import invmod.item.StrangeBoneItem;
import invmod.item.TrapItem;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Invasion.MODID);

    public static final DeferredItem<?> NEXUS = ITEMS.registerSimpleBlockItem("blocknexus", ModBlocks.NEXUS);

    // Traps
    public static final DeferredItem<TrapItem> TRAP_EMPTY = ITEMS.registerItem("emptytrap",
            p -> new TrapItem(p, TrapBlock.TrapType.EMPTY));
    public static final DeferredItem<TrapItem> TRAP_FLAME = ITEMS.registerItem("flametrap",
            p -> new TrapItem(p, TrapBlock.TrapType.FLAME));
    public static final DeferredItem<TrapItem> TRAP_POISON = ITEMS.registerItem("poisontrap",
            p -> new TrapItem(p, TrapBlock.TrapType.POISON));
    public static final DeferredItem<TrapItem> TRAP_RIFT = ITEMS.registerItem("rifttrap",
            p -> new TrapItem(p, TrapBlock.TrapType.RIFT));

    // Crafting materials and catalysts
    public static final DeferredItem<Item> CATALYST_MIXTURE = ITEMS.registerSimpleItem("catalystmixture");
    public static final DeferredItem<Item> STABLE_CATALYST_MIXTURE = ITEMS.registerSimpleItem("stablecatalystmixture");
    public static final DeferredItem<Item> STRONG_CATALYST_MIXTURE = ITEMS.registerSimpleItem("strongcatalystmixture");
    public static final DeferredItem<Item> NEXUS_CATALYST = ITEMS.registerSimpleItem("nexuscatalyst",
            p -> p.stacksTo(1));
    public static final DeferredItem<Item> STABLE_NEXUS_CATALYST = ITEMS.registerSimpleItem("stablenexuscatalyst",
            p -> p.stacksTo(1));
    public static final DeferredItem<Item> STRONG_CATALYST = ITEMS.registerSimpleItem("strongcatalyst",
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> DAMPING_AGENT = ITEMS.registerSimpleItem("dampingagent", p -> p.stacksTo(1));
    public static final DeferredItem<Item> STRONG_DAMPING_AGENT = ITEMS.registerSimpleItem("strongdampingagent",
            p -> p.stacksTo(1));
    public static final DeferredItem<Item> PHASE_CRYSTAL = ITEMS.registerSimpleItem("phasecrystal", p -> p.stacksTo(1));
    public static final DeferredItem<Item> RIFT_FLUX = ITEMS.registerSimpleItem("riftflux",
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SMALL_REMNANTS = ITEMS.registerSimpleItem("smallremnants");
    public static final DeferredItem<Item> ENGY_HAMMER = ITEMS.registerSimpleItem("engyhammer", p -> p.stacksTo(1));

    // Tools
    public static final DeferredItem<InfusedSwordItem> INFUSED_SWORD = ITEMS.registerItem("infusedsword",
            InfusedSwordItem::new, p -> p.sword(ToolMaterial.DIAMOND, 3.0F, -2.4F).durability(40)
                    .rarity(Rarity.RARE));
    public static final DeferredItem<SearingBowItem> SEARING_BOW = ITEMS.registerItem("searingbow",
            SearingBowItem::new, p -> p.durability(384).enchantable(1).rarity(Rarity.RARE));
    public static final DeferredItem<ProbeItem> NEXUS_ADJUSTER = ITEMS.registerItem("nexusadjuster",
            p -> new ProbeItem(p, false), p -> p.stacksTo(1));
    public static final DeferredItem<ProbeItem> MATERIAL_PROBE = ITEMS.registerItem("materialprobe",
            p -> new ProbeItem(p, true), p -> p.stacksTo(1));
    public static final DeferredItem<StrangeBoneItem> STRANGE_BONE = ITEMS.registerItem("strangebone",
            StrangeBoneItem::new, p -> p.stacksTo(1));

    // Spawn eggs (they spawn tier 1 invaders that are not bound to a nexus)
    public static final DeferredItem<SpawnEggItem> ZOMBIE_EGG = egg("im_zombie_spawn_egg", ModEntities.ZOMBIE);
    public static final DeferredItem<SpawnEggItem> ZOMBIE_PIGMAN_EGG = egg("im_zombie_pigman_spawn_egg", ModEntities.ZOMBIE_PIGMAN);
    public static final DeferredItem<SpawnEggItem> SKELETON_EGG = egg("im_skeleton_spawn_egg", ModEntities.SKELETON);
    public static final DeferredItem<SpawnEggItem> SPIDER_EGG = egg("im_spider_spawn_egg", ModEntities.SPIDER);
    public static final DeferredItem<SpawnEggItem> CREEPER_EGG = egg("im_creeper_spawn_egg", ModEntities.CREEPER);
    public static final DeferredItem<SpawnEggItem> PIG_ENGY_EGG = egg("im_pig_engy_spawn_egg", ModEntities.PIG_ENGY);
    public static final DeferredItem<SpawnEggItem> THROWER_EGG = egg("im_thrower_spawn_egg", ModEntities.THROWER);
    public static final DeferredItem<SpawnEggItem> IMP_EGG = egg("im_imp_spawn_egg", ModEntities.IMP);
    public static final DeferredItem<SpawnEggItem> WOLF_EGG = egg("im_wolf_spawn_egg", ModEntities.WOLF);

    private ModItems() {
    }

    private static DeferredItem<SpawnEggItem> egg(String name, Supplier<? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }
}
