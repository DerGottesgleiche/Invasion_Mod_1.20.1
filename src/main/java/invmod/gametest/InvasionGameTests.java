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
import net.minecraft.world.level.block.state.BlockState;
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
        FUNCTIONS.register("zombie_digs_weakest_spot", () -> InvasionGameTests::zombieDigsWeakestSpot);
        FUNCTIONS.register("engineer_climbs_to_nexus", () -> InvasionGameTests::engineerClimbsToNexus);
        FUNCTIONS.register("horde_uses_engineer_ladder", () -> InvasionGameTests::hordeUsesEngineerLadder);
        FUNCTIONS.register("engineer_tower_under_overhang", () -> InvasionGameTests::engineerTowerUnderOverhang);
        FUNCTIONS.register("engineer_builds_scaffold", () -> InvasionGameTests::engineerBuildsScaffold);
        FUNCTIONS.register("tower_with_overhang", () -> InvasionGameTests::towerWithOverhang);
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
        test(event, env, "zombie_digs_weakest_spot", 1500);
        test(event, env, "engineer_climbs_to_nexus", 2400);
        test(event, env, "horde_uses_engineer_ladder", 3000);
        test(event, env, "engineer_tower_under_overhang", 4000);
        test(event, env, "engineer_builds_scaffold", 8000);
        test(event, env, "tower_with_overhang", 6000);
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

    /**
     * A wall of stone with one dirt column: the zombie plans its tunnel through the dirt (weakest spot) instead
     * of digging the stone straight ahead, as the original pathfinder did.
     */
    private static void zombieDigsWeakestSpot(GameTestHelper helper) {
        floor(helper);
        for (int x = 0; x < 15; x++) {
            for (int y = 1; y <= 2; y++) {
                helper.setBlock(x, y, 0, Blocks.BEDROCK);
                helper.setBlock(x, y, 4, Blocks.BEDROCK);
            }
            for (int z = 0; z < 5; z++) {
                helper.setBlock(x, 3, z, Blocks.BEDROCK);
            }
        }
        for (int z = 1; z <= 3; z++) {
            for (int y = 1; y <= 2; y++) {
                helper.setBlock(0, y, z, Blocks.BEDROCK);
                helper.setBlock(7, y, z, z == 3 ? Blocks.DIRT : Blocks.STONE);
            }
        }
        NexusBlockEntity nexus = placeNexus(helper, new BlockPos(12, 1, 2));
        IMZombie zombie = helper.spawn(ModEntities.ZOMBIE.get(), new BlockPos(2, 1, 1));
        zombie.invaderData().setNexusPos(nexus.getBlockPos());
        zombie.setupInvader(1, 0, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(nexus.getHp() < 100, "the zombie has not hit the nexus yet");
            for (int z = 1; z <= 2; z++) {
                for (int y = 1; y <= 2; y++) {
                    helper.assertBlockPresent(Blocks.STONE, new BlockPos(7, y, z));
                }
            }
            Invasion.LOGGER.info("[test] zombie went through the dirt, stone untouched, after {} ticks", helper.getTick());
        });
    }

    /** An engineer below a nexus on top of a 3-high wall builds a ladder, climbs it and hits the nexus. */
    private static void engineerClimbsToNexus(GameTestHelper helper) {
        floor(helper);
        raisedNexusWall(helper);
        NexusBlockEntity nexus = placeNexus(helper, new BlockPos(9, 4, 2));
        IMPigEngy engy = helper.spawn(ModEntities.PIG_ENGY.get(), new BlockPos(2, 1, 2));
        engy.invaderData().setNexusPos(nexus.getBlockPos());
        engy.setupInvader(1, 0, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(nexus.getHp() < 100, "the engineer has not reached the nexus yet (at " + engy.blockPosition() + ")");
            Invasion.LOGGER.info("[test] engineer climbed up and hit the nexus after {} ticks", helper.getTick());
        });
    }

    /** A zombie that cannot dig gets up the wall on the ladder an engineer builds. */
    private static void hordeUsesEngineerLadder(GameTestHelper helper) {
        floor(helper);
        raisedNexusWall(helper);
        NexusBlockEntity nexus = placeNexus(helper, new BlockPos(9, 4, 2));
        IMPigEngy engy = helper.spawn(ModEntities.PIG_ENGY.get(), new BlockPos(2, 1, 2));
        engy.invaderData().setNexusPos(nexus.getBlockPos());
        engy.setupInvader(1, 0, 0);
        IMZombie zombie = helper.spawn(ModEntities.ZOMBIE.get(), new BlockPos(2, 1, 3));
        zombie.invaderData().setNexusPos(nexus.getBlockPos());
        zombie.setupInvader(1, 1, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.getY() >= helper.absolutePos(new BlockPos(0, 4, 0)).getY(),
                    "the zombie is not on top of the wall yet (at " + zombie.blockPosition() + ")");
            Invasion.LOGGER.info("[test] non-digging zombie climbed the engineer's ladder after {} ticks", helper.getTick());
        });
    }

    /**
     * A nexus on a platform overhanging 4 blocks up: the engineer builds a ladder tower (pillar plus ladder)
     * under the overhang and climbs onto it, the original's LADDER_TOWER action.
     */
    private static void engineerTowerUnderOverhang(GameTestHelper helper) {
        floatingNexusOutside(helper, 4, new BlockPos(-300, 0, -300));
    }

    /**
     * A nexus on a platform 8 blocks up, too high for a ladder tower: the attacker AI plans a scaffold and the
     * engineer builds it.
     */
    private static void engineerBuildsScaffold(GameTestHelper helper) {
        floatingNexusOutside(helper, 8, new BlockPos(-300, 0, -400));
    }

    /**
     * The situation from the owner's game: a sandstone tower whose top layer overhangs by one block, with the
     * nexus on top. Engineer and zombies start at the foot.
     */
    private static void towerWithOverhang(GameTestHelper helper) {
        ServerLevelAccess world = new ServerLevelAccess(helper, new BlockPos(-300, 0, -500));
        BlockPos ground = world.ground();
        int height = 7;
        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.set(ground.offset(dx, y, dz), Blocks.SANDSTONE);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.set(ground.offset(dx, height, dz), Blocks.SANDSTONE);
            }
        }
        world.set(ground.above(height + 1), ModBlocks.NEXUS.get());
        NexusBlockEntity nexus = (NexusBlockEntity) helper.getLevel().getBlockEntity(ground.above(height + 1));
        IMPigEngy engy = ModEntities.PIG_ENGY.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
        engy.snapTo(ground.getX() - 5.5, ground.getY(), ground.getZ() + 0.5, 0.0F, 0.0F);
        engy.invaderData().setNexusPos(nexus.getBlockPos());
        engy.setupInvader(1, 0, 0);
        helper.getLevel().addFreshEntity(engy);
        IMZombie zombie = ModEntities.ZOMBIE.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
        zombie.snapTo(ground.getX() - 6.5, ground.getY(), ground.getZ() + 1.5, 0.0F, 0.0F);
        zombie.invaderData().setNexusPos(nexus.getBlockPos());
        zombie.setupInvader(1, 1, 0);
        helper.getLevel().addFreshEntity(zombie);
        helper.succeedWhen(() -> {
            helper.assertTrue(nexus.getHp() < 100, "nobody has reached the nexus on the tower yet");
            helper.assertTrue(zombie.getY() > ground.getY() + height, "the zombie is not on top yet");
            Invasion.LOGGER.info("[test] engineer and zombie got past the overhang after {} ticks", helper.getTick());
            world.clear(engy);
            zombie.discard();
        });
    }

    /** A 3x3 bedrock platform with the nexus on it, far outside the arena so there are no walls to use. */
    private static void floatingNexusOutside(GameTestHelper helper, int height, BlockPos rel) {
        ServerLevelAccess world = new ServerLevelAccess(helper, rel);
        BlockPos ground = world.ground();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.set(ground.offset(dx, height, dz), Blocks.BEDROCK);
            }
        }
        world.set(ground.above(height + 1), ModBlocks.NEXUS.get());
        NexusBlockEntity nexus = (NexusBlockEntity) helper.getLevel().getBlockEntity(ground.above(height + 1));
        IMPigEngy engy = ModEntities.PIG_ENGY.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
        engy.snapTo(ground.getX() - 5.5, ground.getY(), ground.getZ() + 0.5, 0.0F, 0.0F);
        engy.invaderData().setNexusPos(nexus.getBlockPos());
        engy.setupInvader(1, 0, 0);
        helper.getLevel().addFreshEntity(engy);
        helper.succeedWhen(() -> {
            helper.assertTrue(nexus.getHp() < 100, "the engineer has not reached the nexus yet (at "
                    + engy.blockPosition().subtract(ground).toShortString() + ", " + nexus.getAttackerAI().getScaffolds().size()
                    + " scaffolds)");
            int ladders = 0;
            int blocks = 0;
            for (BlockPos p : BlockPos.betweenClosed(ground.offset(-6, 0, -6), ground.offset(6, height + 2, 6))) {
                Block b = helper.getLevel().getBlockState(p).getBlock();
                ladders += b == Blocks.LADDER ? 1 : 0;
                blocks += b == Blocks.OAK_PLANKS ? 1 : 0;
            }
            Invasion.LOGGER.info("[test] engineer reached a platform {} up after {} ticks: {} scaffolds planned, {} ladders and {} planks built",
                    height, helper.getTick(), nexus.getAttackerAI().getScaffolds().size(), ladders, blocks);
            world.clear(engy);
        });
    }

    /** Builds outside the test arena: finds the natural ground and force-loads the chunks around it. */
    private record ServerLevelAccess(GameTestHelper helper, BlockPos rel) {
        BlockPos ground() {
            BlockPos abs = this.helper.absolutePos(this.rel);
            int cx = abs.getX() >> 4;
            int cz = abs.getZ() >> 4;
            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    this.helper.getLevel().setChunkForced(x, z, true);
                }
            }
            while (this.helper.getLevel().getBlockState(abs.below()).isAir() && abs.getY() > this.helper.getLevel().getMinY()) {
                abs = abs.below();
            }
            while (!this.helper.getLevel().getBlockState(abs).isAir()) {
                abs = abs.above();
            }
            return abs;
        }

        void set(BlockPos abs, Block block) {
            this.helper.getLevel().setBlockAndUpdate(abs, block.defaultBlockState());
        }

        void clear(IMPigEngy engy) {
            BlockPos base = engy.getNexusPos();
            if (base != null) {
                for (BlockPos p : BlockPos.betweenClosed(base.offset(-8, -12, -8), base.offset(8, 2, 8))) {
                    BlockState state = this.helper.getLevel().getBlockState(p);
                    if (!state.isAir() && p.getY() > base.getY() - 10) {
                        this.helper.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
            engy.discard();
        }
    }

    private static void raisedNexusWall(GameTestHelper helper) {
        for (int x = 6; x < 15; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(x, y, z, Blocks.BEDROCK);
                }
            }
        }
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
