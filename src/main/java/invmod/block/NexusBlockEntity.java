package invmod.block;

import invmod.Config;
import invmod.Invasion;
import invmod.entity.Invader;
import invmod.nexus.IMWaveBuilder;
import invmod.nexus.IMWaveSpawner;
import invmod.nexus.Wave;
import invmod.nexus.WaveSpawnerException;
import invmod.registry.ModBlockEntities;
import invmod.registry.ModItems;
import invmod.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The nexus: activated with a catalyst, it opens a rift that spawns waves of invaders who try to reach and
 * destroy it. Ported from the original mod's TileEntityNexus.
 *
 * Modes: 0 idle, 1 invasion, 2 continuous (waiting for the next night attack), 3 invasion started while in
 * continuous mode, 4 activating continuous mode.
 */
public class NexusBlockEntity extends BaseContainerBlockEntity {
    public static final int MODE_IDLE = 0;
    public static final int MODE_INVASION = 1;
    public static final int MODE_CONTINUOUS = 2;
    public static final int MODE_CONTINUOUS_INVASION = 3;
    public static final int MODE_CONTINUOUS_ACTIVATING = 4;

    public static final int TOTAL_ACTIVATION_TIME = 200;
    public static final int MAX_COOK_TIME = 1200;
    public static final int MAX_GENERATION_TIME = 3000;
    private static final int TICK_MILLIS = 50;

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final IMWaveSpawner waveSpawner;
    private final IMWaveBuilder waveBuilder = new IMWaveBuilder();
    private final invmod.entity.ai.AttackerAI attackerAI = new invmod.entity.ai.AttackerAI(this);
    private final List<UUID> boundPlayers = new ArrayList<>();
    private List<Mob> mobList = new ArrayList<>();

