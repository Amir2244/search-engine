package ds.searchengine;

import org.springframework.stereotype.Component;
import proto.generated.SearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        List<Map.Entry<String, Double>> toSort = new ArrayList<>(combinedScores.entrySet());
        toSort.sort(Map.Entry.<String, Double>comparingByValue().reversed());
        List<SearchResult> list = new ArrayList<>();
        for (Map.Entry<String, Double> entry : toSort) {
            SearchResult build = SearchResult.newBuilder()
                    .setDocumentId(entry.getKey())
                    .setScore(entry.getValue())
                    .build();
            list.add(build);
        }
        return list;
    }
}
