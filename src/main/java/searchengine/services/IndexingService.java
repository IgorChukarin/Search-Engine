package searchengine.services;

import searchengine.dto.indexing.NegativeIndexingResponse;

public interface IndexingService {
    NegativeIndexingResponse startIndexing();
    NegativeIndexingResponse stopIndexing();
}
