package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.NegativeIndexingResponse;

public interface IndexingService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
}
