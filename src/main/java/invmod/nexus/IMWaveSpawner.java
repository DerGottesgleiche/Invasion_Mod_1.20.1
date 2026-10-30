package invmod.nexus;

import invmod.Invasion;
import invmod.block.NexusBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/** Spawns the mobs of the current wave at points on a circle around the nexus. */
public class IMWaveSpawner implements ISpawnerAccess {
    private static final int MAX_SPAWN_TRIES = 20;
    private static final int NORMAL_SPAWN_HEIGHT = 30;
    private static final int MIN_SPAWN_POINTS_TO_KEEP = 15;
    private static final int MIN_SPAWN_POINTS_TO_KEEP_BELOW_HEIGHT_CUTOFF = 20;
    private static final int HEIGHT_CUTOFF = 35;
    private static final float SPAWN_POINT_CULL_RATE = 0.3F;

    private final NexusBlockEntity nexus;
    private final Random rand = new Random();
    private SpawnPointContainer spawnPointContainer = new SpawnPointContainer();
    private Wave currentWave;
    private boolean active;
    private boolean waveComplete;
    private boolean spawnMode = true;
    private int spawnRadius;
    private int successfulSpawns;
    private long elapsed;

    public IMWaveSpawner(NexusBlockEntity nexus, int radius) {
        this.nexus = nexus;
        this.spawnRadius = radius;
    }

    public long getElapsedTime() {
        return this.elapsed;
    }

    public void setRadius(int radius) {
        if (radius > 8) {
            this.spawnRadius = radius;
        }
    }

    public void beginNextWave(int waveNumber) throws WaveSpawnerException {
        this.beginNextWave(IMWaveBuilder.generateMainInvasionWave(waveNumber));
    }

    public void beginNextWave(Wave wave) throws WaveSpawnerException {
        if (!this.active) {
            this.generateSpawnPoints();
        }
        wave.resetWave();
        this.waveComplete = false;
        this.active = true;
        this.currentWave = wave;
        this.elapsed = 0L;
        this.successfulSpawns = 0;
    }

