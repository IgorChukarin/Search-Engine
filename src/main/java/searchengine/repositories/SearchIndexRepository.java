package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;

import java.util.List;

public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {
    List<SearchIndex> findAllByLemma(Lemma lemma);
    boolean existsByLemmaAndPage(Lemma lemma, Page page);
}
