package invmod.nexus;

public interface ISpawnerAccess {
    boolean attemptSpawn(EntityConstruct construct, int minAngle, int maxAngle);

    int getNumberOfPointsInRange(int minAngle, int maxAngle, SpawnType type);

    void sendSpawnAlert(String message);

    void noSpawnPointNotice();
}
