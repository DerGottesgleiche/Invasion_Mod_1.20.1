package invmod;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DROP_DESTRUCTED_BLOCKS = BUILDER
            .comment("Whether blocks dug out by invaders drop as items.")
            .define("dropDestructedBlocks", true);
    public static final ModConfigSpec.BooleanValue KILL_BOUND_PLAYERS = BUILDER
            .comment("Whether players bound to a nexus die when it is destroyed (the original mod's rule).")
            .define("killBoundPlayersOnNexusDestruction", true);
    public static final ModConfigSpec.IntValue MIN_DAYS_BETWEEN_ATTACKS = BUILDER
            .comment("Continuous mode: minimum number of days between attacks.")
            .defineInRange("minDaysBetweenAttacks", 2, 0, 100);
    public static final ModConfigSpec.IntValue MAX_DAYS_BETWEEN_ATTACKS = BUILDER
            .comment("Continuous mode: maximum number of days between attacks.")
            .defineInRange("maxDaysBetweenAttacks", 3, 0, 100);
    public static final ModConfigSpec.IntValue MOB_SENSE_RANGE = BUILDER
            .comment("Range in blocks within which invaders notice players without seeing them.")
            .defineInRange("mobSenseRange", 12, 0, 64);
    public static final ModConfigSpec.IntValue MOB_SIGHT_RANGE = BUILDER
            .comment("Range in blocks within which invaders attack players they can see.")
            .defineInRange("mobSightRange", 20, 0, 64);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static int maxDays() {
        return Math.max(MIN_DAYS_BETWEEN_ATTACKS.get(), MAX_DAYS_BETWEEN_ATTACKS.get());
    }
}
