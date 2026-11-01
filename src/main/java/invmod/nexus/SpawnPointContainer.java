package invmod.nexus;

import invmod.util.IPolarAngle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

/** Spawn points around the nexus, sorted by polar angle so waves can come from one direction. */
public class SpawnPointContainer {
    private static final Comparator<IPolarAngle> BY_ANGLE = Comparator.comparingInt(IPolarAngle::getAngle);
    private final EnumMap<SpawnType, List<SpawnPoint>> spawnPoints = new EnumMap<>(SpawnType.class);
    private final Random random = new Random();
    private boolean sorted;

    public SpawnPointContainer() {
        for (SpawnType type : SpawnType.values()) {
            this.spawnPoints.put(type, new ArrayList<>());
        }
    }

    public void addSpawnPointXZ(SpawnPoint spawnPoint) {
        List<SpawnPoint> spawnList = this.spawnPoints.get(spawnPoint.type());
        for (int i = 0; i < spawnList.size(); i++) {
            SpawnPoint old = spawnList.get(i);
            if (old.pos().getX() == spawnPoint.pos().getX() && old.pos().getZ() == spawnPoint.pos().getZ()) {
                if (old.pos().getY() > spawnPoint.pos().getY()) {
                    spawnList.set(i, spawnPoint);
                }
                this.sorted = false;
                return;
            }
        }
        spawnList.add(spawnPoint);
        this.sorted = false;
    }

    public SpawnPoint getRandomSpawnPoint(SpawnType spawnType) {
        List<SpawnPoint> spawnList = this.spawnPoints.get(spawnType);
        return spawnList.isEmpty() ? null : spawnList.get(this.random.nextInt(spawnList.size()));
    }

    public SpawnPoint getRandomSpawnPoint(SpawnType spawnType, int minAngle, int maxAngle) {
        List<SpawnPoint> spawnList = this.spawnPoints.get(spawnType);
        if (spawnList.isEmpty()) {
            return null;
        }
        int[] range = this.range(spawnList, minAngle, maxAngle);
        int start = range[0];
        int end = range[1];
        if (end > start) {
            return spawnList.get(start + this.random.nextInt(end - start));
        }
        if (start > end && end > 0) {
            int r = start + this.random.nextInt(spawnList.size() + end - start);
            if (r >= spawnList.size()) {
                r -= spawnList.size();
            }
            return spawnList.get(r);
        }
        return null;
    }

    public int getNumberOfSpawnPoints(SpawnType type) {
        return this.spawnPoints.get(type).size();
    }

    public int getNumberOfSpawnPoints(SpawnType spawnType, int minAngle, int maxAngle) {
        List<SpawnPoint> spawnList = this.spawnPoints.get(spawnType);
        if (spawnList.isEmpty() || maxAngle - minAngle >= 360) {
            return spawnList.size();
        }
        int[] range = this.range(spawnList, minAngle, maxAngle);
        int start = range[0];
        int end = range[1];
        if (end > start) {
            return end - start;
        }
        if (start > end && end > 0) {
            return end + spawnList.size() - start;
        }
        return 0;
    }

    private int[] range(List<SpawnPoint> spawnList, int minAngle, int maxAngle) {
        if (!this.sorted) {
            spawnList.sort(BY_ANGLE);
            this.sorted = true;
        }
        int start = Collections.binarySearch(spawnList, (IPolarAngle) () -> minAngle, BY_ANGLE);
        if (start < 0) {
            start = -start - 1;
        }
        int end = Collections.binarySearch(spawnList, (IPolarAngle) () -> maxAngle, BY_ANGLE);
        if (end < 0) {
            end = -end - 1;
        }
        return new int[] {start, end};
    }
}
