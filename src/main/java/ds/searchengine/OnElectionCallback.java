package ds.searchengine;

public interface OnElectionCallback {
    void onElectedToBeLeader();
    void onWorker();
}