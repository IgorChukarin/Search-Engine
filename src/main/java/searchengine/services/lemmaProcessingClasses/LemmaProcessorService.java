package searchengine.services.lemmaProcessingClasses;

import searchengine.dto.indexing.Response;

public interface LemmaProcessorService {
    Response IndexPage(String url);
}
