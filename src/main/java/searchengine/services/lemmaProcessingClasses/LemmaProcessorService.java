package searchengine.services.lemmaProcessingClasses;

import searchengine.dto.indexing.IndexingResponse;

public interface LemmaProcessorService {
    public IndexingResponse IndexPage(String url);
}
