package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    boolean existsByLemmaAndSiteId(String lemma, Integer siteId);
    Lemma findByLemmaAndSiteId(String lemma, Integer siteId);
}
