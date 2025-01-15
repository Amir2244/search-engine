package ds.searchengine;

import org.springframework.stereotype.Component;
import java.net.InetAddress;

@Component
class OnElectionAction implements OnElectionCallback {
    private final CoordinatorServiceImpl coordinatorService;
    private final WorkerServiceImpl workerService;

    private int port;

    public OnElectionAction(CoordinatorServiceImpl coordinatorService,
                            WorkerServiceImpl workerService) {
        this.coordinatorService = coordinatorService;
        this.workerService = workerService;
    }

    @Override
    public void onElectedToBeLeader() {
        try {
            String address = String.format("http://%s:%d",
                    InetAddress.getLocalHost().getCanonicalHostName(), port);
            System.out.println("Node is now the leader at " + address);
        } catch (Exception e) {
            System.err.println("Error while transitioning to leader: " + e.getMessage());
        }
    }

    @Override
    public void onWorker() {
        try {
            String workerAddress = String.format("http://%s:%d",
                    InetAddress.getLocalHost().getCanonicalHostName(), port);
            System.out.println("Node is now a worker at " + workerAddress);
        } catch (Exception e) {
            System.err.println("Error while transitioning to worker: " + e.getMessage());
        }
    }
}
