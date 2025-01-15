package ds.searchengine;

import proto.generated.Document;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DocumentManager {
    private final Map<String, Document> allDocuments = new HashMap<>();

    public void addDocument(String id, String content) {
        Document doc = Document.newBuilder()
            .setId(id)
            .setContent(content)
            .build();
        allDocuments.put(id, doc);
    }

    public List<List<Document>> partitionDocuments(int workerCount) {
        List<List<Document>> partitions = new ArrayList<>();
        List<Document> documents = new ArrayList<>(allDocuments.values());
        int partitionSize = Math.max(1, documents.size() / workerCount);

        for (int i = 0; i < documents.size(); i += partitionSize) {
            int end = Math.min(i + partitionSize, documents.size());
            partitions.add(documents.subList(i, end));
        }
        return partitions;
    }
}
