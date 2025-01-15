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

    public WorkerServiceImpl() {
        this.tfidfCalculator = new TFIDFCalculator(documentStore);
    }

    @Override
    public void processSearch(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {
        request.getDocumentsList().forEach(doc ->
            documentStore.put(doc.getId(), doc.getContent())
        );

        Map<String, Double> scores = tfidfCalculator.computeTFIDF(request.getQuery());

        SearchResponse.Builder responseBuilder = SearchResponse.newBuilder()
            .setTaskId(request.getTaskId());

        scores.forEach((docId, score) -> {
            SearchResult result = SearchResult.newBuilder()
                .setDocumentId(docId)
                .setScore(score)
                .build();
            responseBuilder.addResults(result);
        });

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
