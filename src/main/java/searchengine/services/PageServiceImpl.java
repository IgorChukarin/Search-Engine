package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageServiceImpl implements PageService{
    private final PageRepository pageRepository;

    @Override
    public boolean save(String path, Integer code, String content, Site site) {
        Optional<Page> optionalPage = pageRepository.findByPathAndSiteId(path, site.getId());
        if (optionalPage.isEmpty()) {
            Page page = new Page(site, path, code, content);
            pageRepository.save(page);
            return true;
        }
        return false;
    }

    @Override
    public boolean existsByPathAndSiteId(String path, Integer siteId) {
        return pageRepository.existsByPathAndSiteId(path, siteId);
    }
}
