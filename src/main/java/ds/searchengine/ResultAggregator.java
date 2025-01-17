package ds.searchengine;

import proto.generated.SearchResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ResultAggregator {

    public List<SearchResult> aggregateResults(List<List<SearchResult>> workerResults) {
        Map<String, Double> combinedScores = new HashMap<>();

        for (List<SearchResult> results : workerResults) {
            for (SearchResult result : results) {
                combinedScores.merge(result.getDocumentId(),
                        result.getScore(),
                        Double::sum);
            }
        }

        return combinedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> SearchResult.newBuilder()
                        .setDocumentId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
