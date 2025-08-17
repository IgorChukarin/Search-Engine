package searchengine.services.repositoryService;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService{
    private final PageRepository pageRepository;

    @Override
    @Async
    @Transactional
    public boolean saveIfNotExist(Page page) {
        String path = page.getPath();
        Integer siteId = page.getSite().getId();
        if (!pageRepository.existsByPathAndSiteId(path, siteId)) {
            pageRepository.save(page);
            return true;
        }
        return false;
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

    @Override
    public void delete(Page page) {
        pageRepository.delete(page);
    }

    @Override
    public void saveAll(List<Page> pages) {
        pageRepository.saveAll(pages);
    }
}
