package searchengine.services.lemmaProcessingClasses;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.NegativeIndexingResponse;

public interface LemmaProcessorService {
    public IndexingResponse IndexPage(String url);
}
