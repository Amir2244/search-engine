package ds.searchengine;

import org.apache.zookeeper.ZooKeeper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;



@SpringBootApplication
public class SearchEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchEngineApplication.class, args);
    }
}

@Component
class LeaderElectionInitializer {
    private final LeaderElectionService leaderElection;

    public LeaderElectionInitializer(ZooKeeper zooKeeper, CoordinatorServiceImpl coordinatorService, OnElectionCallback callback) {
        this.leaderElection = new LeaderElectionService(zooKeeper, callback, coordinatorService);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeLeaderElection() throws Exception {
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();
    }
}
