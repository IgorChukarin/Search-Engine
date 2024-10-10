package searchengine.services.Search;

import searchengine.dto.indexing.Response;
import searchengine.dto.indexing.SearchResponse;

public interface SearchService {
    SearchResponse search(String query, String site, int offset, int limit);
}
