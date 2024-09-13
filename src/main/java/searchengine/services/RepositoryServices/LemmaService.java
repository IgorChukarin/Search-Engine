package searchengine.services.RepositoryServices;

import searchengine.model.Lemma;

public interface LemmaService {
    boolean existsByLemmaAndSiteId(String lemma, Integer siteId);
    Lemma findByLemmaAndSiteId(String lemma, Integer siteId);
    void save(Lemma lemma);
}
