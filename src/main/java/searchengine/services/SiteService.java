package searchengine.services;

import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.time.LocalDateTime;

public interface SiteService {
    void save(Site site);
    void deleteByUrl(String url);
    Site findByUrl(String url);
}
