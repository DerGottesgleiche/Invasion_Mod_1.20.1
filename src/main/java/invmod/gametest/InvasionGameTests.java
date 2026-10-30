package invmod.gametest;

import invmod.Invasion;
import invmod.block.NexusBlockEntity;
import invmod.entity.IMPigEngy;
import invmod.entity.IMZombie;
import invmod.entity.Invader;
import invmod.registry.ModBlocks;
import invmod.registry.ModEntities;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Headless checks of the core loop, run with the gameTestServer run configuration. They only exist
 * when game tests are enabled.
 */
public final class InvasionGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, Invasion.MODID);
    private static final Identifier ARENA = id("arena");

    private InvasionGameTests() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Invasion.MODID, path);
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register("wave_spawns_invaders", () -> InvasionGameTests::waveSpawnsInvaders);
        FUNCTIONS.register("zombie_digs_to_nexus", () -> InvasionGameTests::zombieDigsToNexus);
        FUNCTIONS.register("engineer_builds_ladder", () -> InvasionGameTests::engineerBuildsLadder);
        FUNCTIONS.register("invaders_reach_nexus", () -> InvasionGameTests::invadersReachNexus);
        FUNCTIONS.register("zombie_walks_to_nexus", () -> InvasionGameTests::zombieWalksToNexus);
        FUNCTIONS.register(modBus);
        modBus.addListener(InvasionGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(id("default"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        test(event, env, "wave_spawns_invaders", 600);
        test(event, env, "zombie_digs_to_nexus", 1800);
        test(event, env, "engineer_builds_ladder", 1200);
        test(event, env, "invaders_reach_nexus", 4000);
        test(event, env, "zombie_walks_to_nexus", 800);
    }

    private static void test(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env, String name, int maxTicks) {
        event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                new TestData<>(env, ARENA, maxTicks, 0, true)));
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }

    /** A 1-wide corridor along x at z=2, closed with bedrock so the only way forward is through it. */
    private static void corridor(GameTestHelper helper, int height) {
        for (int x = 0; x < 15; x++) {
            for (int y = 1; y <= height; y++) {
                helper.setBlock(x, y, 1, Blocks.BEDROCK);
                helper.setBlock(x, y, 3, Blocks.BEDROCK);
            }
            helper.setBlock(x, height + 1, 2, Blocks.BEDROCK);
        }
        for (int y = 1; y <= height; y++) {
            helper.setBlock(0, y, 2, Blocks.BEDROCK);
        }
    }

    private static NexusBlockEntity placeNexus(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, ModBlocks.NEXUS.get());
        return helper.getBlockEntity(rel, NexusBlockEntity.class);
    }

    private static List<Mob> boundMobs(GameTestHelper helper, NexusBlockEntity nexus) {
        return helper.getLevel().getEntitiesOfClass(Mob.class, new AABB(nexus.getBlockPos()).inflate(40),
                m -> m instanceof Invader inv && nexus.getBlockPos().equals(inv.getNexusPos()));
    }

    /** Activating a nexus starts wave 1, which spawns nexus-bound invaders around it. */
    private static void waveSpawnsInvaders(GameTestHelper helper) {
        floor(helper);
        NexusBlockEntity nexus = placeNexus(helper, new BlockPos(7, 1, 2));
        nexus.setSpawnRadius(12);
        nexus.debugStartInvasion(1);
        helper.succeedWhen(() -> {
            helper.assertTrue(nexus.getMode() == NexusBlockEntity.MODE_INVASION, "nexus is not in invasion mode");
            helper.assertTrue(nexus.getCurrentWave() == 1, "nexus is not on wave 1");
            helper.assertTrue(!boundMobs(helper, nexus).isEmpty(), "no invaders spawned yet");
            Invasion.LOGGER.info("[test] wave 1 spawned {} after {} ticks", boundMobs(helper, nexus), helper.getTick());
        });
    }

    /** On open ground a zombie walks straight up to the nexus and hits it. */
    private static void zombieWalksToNexus(GameTestHelper helper) {
        floor(helper);
        NexusBlockEntity nexus = placeNexus(helper, new BlockPos(12, 1, 2));
        IMZombie zombie = helper.spawn(ModEntities.ZOMBIE.get(), new BlockPos(1, 1, 2));
        zombie.invaderData().setNexusPos(nexus.getBlockPos());
        zombie.setupInvader(1, 1, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(nexus.getHp() < 100, "the zombie has not hit the nexus yet");
            Invasion.LOGGER.info("[test] zombie walked to and hit the nexus after {} ticks", helper.getTick());
        });
    }

    /**
     * Invaders spawned 24 blocks out on open ground walk in and start hitting the nexus. The surrounding
     * chunks are force-loaded because the test server only ticks entities right around each test.
     */
    private static void invadersReachNexus(GameTestHelper helper) {
        BlockPos rel = new BlockPos(7, 0, -10);
        BlockPos abs = helper.absolutePos(rel);
        // Put the nexus on the natural ground level, where the spawn points are.
        while (helper.getLevel().getBlockState(abs.below()).isAir() && abs.getY() > helper.getLevel().getMinY()) {
            abs = abs.below();
        }
        helper.getLevel().setBlockAndUpdate(abs, ModBlocks.NEXUS.get().defaultBlockState());
        NexusBlockEntity nexus = (NexusBlockEntity) helper.getLevel().getBlockEntity(abs);
        int cx = abs.getX() >> 4;
        int cz = abs.getZ() >> 4;
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                helper.getLevel().setChunkForced(x, z, true);
            }
        }
        nexus.setSpawnRadius(24);
        nexus.debugStartInvasion(1);
        helper.succeedWhen(() -> {
            helper.assertTrue(nexus.getHp() < 100, "no invader has reached the nexus yet ("
                    + boundMobs(helper, nexus).size() + " alive)");
            Invasion.LOGGER.info("[test] invaders reached the nexus (hp {}, {} alive) after {} ticks", nexus.getHp(),
                    boundMobs(helper, nexus).size(), helper.getTick());
            helper.getLevel().setBlockAndUpdate(nexus.getBlockPos(), Blocks.AIR.defaultBlockState());
        });
    }

    /** A digging zombie tunnels through a cobblestone wall and attacks the nexus behind it. */
    private static void zombieDigsToNexus(GameTestHelper helper) {
        floor(helper);
        corridor(helper, 2);
        BlockPos wallLow = new BlockPos(6, 1, 2);
        BlockPos wallHigh = new BlockPos(6, 2, 2);
        helper.setBlock(wallLow, Blocks.COBBLESTONE);
        helper.setBlock(wallHigh, Blocks.COBBLESTONE);
        NexusBlockEntity nexus = placeNexus(helper, new BlockPos(12, 1, 2));
        IMZombie zombie = helper.spawn(ModEntities.ZOMBIE.get(), new BlockPos(2, 1, 2));
        zombie.invaderData().setNexusPos(nexus.getBlockPos());
        zombie.setupInvader(1, 0, 0);
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(Blocks.COBBLESTONE, wallLow);
            helper.assertBlockNotPresent(Blocks.COBBLESTONE, wallHigh);
            helper.assertTrue(nexus.getHp() < 100, "nexus has not been hit yet");
            Invasion.LOGGER.info("[test] zombie dug through and hit the nexus (hp {}) after {} ticks", nexus.getHp(), helper.getTick());
        });
    }

    /** A pigman engineer stuck at a wall below a raised nexus builds a ladder up the wall. */
    private static void engineerBuildsLadder(GameTestHelper helper) {
        floor(helper);
        for (int x = 6; x < 15; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(x, y, z, Blocks.BEDROCK);
                }
            }
        }
        NexusBlockEntity nexus = placeNexus(helper, new BlockPos(10, 4, 2));
        IMPigEngy engy = helper.spawn(ModEntities.PIG_ENGY.get(), new BlockPos(3, 1, 2));
        engy.invaderData().setNexusPos(nexus.getBlockPos());
        engy.setupInvader(1, 0, 0);
        helper.succeedWhen(() -> {
            boolean ladder = false;
            for (int z = 0; z < 5; z++) {
                Block block = helper.getBlockState(new BlockPos(5, 1, z)).getBlock();
                ladder |= block == Blocks.LADDER;
            }
            helper.assertTrue(ladder, "no ladder against the wall yet");
            Invasion.LOGGER.info("[test] engineer placed a ladder after {} ticks", helper.getTick());
        });
    }
}
