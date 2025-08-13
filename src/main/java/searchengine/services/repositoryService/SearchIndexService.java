package searchengine.services.repositoryService;

import searchengine.model.SearchIndex;

import java.util.List;

public interface SearchIndexService {
    void save(SearchIndex searchIndex);

    void saveAll(List<SearchIndex> searchIndices);

    void deleteAllByPageId(Integer pageId);
}
