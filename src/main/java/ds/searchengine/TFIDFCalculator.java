package ds.searchengine;
import java.util.*;
import java.util.stream.Collectors;

public class TFIDFCalculator {

    private final Map<String, String> documents;

    public TFIDFCalculator(Map<String, String> documents) {
        this.documents = documents;
    }

    /**
     * Computes the term frequency (TF).
     *
     * @param term     The term to compute TF for.
     * @param document The document to analyze.
     * @return The term frequency of the term in the document.
     */
    private double computeTF(String term, String document) {
        String[] words = document.split("\\s+");
        long termCount = Arrays.stream(words).filter(word -> word.equalsIgnoreCase(term)).count();
        return (double) termCount / words.length;
    }

    /**
     * Computes the inverse document frequency (IDF).
     *
     * @param term The term to compute IDF for.
     * @return The inverse document frequency of the term across all documents.
     */
    private double computeIDF(String term) {
        long totalDocs = documents.size();
        long docsWithTerm = documents.values().stream()
                .filter(doc -> Arrays.stream(doc.split("\\s+")).anyMatch(word -> word.equalsIgnoreCase(term)))
                .count();
        return Math.log((double) totalDocs / (1 + docsWithTerm));
    }

    /**
     * Computes TF-IDF scores for all documents based on a query.
     *
     * @param query The search query.
     * @return A map of document names to their TF-IDF scores.
     */
    public Map<String, Double> computeTFIDF(String query) {
        String[] queryTerms = query.split("\\s+");
        Map<String, Double> scores = new HashMap<>();

        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String docName = entry.getKey();
            String docContent = entry.getValue();

            double tfidf = Arrays.stream(queryTerms)
                    .mapToDouble(term -> computeTF(term, docContent) * computeIDF(term))
                    .sum();

            scores.put(docName, tfidf);
        }

        // Sort documents by TF-IDF score in descending order
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}