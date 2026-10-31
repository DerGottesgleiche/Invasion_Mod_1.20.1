package invmod.entity.terrain;

/** Told when a terrain task is over (the original INotifyTask). */
public interface TaskListener {
    int SUCCESS = 0;
    int OUT_OF_RANGE = 1;
    int FAILED = 2;

    void onTaskFinished(int result);
}
