package ds.searchengine;

import io.grpc.stub.StreamObserver;

import org.springframework.stereotype.Service;
import proto.generated.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CoordinatorServiceImpl extends CoordinatorServiceGrpc.CoordinatorServiceImplBase {
    private final DocumentManager documentManager;
    private final ResultAggregator resultAggregator;
    private final ConcurrentHashMap<String, WorkerServiceGrpc.WorkerServiceBlockingStub> workerStubs;

    public CoordinatorServiceImpl(DocumentManager documentManager, ResultAggregator resultAggregator) {
        this.documentManager = documentManager;
        this.resultAggregator = resultAggregator;
        this.workerStubs = new ConcurrentHashMap<>();

    }

    @Override
    public void assignTask(TaskRequest request, StreamObserver<TaskResponse> responseObserver) {
        String taskId = request.getTaskId();
        String query = request.getQuery();

        List<WorkerServiceGrpc.WorkerServiceBlockingStub> availableWorkers =
                new ArrayList<>(workerStubs.values());

        if (availableWorkers.isEmpty()) {
            handleNoWorkersAvailable(responseObserver);
            return;
        }

        // Distribute documents among workers
        List<List<Document>> documentPartitions =
                documentManager.partitionDocuments(availableWorkers.size());

        // Collect results from all workers
        List<List<SearchResult>> allResults = new ArrayList<>();

        for (int i = 0; i < availableWorkers.size(); i++) {
            WorkerServiceGrpc.WorkerServiceBlockingStub worker = availableWorkers.get(i);
            List<Document> workerDocuments = documentPartitions.get(i);

            SearchRequest searchRequest = SearchRequest.newBuilder()
                    .setTaskId(taskId)
                    .setQuery(query)
                    .addAllDocuments(workerDocuments)
                    .build();

            SearchResponse response = worker.processSearch(searchRequest);
            allResults.add(response.getResultsList());
        }

        // Aggregate and store results
        List<SearchResult> finalResults = resultAggregator.aggregateResults(allResults);

        TaskResponse response = TaskResponse.newBuilder()
                .setStatus("Completed")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void handleNoWorkersAvailable(StreamObserver<TaskResponse> responseObserver) {
        TaskResponse response = TaskResponse.newBuilder()
                .setStatus("No workers available")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
