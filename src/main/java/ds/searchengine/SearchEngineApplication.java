package ds.searchengine;

import io.grpc.Server;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.logging.Logger;
import java.util.logging.Level;

@SpringBootApplication
public class SearchEngineApplication {
    private static final Logger logger = Logger.getLogger(SearchEngineApplication.class.getName());

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(SearchEngineApplication.class, args);
    }
}

@Component
class LeaderElectionInitializer {
    private static final Logger logger = Logger.getLogger(LeaderElectionInitializer.class.getName());
    private final ZooKeeper zooKeeper;
    private final LeaderElectionService leaderElection;
    private final Server grpcServer;
    private final OnElectionCallback callback;

    public LeaderElectionInitializer(ZooKeeper zooKeeper, Server grpcServer) {
        this.zooKeeper = zooKeeper;
        this.grpcServer = grpcServer;
        this.callback = new OnElectionCallback() {
            @Override
            public void onElectedToBeLeader() {
                try {
                    grpcServer.start();
                    logger.info("gRPC Server started for leader");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to start gRPC server", e);
                }
            }

            @Override
            public void onWorker() {
                logger.info("Node initialized as worker");
            }
        };
        this.leaderElection = new LeaderElectionService(zooKeeper, this.callback);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeLeaderElection() throws Exception {
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();
    }
}