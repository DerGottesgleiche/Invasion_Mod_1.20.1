package invmod.nexus;

import invmod.Invasion;
import invmod.util.ISelect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** One stream of spawns inside a wave: an amount of mobs spread over a time window and an angle sector. */
public class WaveEntry {
    private static final Random RANDOM = new Random();
    private final int timeBegin;
    private final int timeEnd;
    private final int amount;
    private final int granularity;
    private final ISelect<IEntityIMPattern> mobPool;
    private final List<EntityConstruct> spawnList = new ArrayList<>();
    private final Map<Integer, String> alerts = new HashMap<>();
    private int amountQueued;
    private int elapsed;
    private int toNextSpawn;
    private int minAngle;
    private int maxAngle;
    private int minPointsInRange;
    private int nextAlert = Integer.MAX_VALUE;

    public WaveEntry(int timeBegin, int timeEnd, int amount, int granularity, ISelect<IEntityIMPattern> mobPool) {
        this(timeBegin, timeEnd, amount, granularity, mobPool, -180, 180, 1);
    }

    public WaveEntry(int timeBegin, int timeEnd, int amount, int granularity, ISelect<IEntityIMPattern> mobPool,
            int angleRange, int minPointsInRange) {
        this(timeBegin, timeEnd, amount, granularity, mobPool, 0, 0, minPointsInRange);
        this.minAngle = RANDOM.nextInt(360) - 180;
        this.maxAngle = this.minAngle + angleRange;
        while (this.maxAngle > 180) {
            this.maxAngle -= 360;
        }
    }

    public WaveEntry(int timeBegin, int timeEnd, int amount, int granularity, ISelect<IEntityIMPattern> mobPool,
            int minAngle, int maxAngle, int minPointsInRange) {
        this.timeBegin = timeBegin;
        this.timeEnd = timeEnd;
        this.amount = amount;
        this.granularity = granularity;
        this.mobPool = mobPool;
        this.minAngle = minAngle;
        this.maxAngle = maxAngle;
        this.minPointsInRange = minPointsInRange;
    }

    public int doNextSpawns(int elapsedMillis, ISpawnerAccess spawner) {
        this.toNextSpawn -= elapsedMillis;
        if (this.nextAlert <= this.elapsed - this.toNextSpawn) {
            this.sendNextAlert(spawner);
        }

        if (this.toNextSpawn <= 0) {
            this.elapsed += this.granularity;
            this.toNextSpawn += this.granularity;
            if (this.toNextSpawn < 0) {
                this.elapsed -= this.toNextSpawn;
                this.toNextSpawn = 0;
            }

            int amountToSpawn = Math.round((float) this.amount * this.elapsed / (this.timeEnd - this.timeBegin))
                    - this.amountQueued;
            if (amountToSpawn > 0) {
                if (amountToSpawn + this.amountQueued > this.amount) {
                    amountToSpawn = this.amount - this.amountQueued;
                }
                int guard = 0;
                while (amountToSpawn > 0 && guard++ < 100) {
                    IEntityIMPattern pattern = this.mobPool.selectNext();
                    if (pattern != null) {
                        EntityConstruct construct = pattern.generateEntityConstruct(this.minAngle, this.maxAngle);
                        amountToSpawn--;
                        this.amountQueued++;
                        this.spawnList.add(construct);
                    } else {
                        Invasion.LOGGER.warn("A selection pool in wave entry {} returned empty", this);
                    }
                }
            }
        }

        if (!this.spawnList.isEmpty()) {
            int numberOfSpawns = 0;
            if (spawner.getNumberOfPointsInRange(this.minAngle, this.maxAngle, SpawnType.HUMANOID) >= this.minPointsInRange) {
                for (int i = this.spawnList.size() - 1; i >= 0; i--) {
                    if (spawner.attemptSpawn(this.spawnList.get(i), this.minAngle, this.maxAngle)) {
                        numberOfSpawns++;
                        this.spawnList.remove(i);
                    }
                }
            } else {
                this.reviseSpawnAngles(spawner);
            }
            return numberOfSpawns;
        }
        return 0;
    }

    public void resetToBeginning() {
        this.elapsed = 0;
        this.amountQueued = 0;
        this.mobPool.reset();
    }

    public int getTimeBegin() {
        return this.timeBegin;
    }

    public int getTimeEnd() {
        return this.timeEnd;
    }

    public int getAmount() {
        return this.amount;
    }

    public void addAlert(String message, int timeElapsed) {
        this.alerts.put(timeElapsed, message);
        if (timeElapsed < this.nextAlert) {
            this.nextAlert = timeElapsed;
        }
    }

    @Override
    public String toString() {
        return "WaveEntry#time=" + this.timeBegin + "-" + this.timeEnd + "#amount=" + this.amount;
    }

    private void sendNextAlert(ISpawnerAccess spawner) {
        String message = this.alerts.remove(this.nextAlert);
        if (message != null) {
            spawner.sendSpawnAlert(message);
        }
        this.nextAlert = Integer.MAX_VALUE;
        for (Integer key : this.alerts.keySet()) {
            if (key < this.nextAlert) {
                this.nextAlert = key;
            }
        }
    }

    private void reviseSpawnAngles(ISpawnerAccess spawner) {
        int angleRange = this.maxAngle - this.minAngle;
        while (angleRange < 0) {
            angleRange += 360;
        }
        if (angleRange == 0) {
            angleRange = 360;
        }
        List<Integer> validAngles = new ArrayList<>();
        for (int angle = -180; angle < 180; angle += angleRange) {
            int nextAngle = angle + angleRange;
            if (nextAngle >= 180) {
                nextAngle -= 360;
            }
            if (spawner.getNumberOfPointsInRange(angle, nextAngle, SpawnType.HUMANOID) >= this.minPointsInRange) {
                validAngles.add(angle);
            }
        }
        if (!validAngles.isEmpty()) {
            this.minAngle = validAngles.get(RANDOM.nextInt(validAngles.size()));
            this.maxAngle = this.minAngle + angleRange;
            while (this.maxAngle >= 180) {
                this.maxAngle -= 360;
            }
        }
        if (this.minPointsInRange > 1) {
            this.minPointsInRange = 1;
        } else if (this.maxAngle - this.minAngle < 360) {
            this.minAngle = -180;
            this.maxAngle = 180;
        } else {
            Invasion.LOGGER.warn("Wave entry cannot find a single spawn point");
            spawner.noSpawnPointNotice();
        }
    }
}
