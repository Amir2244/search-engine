package ds.searchengine;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Bean
    public Server grpcServer(WorkerServiceImpl workerService,
                             CoordinatorServiceImpl coordinatorService) {
        return ServerBuilder.forPort(9090)
                .addService(workerService)
                .addService(coordinatorService)
                .build();
    }

    @Bean
    public WorkerServiceImpl workerService() {
        return new WorkerServiceImpl();
    }

    @Bean
    public DocumentManager documentManager() {
        return new DocumentManager();
    }

    @Bean
    public ResultAggregator resultAggregator() {
        return new ResultAggregator();
    }

    @Bean
    public CoordinatorServiceImpl coordinatorService(DocumentManager documentManager,
                                                     ResultAggregator resultAggregator) {
        return new CoordinatorServiceImpl(documentManager, resultAggregator);
    }
}
