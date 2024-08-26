package searchengine.services;

import searchengine.model.Site;

public interface PageService {
    void saveIfNotExist(String path, Integer code, String content, Site site);
    boolean existsByPathAndSiteId(String path, Integer siteId);
}
