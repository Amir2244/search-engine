package ds.searchengine;

import io.grpc.Server;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@SpringBootApplication
public class SearchEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchEngineApplication.class, args);
    }
}

@Component
class LeaderElectionInitializer {
    private static final Logger logger = Logger.getLogger(LeaderElectionInitializer.class.getName());
    private final ZooKeeper zooKeeper;
    private final LeaderElectionService leaderElection;
    private final Server grpcServer;
    private final OnElectionCallback callback;
    private final CoordinatorServiceImpl coordinatorService;

    public LeaderElectionInitializer(ZooKeeper zooKeeper, Server grpcServer, CoordinatorServiceImpl coordinatorService, OnElectionCallback callback) {
        this.zooKeeper = zooKeeper;
        this.grpcServer = grpcServer;
        this.callback = callback;
        this.coordinatorService = coordinatorService;
        this.leaderElection = new LeaderElectionService(zooKeeper, this.callback, coordinatorService);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeLeaderElection() throws Exception {
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();
    }
}
