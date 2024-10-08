package searchengine.services.RepositoryServices;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService{

    private final SiteRepository siteRepository;


    @Override
    public void save(Site site) {
        siteRepository.save(site);
    }


    @Override
    @Async
    @Transactional
    public void deleteByUrl(String url) {
        siteRepository.deleteByUrl(url);
    }


    @Override
    public Site findByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    @Override
    public long count() {
        return siteRepository.count();
    }
}
