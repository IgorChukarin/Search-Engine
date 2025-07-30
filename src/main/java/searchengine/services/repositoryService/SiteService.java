package searchengine.services.repositoryService;

import searchengine.model.Site;

public interface SiteService {
    void save(Site site);
    void deleteByUrl(String url);
    Site findByUrl(String url);
    long count();
}
