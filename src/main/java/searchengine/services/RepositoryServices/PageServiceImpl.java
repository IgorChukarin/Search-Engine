package searchengine.services.RepositoryServices;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService{
    private final PageRepository pageRepository;

    @Override
    @Async
    @Transactional
    public void saveIfNotExist(String path, Integer code, String content, Site site) {
        if (!pageRepository.existsByPathAndSiteId(path, site.getId())) {
            Page page = new Page(site, path, code, content);
            pageRepository.save(page);
        }
    }

    @Override
    public boolean existsByPathAndSiteId(String path, Integer siteId) {
        return pageRepository.existsByPathAndSiteId(path, siteId);
    }

    @Override
    public List<Page> findAllByPath(String path) {
        List<Page> pages = pageRepository.findAllByPath(path);
        return pages;
    }

    @Override
    public long count() {
        return pageRepository.count();
    }

    @Override
    public Integer countPagesBySiteId(Integer id) {
        return pageRepository.countBySiteId(id);
    }
}
