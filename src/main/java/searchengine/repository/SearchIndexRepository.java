package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;

import java.util.List;
import java.util.Optional;

public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {
    List<SearchIndex> findAllByLemma(Lemma lemma);
    boolean existsByLemmaAndPage(Lemma lemma, Page page);
    Optional<SearchIndex> findByLemmaAndPage(Lemma lemma, Page page);
}
