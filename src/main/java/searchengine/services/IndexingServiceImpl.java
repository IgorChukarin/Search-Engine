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
import java.util.concurrent.*;

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
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        isIndexing = true;
        try {
            List<LinkFinderAction> actions = new ArrayList<>();
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
                LinkFinderAction linkFinderAction = new LinkFinderAction(site, url, url, cache, pageRepository, siteRepository);
                actions.add(linkFinderAction);
            }

            List<RunnableFuture<String>> futureTasks = new ArrayList<>();
            for (LinkFinderAction action : actions) {
                futureTasks.add(createFutureTask(action));
            }

            ExecutorService executor = Executors.newFixedThreadPool(4);
            futureTasks.forEach(executor::execute);
            SiteIndexingResultHandler resultCheckerExample = new SiteIndexingResultHandler(futureTasks, siteRepository);
            executor.execute(resultCheckerExample);
            executor.shutdown();
            indexingResponse.setResult(true);
        }
        catch (Exception e) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Возникла ошибка в indexing service");
        }
        return indexingResponse;
    }

    private RunnableFuture<String> createFutureTask(LinkFinderAction linkFinderTask) {
        RunnableFuture<String> future = new FutureTask<>(new RunnableForkJoin(linkFinderTask), linkFinderTask.getSite().getUrl());
        return future;
    }
}