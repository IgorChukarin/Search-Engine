package searchengine.services.lemmaProcessingClasses;

import searchengine.dto.indexing.NegativeIndexingResponse;

public interface LemmaProcessorService {
    public NegativeIndexingResponse IndexPage(String url);
}
