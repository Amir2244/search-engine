package ds.searchengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SearchEngineLogger {
    private final Logger logger;

    public SearchEngineLogger() {
        this.logger = LoggerFactory.getLogger(SearchEngineLogger.class);
    }

    public void infoTask(String taskId, String message) {
        logger.info("[Task: {}] {}", taskId, message);
    }

    public void errorTask(String taskId, String message, Throwable error) {
        logger.error("[Task: {}] {} - Error: {}", taskId, message, error.getMessage());
    }

    public void debugTask(String taskId, String message) {
        logger.debug("[Task: {}] {}", taskId, message);
    }

    public void workerInfo(String workerId, String message) {
        logger.info("[Worker: {}] {}", workerId, message);
    }

    public void workerError(String workerId, String message, Throwable error) {
        logger.error("[Worker: {}] {} - Error: {}", workerId, message, error.getMessage());
    }

    public void systemInfo(String message) {
        logger.info("[System] {}", message);
    }

    public void systemError(String message, Throwable error) {
        logger.error("[System] {} - Error: {}", message, error.getMessage());
    }
}
