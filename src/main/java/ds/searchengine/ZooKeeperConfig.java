package ds.searchengine;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.CountDownLatch;

@Configuration
public class ZooKeeperConfig {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;

    @Bean(destroyMethod = "close")
    public ZooKeeper zooKeeper() throws Exception {
        final CountDownLatch connectionLatch = new CountDownLatch(1);
        try {
            ZooKeeper zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        connectionLatch.countDown();
                    }
                }
            });

            connectionLatch.await();
            return zooKeeper;
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to ZooKeeper", e);
        }
    }

    @Bean
    public OnElectionCallback onElectionCallback() {
        return new OnElectionCallback() {
            @Override
            public void onElectedToBeLeader() {
                System.out.println("Starting leader tasks...");
            }

            @Override
            public void onWorker() {
                System.out.println("Starting worker tasks...");
            }
        };
    }
}
