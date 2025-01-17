package ds.searchengine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TFIDFCalculator {
    private final Map<String, String> documents;

    public TFIDFCalculator(Map<String, String> documents) {
        this.documents = documents;
    }

    public Map<String, Map<String, Double>> computeTermFrequencies(String[] queryTerms) {
        Map<String, Map<String, Double>> docTermFreqs = new HashMap<>();

        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String docId = entry.getKey();
            String content = entry.getValue();
            Map<String, Double> termFreqs = new HashMap<>();

            for (String term : queryTerms) {
                double tf = computeTF(term, content);
                termFreqs.put(term, tf);
            }

            docTermFreqs.put(docId, termFreqs);
        }

        return docTermFreqs;
    }

    private double computeTF(String term, String document) {
        String[] words = document.split("\\s+");
        long termCount = Arrays.stream(words).filter(word -> word.equalsIgnoreCase(term)).count();
        return (double) termCount / words.length;
    }
}
