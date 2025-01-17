package ds.searchengine;

import io.grpc.stub.StreamObserver;

import org.springframework.stereotype.Component;
import proto.generated.*;

import java.util.HashMap;
import java.util.Map;
@Component
public class WorkerServiceImpl extends WorkerServiceGrpc.WorkerServiceImplBase {
    private final Map<String, String> documentStore = new HashMap<>();
    private final TFIDFCalculator tfidfCalculator;
    private final SearchEngineLogger logger;

    public WorkerServiceImpl(SearchEngineLogger logger) {
        this.tfidfCalculator = new TFIDFCalculator(documentStore);
        this.logger = logger;
    }

    @Override
    public void processSearch(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {
        String taskId = request.getTaskId();
        logger.infoTask(taskId, "Processing search request for worker partition");

        documentStore.clear();
        request.getDocumentsList().forEach(doc ->
                documentStore.put(doc.getId(), doc.getContent())
        );

        String[] queryTerms = request.getQuery().split("\\s+");
        Map<String, Map<String, Double>> termFrequencies = tfidfCalculator.computeTermFrequencies(queryTerms);

        SearchResponse.Builder responseBuilder = SearchResponse.newBuilder()
                .setTaskId(request.getTaskId());

        termFrequencies.forEach((docId, termScores) -> {
            TermFrequencies tfScores = TermFrequencies.newBuilder()
                    .putAllTermScores(termScores)
                    .build();

            SearchResult result = SearchResult.newBuilder()
                    .setDocumentId(docId)
                    .setTermFrequencies(tfScores)
                    .build();

            responseBuilder.addResults(result);
        });

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
