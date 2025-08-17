package searchengine.services.repositoryService;

import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;

import java.util.List;
import java.util.Set;

public interface SearchIndexService {
    void save(SearchIndex searchIndex);

    void saveAll(List<SearchIndex> searchIndices);

    void deleteAllByPageId(Integer pageId);

    boolean existsByLemmaAndPage(Lemma lemma, Page page);

    boolean existsByLemma_LemmaAndPage_Id(String lemma, Integer pageId);

    List<SearchIndex> findAllByLemma_Lemma(String lemma);
}
