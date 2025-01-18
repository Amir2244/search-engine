package ds.searchengine;

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
    private boolean initialized = false;

    public LeaderElectionInitializer(LeaderElectionService leaderElectionService) {
        this.leaderElection = leaderElectionService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeLeaderElection() throws Exception {
        if (!initialized) {
            leaderElection.volunteerForLeadership();
            leaderElection.reelectLeader();
            initialized = true;
        }
    }
}
