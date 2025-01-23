package ds.searchengine;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import proto.generated.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CoordinatorServiceImpl extends CoordinatorServiceGrpc.CoordinatorServiceImplBase {
    private final SearchEngineLogger logger;
    private final ConcurrentHashMap<String, WorkerServiceGrpc.WorkerServiceBlockingStub> workerStubs;

    public CoordinatorServiceImpl(SearchEngineLogger logger) {
        this.logger = logger;
        this.workerStubs = new ConcurrentHashMap<>();
    }

    private Map<String, Double> calculateIDFScores(String query, List<SearchResult> allResults) {
        String[] queryTerms = query.split("\\s+");
        Map<String, Double> idfScores = new HashMap<>();
        int totalDocs = allResults.size();

        for (String term : queryTerms) {
            long docsWithTerm = allResults.stream()
                    .filter(result -> result.getTermFrequencies().getTermScoresMap().containsKey(term))
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

        List<String> allDocumentNames = getDocumentNames();
        int documentsPerWorker = Math.max(1, allDocumentNames.size() / availableWorkers.size());
        List<List<SearchResult>> allResults = new ArrayList<>();

        for (int i = 0; i < availableWorkers.size(); i++) {
            int start = i * documentsPerWorker;
            int end = Math.min(start + documentsPerWorker, allDocumentNames.size());
            List<String> workerDocNames = allDocumentNames.subList(start, end);

            List<Document> workerDocs = workerDocNames.stream()
                    .map(name -> Document.newBuilder().setId(name).build())
                    .collect(Collectors.toList());

            SearchRequest searchRequest = SearchRequest.newBuilder()
                    .setTaskId(taskId)
                    .setQuery(request.getQuery())
                    .addAllDocuments(workerDocs)
                    .build();

            SearchResponse response = availableWorkers.get(i).processSearch(searchRequest);
            allResults.add(response.getResultsList());
        }

        List<SearchResult> flatResults = allResults.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        Map<String, Double> idfScores = calculateIDFScores(request.getQuery(), flatResults);

        List<SearchResult> finalResults = flatResults.stream()
                .map(result -> {
                    double score = result.getTermFrequencies().getTermScoresMap().entrySet().stream()
                            .mapToDouble(entry -> entry.getValue() * idfScores.getOrDefault(entry.getKey(), 0.0))
                            .sum();
                    return SearchResult.newBuilder()
                            .setDocumentId(result.getDocumentId())
                            .setScore(score)
                            .setTermFrequencies(result.getTermFrequencies())
                            .build();
                })
                .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                .collect(Collectors.toList());

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

        ManagedChannel channel = ManagedChannelBuilder.forAddress(request.getAddress(), request.getPort())
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

    private List<String> getDocumentNames() {
        try {
            File baseDir = new File(Objects.requireNonNull(getClass().getResource("/documents")).getFile());
            return Arrays.stream(Objects.requireNonNull(baseDir.listFiles()))
                    .map(File::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.systemError("Failed to get document names", e);
            throw new RuntimeException("Could not get document names", e);
        }
    }

    private void handleNoWorkersAvailable(StreamObserver<TaskResponse> responseObserver) {
        TaskResponse response = TaskResponse.newBuilder()
                .setStatus("No workers available")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
