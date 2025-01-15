package ds.searchengine;

import org.apache.zookeeper.*;
import org.springframework.stereotype.Component;
import java.util.logging.Logger;

import java.util.Collections;
import java.util.List;

@Component
class LeaderElectionService implements Watcher {

    private static final Logger LOGGER = Logger.getLogger(LeaderElectionService.class.getName());
    private static final String ELECTION_NAMESPACE = "/election";
    private final ZooKeeper zooKeeper;
    private final OnElectionCallback onElectionCallback;
    private String currentZnodeName;
    private boolean isLeader = false;

    public LeaderElectionService(ZooKeeper zooKeeper, OnElectionCallback onElectionCallback) {
        this.zooKeeper = zooKeeper;
        this.onElectionCallback = onElectionCallback;
        try {
            ensureElectionNamespace();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize leader election", e);
        }
    }

    private void ensureElectionNamespace() throws KeeperException, InterruptedException {
        if (zooKeeper.exists(ELECTION_NAMESPACE, false) == null) {
            zooKeeper.create(ELECTION_NAMESPACE, new byte[]{},
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    public void volunteerForLeadership() throws KeeperException, InterruptedException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{},
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void reelectLeader() throws KeeperException, InterruptedException {
        List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
        Collections.sort(children);
        String smallestChild = children.get(0);

        if (smallestChild.equals(currentZnodeName) && !isLeader) {
            isLeader = true;
            LOGGER.info("Current leader candidate: " + smallestChild + ", my znode: " + currentZnodeName);
            onElectionCallback.onElectedToBeLeader();
        } else if (!smallestChild.equals(currentZnodeName)) {
            isLeader = false;
            watchPredecessor(smallestChild);
        }
    }

    private void watchPredecessor(String predecessor) throws KeeperException, InterruptedException {
        String predecessorPath = ELECTION_NAMESPACE + "/" + predecessor;
        zooKeeper.exists(predecessorPath, this);
        onElectionCallback.onWorker();
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted) {
            try {
                reelectLeader();
            } catch (KeeperException | InterruptedException e) {
                LOGGER.severe("Error during leader reelection: " + e.getMessage());
            }
        }
    }
}