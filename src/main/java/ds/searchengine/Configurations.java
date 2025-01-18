package ds.searchengine;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Configuration
public class Configurations implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(Configurations.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    @Value("${zookeeper.host}")
    private String host;

    @Value("${zookeeper.port}")
    private int port;

    @Value("${zookeeper.session_timeout}")
    private int sessionTimeout;

    @Value("${zookeeper.connection_timeout}")
    private int connectionTimeout;

    @Bean
    public DocumentManager documentManager() {
        return new DocumentManager();
    }

    @Bean
    public ResultAggregator resultAggregator() {
        return new ResultAggregator();
    }

    @Bean
    public SearchEngineLogger searchEngineLogger() {
        return new SearchEngineLogger();
    }

    @Bean
    public OnElectionCallback onElectionCallback() {
        return new OnElectionCallback() {
            @Override
            public void onElectedToBeLeader() {
                LOGGER.info("Node elected as leader");
            }

            @Override
            public void onWorker() {
                LOGGER.info("Node initialized as worker");
            }
        };
    }

    @Bean(destroyMethod = "close")
    public ZooKeeper zooKeeper() throws Exception {
        final CountDownLatch connectionLatch = new CountDownLatch(1);
        String connectString = host + ":" + port;

        ZooKeeper zooKeeper = null;
        int retryCount = 0;
        ZooKeeper tempZooKeeper = null;
        while (retryCount < MAX_RETRIES && zooKeeper == null) {
            try {
                tempZooKeeper = new ZooKeeper(connectString, sessionTimeout, event -> {
                    if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        connectionLatch.countDown();
                    }
                });
                if (!connectionLatch.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException("Timeout waiting for ZooKeeper connection");
                }
                LOGGER.info("Successfully connected to ZooKeeper");
                zooKeeper = tempZooKeeper;
                tempZooKeeper = null;
            } catch (Exception e) {
                LOGGER.warning("Failed to connect to ZooKeeper, attempt " + (retryCount + 1) + " of " + MAX_RETRIES);
                retryCount++;
                if (retryCount == MAX_RETRIES) {
                    throw new IllegalStateException("Failed to connect to ZooKeeper after " + MAX_RETRIES + " attempts", e);
                }
                TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
            } finally {
                if (tempZooKeeper != null) {
                    tempZooKeeper.close();
                }
            }
        }
        return zooKeeper;
    }

    @Override
    public void close() throws Exception {
        LOGGER.info("Closing ZooKeeper connection");
        zooKeeper().close();
    }
}
