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

    private volatile boolean isIndexing = false;

    @Override
    public IndexingResponse startIndexing() {

        IndexingResponse indexingResponse = new IndexingResponse();

        if (isIndexing) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Индексация уже запущена");
            return indexingResponse;
        }

        isIndexing = true;
        try {
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
                LinkFinderTask task = new LinkFinderTask(site, url, url, cache, pageRepository, siteRepository);
                tasks.add(task);
            }

            ForkJoinPool[] pools = new ForkJoinPool[3];
            for (int i = 0; i < pools.length; i++) {
                pools[i] = new ForkJoinPool();
            }

            Thread[] threads = new Thread[pools.length];
            for (int i = 0; i < pools.length; i++) {
                final int index = i;
                threads[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        pools[index].invoke(tasks.get(index));
                    }
                });
                threads[i].start();
            }


            forkJoinPool.invoke(tasks.get(0));
            indexingResponse.setResult(true);
        } catch (Exception e) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Возникла ошибка: " + e.getMessage());
        } finally {
            isIndexing = false;
        }
        return indexingResponse;
    }
}
