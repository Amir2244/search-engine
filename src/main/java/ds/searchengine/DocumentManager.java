package ds.searchengine;

import jakarta.annotation.PostConstruct;
import proto.generated.Document;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

@Component
public class DocumentManager {
    private final Map<String, Document> allDocuments = new HashMap<>();

    @PostConstruct
    public List<Document> loadDocuments() {
        try (InputStream inputStream = getClass().getResourceAsStream("src/main/resources/documents")) {
            File documentsDir = new File(Objects.requireNonNull(getClass().getResource("/documents")).getFile());
            for (File file : Objects.requireNonNull(documentsDir.listFiles())) {
                String content = Files.readString(file.toPath());
                Document doc = Document.newBuilder()
                        .setId(file.getName())
                        .setContent(content)
                        .build();
                allDocuments.put(file.getName(), doc);
            }
            return new ArrayList<>(allDocuments.values());
        } catch (IOException e) {
            throw new InputMismatchException();
        }
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