    private int fluxGeneration;
    private int activationTimer;
    private int currentWave;
    private int spawnRadius = 52;
    private int nexusLevel = 1;
    private int nexusKills;
    private int cookTime;
    private final int maxHp = 100;
    private int hp = 100;
    private int lastHp = 100;
    private int mode;
    private int powerLevel;
    private int lastPowerLevel;
    private int powerLevelTimer;
    private int mobsLeftInWave;
    private int lastMobsLeftInWave;
    private int mobsToKillInWave;
    private long nextAttackTime;
    private long lastWorldTime;
    private int zapTimer;
    private int tickCount;
    private int immuneTicks;
    private long spawnerElapsedRestore;
    private long waveDelayTimer = -1L;
    private long waveDelay;
    private boolean continuousAttack;
    private boolean resumedFromNBT;
    private boolean activated;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            NexusBlockEntity n = NexusBlockEntity.this;
            return switch (index) {
                case 0 -> n.nexusLevel;
                case 1 -> n.nexusKills;
                case 2 -> n.spawnRadius;
                case 3 -> n.mode;
                case 4 -> n.currentWave;
                case 5 -> n.powerLevel;
                case 6 -> n.activationTimer;
                case 7 -> n.fluxGeneration;
                case 8 -> n.cookTime;
                case 9 -> n.hp;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            NexusBlockEntity n = NexusBlockEntity.this;
            switch (index) {
                case 0 -> n.nexusLevel = value;
                case 1 -> n.nexusKills = value;
                case 2 -> n.spawnRadius = value;
                case 3 -> n.mode = value;
                case 4 -> n.currentWave = value;
                case 5 -> n.powerLevel = value;
                case 6 -> n.activationTimer = value;
                case 7 -> n.fluxGeneration = value;
                case 8 -> n.cookTime = value;
                case 9 -> n.hp = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public NexusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEXUS.get(), pos, state);
        this.waveSpawner = new IMWaveSpawner(this, this.spawnRadius);
    }

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, NexusBlockEntity nexus) {
        nexus.tick();
    }

    private void tick() {
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        this.updateStatus();

        if (this.mode == MODE_INVASION || this.mode == MODE_CONTINUOUS || this.mode == MODE_CONTINUOUS_INVASION) {
            if (this.resumedFromNBT) {
                this.resumedFromNBT = false;
                if (this.mode == MODE_CONTINUOUS && this.continuousAttack) {
                    if (this.resumeSpawnerContinuous()) {
                        this.mobsLeftInWave = this.lastMobsLeftInWave += this.countBoundMobs();
                    }
                } else if (this.mode != MODE_CONTINUOUS) {
                    this.resumeSpawnerInvasion();
                }
            }
            try {
                if (++this.tickCount >= 60) {
                    this.tickCount = 0;
                    this.bindPlayers();
                    this.updateMobList();
                }
                this.attackerAI.tick(this.mobList);
                if (this.mode == MODE_INVASION || this.mode == MODE_CONTINUOUS_INVASION) {
                    this.doInvasion(TICK_MILLIS);
                } else {
                    this.doContinuous(TICK_MILLIS);
                }
            } catch (WaveSpawnerException e) {
                Invasion.LOGGER.error("Invasion stopped: {}", e.getMessage());
                this.messageBoundPlayers(e.getMessage());
                this.stop();
            }
        }
    }

    private void updateStatus() {
        if (this.immuneTicks > 0) {
            this.immuneTicks--;
        }
        ItemStack input = this.items.get(0);

        // Cooking: the nexus turns empty traps into rift traps (faster while an invasion runs).
        if (input.is(ModItems.TRAP_EMPTY.get())) {
            if (this.cookTime < MAX_COOK_TIME) {
                this.cookTime += this.mode == MODE_IDLE ? 1 : 9;
            }
            if (this.cookTime >= MAX_COOK_TIME && this.tryOutput(ModItems.TRAP_RIFT.get())) {
                input.shrink(1);
                this.cookTime = 0;
                this.setChanged();
            }
        } else {
            this.cookTime = 0;
        }

        if (this.activationTimer >= TOTAL_ACTIVATION_TIME) {
            this.activationTimer = 0;
            if (input.is(ModItems.NEXUS_CATALYST.get())) {
                input.shrink(1);
                this.setActivated(true);
                this.startInvasion(1);
            } else if (input.is(ModItems.STRONG_CATALYST.get())) {
                input.shrink(1);
                this.setActivated(true);
                this.startInvasion(10);
            } else if (input.is(ModItems.STABLE_NEXUS_CATALYST.get())) {
                input.shrink(1);
                this.setActivated(true);
                this.startContinuousPlay();
            }
            this.setChanged();
        } else if (this.mode == MODE_IDLE || this.mode == MODE_CONTINUOUS_ACTIVATING) {
            if (input.is(ModItems.NEXUS_CATALYST.get()) || input.is(ModItems.STRONG_CATALYST.get())) {
                this.activationTimer++;
                this.mode = MODE_IDLE;
            } else if (input.is(ModItems.STABLE_NEXUS_CATALYST.get())) {
                this.activationTimer++;
                this.mode = MODE_CONTINUOUS_ACTIVATING;
            } else {
                this.activationTimer = 0;
            }
        } else if (this.mode == MODE_CONTINUOUS) {
            if (input.is(ModItems.NEXUS_CATALYST.get()) || input.is(ModItems.STRONG_CATALYST.get())) {
                this.activationTimer++;
            } else {
                this.activationTimer = 0;
            }
        }
    }

    private boolean tryOutput(Item item) {
        ItemStack output = this.items.get(1);
        if (output.isEmpty()) {
            this.items.set(1, new ItemStack(item));
            return true;
        }
        if (output.is(item) && output.getCount() < output.getMaxStackSize()) {
            output.grow(1);
            return true;
        }
        return false;
    }

    private void generateFlux(int increment) {
        this.fluxGeneration += increment;
        if (this.fluxGeneration >= MAX_GENERATION_TIME && this.tryOutput(ModItems.RIFT_FLUX.get())) {
            this.fluxGeneration -= MAX_GENERATION_TIME;
            this.setChanged();
        }
        this.fluxGeneration = Math.min(this.fluxGeneration, MAX_GENERATION_TIME);
    }

    private void doInvasion(int elapsed) throws WaveSpawnerException {
        if (!this.waveSpawner.isActive()) {
            return;
        }
        if (this.hp <= 0) {
            this.theEnd();
            return;
        }
        this.generateFlux(1);
        if (this.waveSpawner.isWaveComplete()) {
            if (this.waveDelayTimer == -1L) {
                this.messageBoundPlayers("Wave " + this.currentWave + " almost complete!");
                this.playSoundForBoundPlayers(ModSounds.CHIME.get());
                this.waveDelayTimer = 0L;
                this.waveDelay = this.waveSpawner.getWaveRestTime();
            } else {
                this.waveDelayTimer += elapsed;
                if (this.waveDelayTimer > this.waveDelay) {
                    this.currentWave++;
                    this.messageBoundPlayers("Wave " + this.currentWave + " about to begin");
                    this.waveSpawner.beginNextWave(this.currentWave);
                    this.waveDelayTimer = -1L;
                    this.playSoundForBoundPlayers(ModSounds.RUMBLE.get());
                    if (this.currentWave > this.nexusLevel) {
                        this.nexusLevel = this.currentWave;
                    }
                    this.setChanged();
                }
            }
        } else {
            this.waveSpawner.spawn(elapsed);
        }
    }

    private void doContinuous(int elapsed) {
        ItemStack input = this.items.get(0);
        this.powerLevelTimer += elapsed;
        if (this.powerLevelTimer > 2200) {
            this.powerLevelTimer -= 2200;
            this.generateFlux(5 + (int) (5 * this.powerLevel / 1550.0F));
            if (!input.is(ModItems.DAMPING_AGENT.get())) {
                this.powerLevel++;
            }
        }
        if (input.is(ModItems.STRONG_DAMPING_AGENT.get()) && this.powerLevel >= 0 && !this.continuousAttack) {
            this.powerLevel--;
            if (this.powerLevel < 0) {
                this.stop();
                return;
            }
        }

        if (!this.continuousAttack) {
            long currentTime = this.level.getOverworldClockTime();
            long timeOfDay = this.lastWorldTime % 24000L;
            if (timeOfDay < 12000L && currentTime % 24000L >= 12000L && currentTime + 12000L > this.nextAttackTime) {
                this.messageBoundPlayers("The night looms around the nexus...");
            }
            if (this.lastWorldTime > currentTime) {
                this.nextAttackTime -= this.lastWorldTime - currentTime;
            }
            this.lastWorldTime = currentTime;
            if (this.lastWorldTime >= this.nextAttackTime) {
                float difficulty = 1.0F + this.powerLevel / 4500.0F;
                try {
                    Wave wave = this.waveBuilder.generateWave(difficulty, difficulty, 240);
                    this.mobsLeftInWave = this.lastMobsLeftInWave = this.mobsToKillInWave = (int) (wave.getTotalMobAmount() * 0.8F);
                    this.waveSpawner.beginNextWave(wave);
                    this.continuousAttack = true;
                    this.scheduleNextAttack(currentTime);
                    this.hp = this.lastHp = this.maxHp;
                    this.zapTimer = 0;
                    this.waveDelayTimer = -1L;
                    this.messageBoundPlayers("Enemy forces are destabilising the nexus!");
                    this.playSoundForBoundPlayers(ModSounds.RUMBLE.get());
                } catch (WaveSpawnerException e) {
                    Invasion.LOGGER.error("Continuous mode stopped: {}", e.getMessage());
                    this.messageBoundPlayers(e.getMessage());
                    this.stop();
                }
            }
        } else if (this.hp <= 0) {
            this.continuousAttack = false;
            this.continuousNexusHurt();
        } else if (this.waveSpawner.isWaveComplete()) {
            if (this.waveDelayTimer == -1L) {
                this.waveDelayTimer = 0L;
                this.waveDelay = this.waveSpawner.getWaveRestTime();
            } else {
                this.waveDelayTimer += elapsed;
                if (this.waveDelayTimer > this.waveDelay && this.zapTimer < -200) {
                    this.waveDelayTimer = -1L;
                    this.continuousAttack = false;
                    this.waveSpawner.stop();
                    this.hp = this.lastHp = this.maxHp;
                    this.lastPowerLevel = this.powerLevel;
                }
            }
            this.zapTimer--;
            if (this.mobsLeftInWave <= 0 && this.zapTimer <= 0 && this.zapEnemy()) {
                this.zapTimer = 23;
            }
        } else {
            try {
                this.waveSpawner.spawn(elapsed);
            } catch (WaveSpawnerException e) {
                Invasion.LOGGER.error("Continuous mode stopped: {}", e.getMessage());
                this.stop();
            }
        }
    }

    private void scheduleNextAttack(long currentTime) {
        int min = Config.MIN_DAYS_BETWEEN_ATTACKS.get();
        int days = min + this.level.getRandom().nextInt(1 + Config.maxDays() - min);
        this.nextAttackTime = currentTime / 24000L * 24000L + 14000L + days * 24000L;
    }

    // ---------------------------------------------------------------- invasion control

    private void startInvasion(int startWave) {
        if (this.mode == MODE_CONTINUOUS && this.continuousAttack) {
            this.messageBoundPlayers("Can't activate nexus when already under attack!");
            return;
        }
        if (this.mode != MODE_IDLE && this.mode != MODE_CONTINUOUS) {
            return;
        }
        if (!this.waveSpawner.isReady()) {
            Invasion.LOGGER.warn("Wave spawner not in ready state");
            return;
        }
        try {
            this.currentWave = startWave;
            this.waveSpawner.beginNextWave(this.currentWave);
            this.mode = this.mode == MODE_IDLE ? MODE_INVASION : MODE_CONTINUOUS_INVASION;
            this.bindPlayers();
            this.hp = this.lastHp = this.maxHp;
            this.waveDelayTimer = -1L;
            this.messageBoundPlayers("The first wave is coming soon!");
            this.playSoundForBoundPlayers(ModSounds.RUMBLE.get());
            this.setChanged();
        } catch (WaveSpawnerException e) {
            this.messageBoundPlayers(e.getMessage());
            this.stop();
        }
    }

    private void startContinuousPlay() {
        if (this.mode == MODE_CONTINUOUS_ACTIVATING && this.waveSpawner.isReady() && this.activated) {
            this.mode = MODE_CONTINUOUS;
            this.hp = this.lastHp = this.maxHp;
            this.lastPowerLevel = this.powerLevel;
            this.lastWorldTime = this.level.getOverworldClockTime();
            this.nextAttackTime = this.lastWorldTime / 24000L * 24000L + 14000L;
            this.bindPlayers();
            long timeOfDay = this.lastWorldTime % 24000L;
            this.messageBoundPlayers(timeOfDay > 12000L && timeOfDay < 16000L
                    ? "The night looms around the nexus..." : "Nexus activated and stable");
        } else {
            this.messageBoundPlayers("Couldn't activate nexus");
            this.setActivated(false);
        }
    }

    public void stop() {
        if (this.mode == MODE_CONTINUOUS_INVASION) {
            this.mode = MODE_CONTINUOUS;
            this.scheduleNextAttack(this.level.getOverworldClockTime());
        } else {
            this.mode = MODE_IDLE;
            this.setActivated(false);
        }
        this.waveSpawner.stop();
        this.attackerAI.clear();
        this.activationTimer = 0;
        this.currentWave = 0;
        this.messageBoundPlayers("Invasion ended.");
        if (this.mode == MODE_IDLE) {
            this.boundPlayers.clear();
        }
        this.setChanged();
    }

    public void emergencyStop() {
        Invasion.LOGGER.info("Nexus at {} manually stopped", this.worldPosition);
        this.stop();
        this.killAllMobs();
    }

    public void debugStartInvasion(int startWave) {
        this.mode = MODE_IDLE;
        this.waveSpawner.stop();
        this.setActivated(true);
        this.startInvasion(startWave);
    }

    public void debugStartContinuous() {
        this.mode = MODE_CONTINUOUS_ACTIVATING;
        this.waveSpawner.stop();
        this.setActivated(true);
        this.startContinuousPlay();
    }

    private void theEnd() {
        this.messageBoundPlayers("The nexus is destroyed!");
        if (this.level instanceof ServerLevel serverLevel) {
            this.playSoundForBoundPlayers(SoundEvents.GENERIC_EXPLODE.value());
            if (Config.KILL_BOUND_PLAYERS.get()) {
                for (ServerPlayer player : this.getBoundPlayerEntities()) {
                    player.hurtServer(serverLevel, serverLevel.damageSources().magic(), Float.MAX_VALUE);
                }
            }
        }
        this.stop();
        this.killAllMobs();
    }

    private void continuousNexusHurt() {
        this.messageBoundPlayers("Nexus severely damaged!");
        this.playSoundForBoundPlayers(SoundEvents.GENERIC_EXPLODE.value());
        this.killAllMobs();
        this.waveSpawner.stop();
        this.powerLevel = (int) ((this.powerLevel - (this.powerLevel - this.lastPowerLevel)) * 0.7F);
        this.lastPowerLevel = this.powerLevel;
        if (this.powerLevel < 0) {
            this.powerLevel = 0;
            this.stop();
        }
    }

    public void attackNexus(int damage) {
        if (this.immuneTicks > 0 || this.level == null) {
            return;
        }
        this.immuneTicks = 10 + this.level.getRandom().nextInt(30);
        this.hp = Math.max(0, this.hp - damage);
        if (this.hp <= 0 && this.mode == MODE_INVASION) {
            this.theEnd();
            return;
        }
        while (this.hp + 5 <= this.lastHp) {
            this.lastHp -= 5;
            this.messageBoundPlayers(Component.literal("Nexus at " + this.lastHp + " hp").withStyle(ChatFormatting.RED));
        }
        this.setChanged();
    }

    public void registerMobDied() {
        this.nexusKills++;
        this.mobsLeftInWave--;
        if (this.mobsLeftInWave <= 0) {
            if (this.lastMobsLeftInWave > 0) {
                this.messageBoundPlayers("Nexus rift stable again!");
                this.messageBoundPlayers("Unleashing tapped energy...");
                this.lastMobsLeftInWave = this.mobsLeftInWave;
            }
        } else if (this.mobsToKillInWave > 0) {
            while (this.mobsLeftInWave + this.mobsToKillInWave * 0.1F <= this.lastMobsLeftInWave) {
                this.messageBoundPlayers("Nexus rift stabilised to " + (100 - 100 * this.mobsLeftInWave / this.mobsToKillInWave) + "%");
                this.lastMobsLeftInWave = (int) (this.lastMobsLeftInWave - this.mobsToKillInWave * 0.1F);
            }
        }
        this.setChanged();
    }

    private boolean zapEnemy() {
        if (this.mobList.isEmpty() || !(this.level instanceof ServerLevel serverLevel)) {
            return false;
        }
        this.mobList.sort(Comparator.comparingDouble(m -> m.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.worldPosition))));
        Mob mob = this.mobList.remove(this.mobList.size() - 1);
        if (!mob.isAlive()) {
            return true;
        }
        LightningBolt bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.EVENT);
        if (bolt != null) {
            bolt.snapTo(mob.getX(), mob.getY(), mob.getZ());
            bolt.setVisualOnly(true);
            serverLevel.addFreshEntity(bolt);
        }
        serverLevel.playSound(null, this.worldPosition, ModSounds.ZAP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        mob.hurtServer(serverLevel, serverLevel.damageSources().magic(), 500.0F);
        return true;
    }

    private void killAllMobs() {
        if (this.level instanceof ServerLevel serverLevel) {
            for (Mob mob : this.findBoundMobs()) {
                mob.hurtServer(serverLevel, serverLevel.damageSources().magic(), Float.MAX_VALUE);
            }
        }
    }

    private AABB area() {
        int r = this.spawnRadius + 10;
        return new AABB(this.worldPosition).inflate(r, r + 40, r);
    }

    private List<Mob> findBoundMobs() {
        return this.level.getEntitiesOfClass(Mob.class, this.area().inflate(10, 64, 10),
                m -> m instanceof Invader inv && this.worldPosition.equals(inv.getNexusPos()));
    }

    private void updateMobList() {
        this.mobList = new ArrayList<>(this.findBoundMobs());
    }

    private int countBoundMobs() {
        return this.findBoundMobs().size();
    }

    private void resumeSpawnerInvasion() {
        try {
            this.waveSpawner.resumeFromState(this.currentWave, this.spawnerElapsedRestore, this.spawnRadius);
        } catch (WaveSpawnerException e) {
            Invasion.LOGGER.error("Error resuming spawner: {}", e.getMessage());
            this.waveSpawner.stop();
        }
    }

    private boolean resumeSpawnerContinuous() {
        try {
            float difficulty = 1.0F + this.powerLevel / 4500.0F;
            Wave wave = this.waveBuilder.generateWave(difficulty, difficulty, 240);
            this.mobsToKillInWave = (int) (wave.getTotalMobAmount() * 0.8F);
            this.mobsLeftInWave = this.lastMobsLeftInWave = this.mobsToKillInWave
                    - this.waveSpawner.resumeFromState(wave, this.spawnerElapsedRestore, this.spawnRadius);
            return true;
        } catch (WaveSpawnerException e) {
            Invasion.LOGGER.error("Error resuming spawner: {}", e.getMessage());
            this.waveSpawner.stop();
            return false;
        }
    }

    // ---------------------------------------------------------------- players

    private void bindPlayers() {
        for (Player player : this.level.getEntitiesOfClass(Player.class, this.area())) {
            if (player.isSpectator() || this.boundPlayers.contains(player.getUUID())) {
                continue;
            }
            String name = player.getName().getString();
            this.messageBoundPlayers(name + (name.toLowerCase().endsWith("s") ? "'" : "'s") + " life is now bound to the nexus");
            player.sendSystemMessage(Component.literal("Your life is now bound to the nexus!").withStyle(ChatFormatting.DARK_PURPLE));
            this.boundPlayers.add(player.getUUID());
        }
    }

    public List<ServerPlayer> getBoundPlayerEntities() {
        List<ServerPlayer> players = new ArrayList<>();
        if (this.level instanceof ServerLevel serverLevel) {
            for (UUID uuid : this.boundPlayers) {
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
                if (player != null) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    public void messageBoundPlayers(String message) {
        this.messageBoundPlayers(Component.literal(message));
    }

    public void messageBoundPlayers(Component message) {
        for (ServerPlayer player : this.getBoundPlayerEntities()) {
            player.sendSystemMessage(message);
        }
    }

    private void playSoundForBoundPlayers(SoundEvent sound) {
        for (ServerPlayer player : this.getBoundPlayerEntities()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    // ---------------------------------------------------------------- accessors

    private void setActivated(boolean active) {
        this.activated = active;
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.hasProperty(NexusBlock.ACTIVE) && state.getValue(NexusBlock.ACTIVE) != active) {
                this.level.setBlockAndUpdate(this.worldPosition, state.setValue(NexusBlock.ACTIVE, active));
            }
        }
    }

    public boolean isActive() {
        return this.activated;
    }

    public invmod.entity.ai.AttackerAI getAttackerAI() {
        return this.attackerAI;
    }

    public int getMode() {
        return this.mode;
    }

    public int getCurrentWave() {
        return this.currentWave;
    }

    public int getSpawnRadius() {
        return this.spawnRadius;
    }

    public int getHp() {
        return this.hp;
    }

    public int getNexusLevel() {
        return this.nexusLevel;
    }

    public int getNexusKills() {
        return this.nexusKills;
    }

    public int getPowerLevel() {
        return this.powerLevel;
    }

    public boolean setSpawnRadius(int radius) {
        if (!this.waveSpawner.isActive() && radius > 8) {
            this.spawnRadius = radius;
            this.waveSpawner.setRadius(radius);
            this.setChanged();
            return true;
        }
        return false;
    }

    public String debugStatus() {
        return "Mode: " + this.mode + ", wave: " + this.currentWave + ", hp: " + this.hp + ", mobs alive: "
                + this.countBoundMobs() + ", next attack: " + this.nextAttackTime + ", power: " + this.powerLevel;
    }

    // ---------------------------------------------------------------- container

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mod_invasion.nexus");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new NexusMenu(containerId, inventory, this, this.dataAccess);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.boundPlayers.clear();
        input.getString("BoundPlayers").ifPresent(s -> {
            for (String part : s.split(",")) {
                if (!part.isEmpty()) {
                    try {
                        this.boundPlayers.add(UUID.fromString(part));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        });
        this.fluxGeneration = input.getIntOr("Flux", 0);
        this.activationTimer = input.getIntOr("ActivationTimer", 0);
        this.mode = input.getIntOr("Mode", 0);
        this.currentWave = input.getIntOr("CurrentWave", 0);
        this.spawnRadius = input.getIntOr("SpawnRadius", 52);
        this.nexusLevel = input.getIntOr("NexusLevel", 1);
        this.hp = input.getIntOr("Hp", this.maxHp);
        this.lastHp = this.hp;
        this.nexusKills = input.getIntOr("NexusKills", 0);
        this.cookTime = input.getIntOr("CookTime", 0);
        this.powerLevel = input.getIntOr("PowerLevel", 0);
        this.lastPowerLevel = input.getIntOr("LastPowerLevel", 0);
        this.nextAttackTime = input.getLongOr("NextAttackTime", 0L);
        this.lastWorldTime = input.getLongOr("LastWorldTime", 0L);
        this.continuousAttack = input.getBooleanOr("ContinuousAttack", false);
        this.activated = input.getBooleanOr("Activated", false);
        this.waveSpawner.setRadius(this.spawnRadius);
        this.attackerAI.load(input);
        if (this.mode == MODE_INVASION || this.mode == MODE_CONTINUOUS_INVASION
                || (this.mode == MODE_CONTINUOUS && this.continuousAttack)) {
            this.resumedFromNBT = true;
            this.spawnerElapsedRestore = input.getLongOr("SpawnerElapsed", 0L);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        StringBuilder players = new StringBuilder();
        for (UUID uuid : this.boundPlayers) {
            players.append(uuid).append(',');
        }
        output.putString("BoundPlayers", players.toString());
        output.putInt("Flux", this.fluxGeneration);
        output.putInt("ActivationTimer", this.activationTimer);
        output.putInt("Mode", this.mode);
        output.putInt("CurrentWave", this.currentWave);
        output.putInt("SpawnRadius", this.spawnRadius);
        output.putInt("NexusLevel", this.nexusLevel);
        output.putInt("Hp", this.hp);
        output.putInt("NexusKills", this.nexusKills);
        output.putInt("CookTime", this.cookTime);
        output.putInt("PowerLevel", this.powerLevel);
        output.putInt("LastPowerLevel", this.lastPowerLevel);
        output.putLong("NextAttackTime", this.nextAttackTime);
        output.putLong("LastWorldTime", this.lastWorldTime);
        output.putBoolean("ContinuousAttack", this.continuousAttack);
        output.putBoolean("Activated", this.activated);
        output.putLong("SpawnerElapsed", this.waveSpawner.getElapsedTime());
        this.attackerAI.save(output);
    }

    public static @Nullable NexusBlockEntity findNearest(Level level, BlockPos origin, int range) {
        NexusBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -range, -range), origin.offset(range, range, range))) {
            if (level.getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
                double d = pos.distSqr(origin);
                if (d < bestDist) {
                    bestDist = d;
                    best = nexus;
                }
            }
        }
        return best;
    }
}
