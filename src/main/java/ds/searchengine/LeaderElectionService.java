package ds.searchengine;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import proto.generated.CoordinatorServiceGrpc;
import proto.generated.RegisterRequest;
import proto.generated.RegisterResponse;

import java.util.Collections;
import java.util.List;

@Service
class LeaderElectionService implements Watcher {
    private static final String ELECTION_NAMESPACE = "/election";
    private static final String LEADER_INFO_PATH = "/leader_info";
    private final ZooKeeper zooKeeper;
    private final OnElectionCallback onElectionCallback;
    private final SearchEngineLogger logger;
    private String currentZnodeName;
    private boolean isLeader = false;

    @Value("${spring.grpc.server.port}")
    private int serverPort;

    public LeaderElectionService(ZooKeeper zooKeeper, OnElectionCallback onElectionCallback, SearchEngineLogger logger) {
        this.zooKeeper = zooKeeper;
        this.onElectionCallback = onElectionCallback;
        this.logger = logger;
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
        if (zooKeeper.exists(LEADER_INFO_PATH, false) == null) {
            zooKeeper.create(LEADER_INFO_PATH, new byte[]{},
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
        String predecessorName;
        Stat predecessorStat = null;

        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, this);
            Collections.sort(children);

            String smallestChild = children.getFirst();
            if (smallestChild.equals(currentZnodeName)) {
                isLeader = true;
                logger.systemInfo("Node " + currentZnodeName + " elected as leader");
                publishLeaderInfo();
                onElectionCallback.onElectedToBeLeader();
                return;
            } else {
                isLeader = false;
                logger.systemInfo("Node " + currentZnodeName + " initialized as worker");
                int predecessorIndex = children.indexOf(currentZnodeName) - 1;
                predecessorName = children.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorName, this);
                watchLeaderAndRegister();
                onElectionCallback.onWorker();
                logger.systemInfo("Watching predecessor znode: " + predecessorName);
            }
        }
    }

    private void publishLeaderInfo() throws KeeperException, InterruptedException {
        String leaderData = "localhost:" + serverPort;
        byte[] leaderBytes = leaderData.getBytes();
        zooKeeper.setData(LEADER_INFO_PATH, leaderBytes, -1);
        logger.systemInfo("Published new leader info: " + leaderData);
    }

    private void watchLeaderAndRegister() throws KeeperException, InterruptedException {
        Watcher leaderWatcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getType() == Event.EventType.NodeDataChanged) {
                    try {
                        byte[] leaderData = zooKeeper.getData(LEADER_INFO_PATH, this, null);
                        String leaderAddress = new String(leaderData);
                        registerWithLeader(leaderAddress);
                    } catch (InterruptedException | KeeperException e) {
                        logger.systemError("Failed to register with leader", e);
                    }
                }
            }
        };

        byte[] leaderData = zooKeeper.getData(LEADER_INFO_PATH, leaderWatcher, null);
        if (leaderData != null && leaderData.length > 0) {
            registerWithLeader(new String(leaderData));
        }
    }

    private void registerWithLeader(String leaderAddress) {
        if (isLeader) {
            logger.systemInfo("Skip registration as this node is the leader");
            return;
        }

        String[] hostPort = leaderAddress.split(":");
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(hostPort[0], Integer.parseInt(hostPort[1]))
                .usePlaintext()
                .build();

        CoordinatorServiceGrpc.CoordinatorServiceBlockingStub stub =
                CoordinatorServiceGrpc.newBlockingStub(channel);

        RegisterRequest request = RegisterRequest.newBuilder()
                .setWorkerId(currentZnodeName)
                .setAddress("localhost")
                .setPort(serverPort)
                .build();

        RegisterResponse response = stub.registerWorker(request);
        logger.systemInfo("Worker " + currentZnodeName + " registered with leader. Status: " + response.getStatus() + " on port: " + serverPort);
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted && !isLeader) {
            try {
                reelectLeader();
            } catch (Exception e) {
                logger.systemError("Error during leader reelection", e);
            }
        }
    }
}
