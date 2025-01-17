package ds.searchengine;

import io.grpc.Server;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.ConnectException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
public class Configurations {
    private static final Logger LOGGER = Logger.getLogger(Configurations.class.getName());
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 5000;

    @Bean(destroyMethod = "close")
    public ZooKeeper zooKeeper() throws ConnectException, InterruptedException {
        final CountDownLatch connectionLatch = new CountDownLatch(1);
        try {
            try (ZooKeeper zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connectionLatch.countDown();
                }
            })) {

                connectionLatch.await();
                return zooKeeper;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Interrupted while waiting for ZooKeeper connection");
        } catch (Exception e) {
            throw new ConnectException("Failed to connect to ZooKeeper");
        }
    }

    @Bean
    public OnElectionCallback onElectionCallback(Server grpcServer) {
        return new OnElectionCallback() {
            @Override
            public void onElectedToBeLeader() {
                try {
                    grpcServer.start();
                    LOGGER.info("gRPC Server started for leader");
                } catch (Exception e) {
                    grpcServer.shutdown();
                    LOGGER.log(Level.SEVERE, "Failed to start gRPC server", e);
                }
            }

            @Override
            public void onWorker() {
                LOGGER.info("Node initialized as worker");
            }
        };
    }
}
