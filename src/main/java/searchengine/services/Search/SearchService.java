package searchengine.services.Search;

import searchengine.dto.indexing.Response;

public interface SearchService {
    Response search(String query, String site, int offset, int limit);
}
