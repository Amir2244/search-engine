package ds.searchengine;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
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
                .executor(new VirtualThreadExecutor("VirtualThreadExecutor"))
                .build();
    }

    @Bean
    public DocumentManager documentManager() {
        return new DocumentManager();
    }

    @Bean
    public ResultAggregator resultAggregator() {
        return new ResultAggregator();
    }

}
