package searchengine.services.RepositoryServices;

import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

public interface PageService {
    void saveIfNotExist(String path, Integer code, String content, Site site);
    boolean existsByPathAndSiteId(String path, Integer siteId);
    List<Page> findAllByPath(String path);
    long count();
}
