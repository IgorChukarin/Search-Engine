package searchengine.services;

import searchengine.model.Site;

public interface PageService {
    boolean save(String path, Integer code, String content, Site site);
    boolean existsByPathAndSiteId(String path, Integer siteId);
}
