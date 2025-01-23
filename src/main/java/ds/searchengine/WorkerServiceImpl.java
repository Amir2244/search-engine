package ds.searchengine;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import proto.generated.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Service
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
        for (Document doc : request.getDocumentsList()) {
            try {
                String content = Files.readString(new File(getClass().getResource("/documents/" + doc.getId()).getFile()).toPath());
                Document fullDoc = Document.newBuilder()
                        .setId(doc.getId())
                        .setContent(content)
                        .build();
                documentStore.put(doc.getId(), fullDoc.getContent());
                logger.infoTask(taskId, "Loaded document: " + doc.getId());
            } catch (IOException e) {
                logger.errorTask(taskId, "Failed to load document: " + doc.getId(), e);
            }
        }

        String[] queryTerms = request.getQuery().split("\\s+");
        Map<String, Map<String, Double>> termFrequencies = tfidfCalculator.computeTermFrequencies(queryTerms);

        SearchResponse.Builder responseBuilder = SearchResponse.newBuilder()
                .setTaskId(request.getTaskId());

        termFrequencies.forEach((docId, termScores) -> {
            double score = termScores.values().stream().mapToDouble(Double::doubleValue).sum();

            TermFrequencies tfScores = TermFrequencies.newBuilder()
                    .putAllTermScores(termScores)
                    .build();

            SearchResult result = SearchResult.newBuilder()
                    .setDocumentId(docId)
                    .setScore(score)
                    .setTermFrequencies(tfScores)
                    .build();

            responseBuilder.addResults(result);
        });

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
        logger.infoTask(taskId, "Completed search processing for worker ");
    }
}
