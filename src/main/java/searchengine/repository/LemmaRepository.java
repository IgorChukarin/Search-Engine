package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    boolean existsByLemmaAndSiteId(String lemma, Integer siteId);
    Lemma findByLemmaAndSiteId(String lemma, Integer siteId);
    Integer countBySiteId(Integer siteId);
    List<Lemma> findAllByLemma(String lemma);
}
