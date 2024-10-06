package searchengine.services.RepositoryServices;

import searchengine.model.Lemma;

import java.util.List;

public interface LemmaService {
    boolean existsByLemmaAndSiteId(String lemma, Integer siteId);

    Lemma findByLemmaAndSiteId(String lemma, Integer siteId);

    void save(Lemma lemma);

    long count();

    Integer countBySiteId(Integer siteId);

    List<Lemma> findAllByLemma(String lemma);
}
