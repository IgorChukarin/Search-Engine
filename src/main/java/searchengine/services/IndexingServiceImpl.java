package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SitesListConfig sitesList;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Override
    public IndexingResponse startIndexing() {
        List<LinkFinderTask> tasks = new ArrayList<>();
        siteRepository.deleteAll();
        for (SiteConfig siteConfig : sitesList.getSites()) {
            String url = siteConfig.getUrl();
            siteRepository.deleteByUrl(url);

            Site site = new Site();
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatus(SiteStatus.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            Set<String> cache = new HashSet<>();
            LinkFinderTask task = new LinkFinderTask(site, url, url, cache, pageRepository);
            tasks.add(task);
        }
        forkJoinPool.invoke(tasks.get(2));
        IndexingResponse indexingResponse = new IndexingResponse();
        indexingResponse.setResult(true);
        return indexingResponse;
    }
}
