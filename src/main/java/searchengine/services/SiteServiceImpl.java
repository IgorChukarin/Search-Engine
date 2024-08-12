package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService{

    private final SiteRepository siteRepository;

    @Override
    public void save(Site site) {
        siteRepository.save(site);
    }

    @Override
    public void deleteByUrl(String url) {
        siteRepository.deleteByUrl(url);
    }
}
