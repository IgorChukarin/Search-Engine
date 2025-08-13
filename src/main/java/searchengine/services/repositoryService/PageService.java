package searchengine.services.repositoryService;

import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

public interface PageService {

    boolean saveIfNotExist(Page page);

    boolean existsByPathAndSiteId(String path, Integer siteId);

    List<Page> findAllByPath(String path);

    long count();

    Integer countPagesBySiteId(Integer id);

    void delete(Page page);
}
