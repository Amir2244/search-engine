package ds.searchengine;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import proto.generated.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Conditional(LeaderCondition.class)
public class CoordinatorServiceImpl extends CoordinatorServiceGrpc.CoordinatorServiceImplBase {
    private final DocumentManager documentManager;
    private final ResultAggregator resultAggregator;
    private final SearchEngineLogger logger;
    private final ConcurrentHashMap<String, WorkerServiceGrpc.WorkerServiceBlockingStub> workerStubs;

    public CoordinatorServiceImpl(DocumentManager documentManager, ResultAggregator resultAggregator, SearchEngineLogger logger) {
        this.documentManager = documentManager;
        this.resultAggregator = resultAggregator;
        this.logger = logger;
        this.workerStubs = new ConcurrentHashMap<>();
    }

    private Map<String, Double> calculateIDFScores(String query, List<Document> allDocuments) {
        String[] queryTerms = query.split("\\s+");
        Map<String, Double> idfScores = new HashMap<>();
        int totalDocs = allDocuments.size();

        for (String term : queryTerms) {
            long docsWithTerm = allDocuments.stream()
                    .filter(doc -> doc.getContent().toLowerCase().contains(term.toLowerCase()))
                    .count();
            double idf = Math.log((double) totalDocs / (1 + docsWithTerm));
            idfScores.put(term, idf);
        }

        return idfScores;
    }

    @Override
    public void assignTask(TaskRequest request, StreamObserver<TaskResponse> responseObserver) {
        String taskId = request.getTaskId();
        logger.infoTask(taskId, "Received new task request with query: " + request.getQuery());

        List<WorkerServiceGrpc.WorkerServiceBlockingStub> availableWorkers =
                new ArrayList<>(workerStubs.values());
        if (availableWorkers.isEmpty()) {
            handleNoWorkersAvailable(responseObserver);
            return;
        }

        List<Document> allDocuments = documentManager.loadDocuments();
        Map<String, Double> idfScores = calculateIDFScores(request.getQuery(), allDocuments);

        List<List<Document>> documentPartitions =
                documentManager.partitionDocuments(availableWorkers.size());

        List<List<SearchResult>> allResults = new ArrayList<>();

        for (int i = 0; i < availableWorkers.size(); i++) {
            WorkerServiceGrpc.WorkerServiceBlockingStub worker = availableWorkers.get(i);
            List<Document> workerDocuments = documentPartitions.get(i);

            SearchRequest searchRequest = SearchRequest.newBuilder()
                    .setTaskId(taskId)
                    .setWorkerId(workerStubs.keys().nextElement())
                    .setQuery(request.getQuery())
                    .addAllDocuments(workerDocuments)
                    .build();

            SearchResponse response = worker.processSearch(searchRequest);
            List<SearchResult> processedResults = new ArrayList<>();
            for (SearchResult searchResult : response.getResultsList()) {
                double score = searchResult.getTermFrequencies().getTermScoresMap().entrySet().stream()
                        .mapToDouble(entry -> entry.getValue() * idfScores.get(entry.getKey()))
                        .sum();
                SearchResult apply = SearchResult.newBuilder()
                        .setDocumentId(searchResult.getDocumentId())
                        .setScore(score)
                        .build();
                processedResults.add(apply);
            }

            allResults.add(processedResults);
        }

        List<SearchResult> finalResults = resultAggregator.aggregateResults(allResults);

        TaskResponse response = TaskResponse.newBuilder()
                .setStatus("Completed")
                .addAllResults(finalResults)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void registerWorker(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        String workerId = request.getWorkerId();
        logger.workerInfo(workerId, "Registering new worker at " + request.getAddress() + ":" + request.getPort());
        String address = request.getAddress();
        int port = request.getPort();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port)
                .usePlaintext()
                .build();
        WorkerServiceGrpc.WorkerServiceBlockingStub stub = WorkerServiceGrpc.newBlockingStub(channel);
        workerStubs.put(workerId, stub);

        RegisterResponse response = RegisterResponse.newBuilder()
                .setStatus("Registered")
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