    public void spawn(int elapsedMillis) throws WaveSpawnerException {
        this.elapsed += elapsedMillis;
        if (this.waveComplete || !this.active) {
            return;
        }
        if (this.spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID) < 10) {
            this.generateSpawnPoints();
            if (this.spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID) < 10) {
                throw new WaveSpawnerException("Not enough spawn points around the nexus. Is it buried, or is the range too small?");
            }
        }
        this.currentWave.doNextSpawns(elapsedMillis, this);
        if (this.currentWave.isComplete()) {
            this.waveComplete = true;
        }
    }

    /** Fast-forwards a wave without spawning anything; returns how many mobs it would have spawned. */
    public int resumeFromState(Wave wave, long elapsedTime, int radius) throws WaveSpawnerException {
        this.spawnRadius = radius;
        this.stop();
        this.beginNextWave(wave);
        this.spawnMode = false;
        int numberOfSpawns = 0;
        for (; this.elapsed < elapsedTime; this.elapsed += 100L) {
            numberOfSpawns += this.currentWave.doNextSpawns(100, this);
        }
        this.spawnMode = true;
        return numberOfSpawns;
    }

    public void resumeFromState(int waveNumber, long elapsedTime, int radius) throws WaveSpawnerException {
        this.resumeFromState(IMWaveBuilder.generateMainInvasionWave(waveNumber), elapsedTime, radius);
    }

    public void stop() {
        this.active = false;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isReady() {
        return !this.active && this.nexus.getLevel() != null;
    }

    public boolean isWaveComplete() {
        return this.waveComplete;
    }

    public int getWaveRestTime() {
        return this.currentWave == null ? 0 : this.currentWave.getWaveBreakTime();
    }

    public int getSuccessfulSpawnsThisWave() {
        return this.successfulSpawns;
    }

    @Override
    public void sendSpawnAlert(String message) {
        this.nexus.messageBoundPlayers(message);
    }

    @Override
    public void noSpawnPointNotice() {
    }

    @Override
    public int getNumberOfPointsInRange(int minAngle, int maxAngle, SpawnType type) {
        return this.spawnPointContainer.getNumberOfSpawnPoints(type, minAngle, maxAngle);
    }

    @Override
    public boolean attemptSpawn(EntityConstruct construct, int minAngle, int maxAngle) {
        if (!this.spawnMode) {
            this.successfulSpawns++;
            return true;
        }
        if (!(this.nexus.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        Mob mob = MobBuilder.createMob(construct, level, this.nexus.getBlockPos());
        if (mob == null) {
            Invasion.LOGGER.warn("Invalid entity construct {}", construct);
            return false;
        }
        int spawnTries = Math.min(MAX_SPAWN_TRIES, this.getNumberOfPointsInRange(minAngle, maxAngle, SpawnType.HUMANOID));
        for (int j = 0; j < spawnTries; j++) {
            SpawnPoint spawnPoint = maxAngle - minAngle >= 360
                    ? this.spawnPointContainer.getRandomSpawnPoint(SpawnType.HUMANOID)
                    : this.spawnPointContainer.getRandomSpawnPoint(SpawnType.HUMANOID, minAngle, maxAngle);
            if (spawnPoint == null) {
                return false;
            }
            BlockPos pos = spawnPoint.pos();
            mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.rand.nextFloat() * 360.0F, 0.0F);
            if (level.isLoaded(pos) && level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox())) {
                this.successfulSpawns++;
                level.addFreshEntity(mob);
                return true;
            }
        }
        return false;
    }

    private void generateSpawnPoints() {
        Level level = this.nexus.getLevel();
        if (level == null) {
            return;
        }
        this.spawnPointContainer = new SpawnPointContainer();
        List<SpawnPoint> spawnPoints = new ArrayList<>();
        BlockPos center = this.nexus.getBlockPos();
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        int minY = level.getMinY() + 1;
        int maxY = level.getMaxY() - 2;
        for (int vertical = 0; vertical < 128; vertical = vertical > 0 ? -vertical : -vertical + 1) {
            int py = y + vertical;
            if (py < minY || py > maxY) {
                continue;
            }
            for (int i = 0; i <= this.spawnRadius * 0.7D + 1.0D; i++) {
                int j = (int) Math.round(this.spawnRadius * Math.cos(Math.asin((double) i / this.spawnRadius)));
                this.addValidSpawn(level, spawnPoints, x + i, py, z + j);
                this.addValidSpawn(level, spawnPoints, x + j, py, z + i);
                this.addValidSpawn(level, spawnPoints, x + i, py, z - j);
                this.addValidSpawn(level, spawnPoints, x + j, py, z - i);
                this.addValidSpawn(level, spawnPoints, x - i, py, z + j);
                this.addValidSpawn(level, spawnPoints, x - j, py, z + i);
                this.addValidSpawn(level, spawnPoints, x - i, py, z - j);
                this.addValidSpawn(level, spawnPoints, x - j, py, z - i);
            }
        }

        // Prefer points near the nexus' height, as the original did: cull a share of the far-off ones.
        if (spawnPoints.size() > MIN_SPAWN_POINTS_TO_KEEP) {
            int amountToRemove = (int) ((spawnPoints.size() - MIN_SPAWN_POINTS_TO_KEEP) * SPAWN_POINT_CULL_RATE);
            int i = spawnPoints.size() - 1;
            for (; i >= spawnPoints.size() - amountToRemove; i--) {
                if (Math.abs(spawnPoints.get(i).pos().getY() - y) < NORMAL_SPAWN_HEIGHT) {
                    break;
                }
            }
            for (; i >= MIN_SPAWN_POINTS_TO_KEEP_BELOW_HEIGHT_CUTOFF; i--) {
                SpawnPoint spawnPoint = spawnPoints.get(i);
                if (spawnPoint.pos().getY() - y <= HEIGHT_CUTOFF) {
                    this.spawnPointContainer.addSpawnPointXZ(spawnPoint);
                }
            }
            for (; i >= 0; i--) {
                this.spawnPointContainer.addSpawnPointXZ(spawnPoints.get(i));
            }
        } else {
            spawnPoints.forEach(this.spawnPointContainer::addSpawnPointXZ);
        }
        Invasion.LOGGER.debug("Nexus at {}: {} spawn points", center,
                this.spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID));
    }

    private void addValidSpawn(Level level, List<SpawnPoint> spawnPoints, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isLoaded(pos) || !level.isLoaded(pos.above())) {
            return;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return;
        }
        for (BlockPos p : new BlockPos[] {pos, pos.above()}) {
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !level.getFluidState(p).isEmpty()) {
                return;
            }
        }
        int angle = (int) (Math.atan2(this.nexus.getBlockPos().getZ() - z, this.nexus.getBlockPos().getX() - x) * 180.0D / Math.PI);
        spawnPoints.add(new SpawnPoint(pos, angle, SpawnType.HUMANOID));
    }
}
