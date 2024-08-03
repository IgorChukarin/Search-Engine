package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.linkFinderClasses.LinkFinderAction;
import searchengine.services.linkFinderClasses.RunnableForkJoin;
import searchengine.services.linkFinderClasses.SiteIndexingResultHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SitesListConfig sitesList;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final PageService pageService;
    private final JsoupConfig jsoupConfig;

    private volatile boolean isIndexing = false;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

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

                Set<String> cache = ConcurrentHashMap.newKeySet();
                LinkFinderAction linkFinderAction = new LinkFinderAction(site, url, url, cache, pageRepository, siteRepository, pageService, jsoupConfig);
                actions.add(linkFinderAction);
            }

            List<RunnableFuture<String>> futureTasks = new ArrayList<>();
            for (LinkFinderAction action : actions) {
                futureTasks.add(createFutureTask(action));
            }

            futureTasks.forEach(executor::execute);
            SiteIndexingResultHandler resultCheckerExample = new SiteIndexingResultHandler(futureTasks, siteRepository);

            executor.execute(resultCheckerExample);
            executor.shutdown();
            indexingResponse.setResult(true);
        }
        catch (Exception e) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Возникла ошибка в indexing service");

            e.printStackTrace();
        }
        return indexingResponse;
    }

    private RunnableFuture<String> createFutureTask(LinkFinderAction linkFinderTask) {
        RunnableFuture<String> future = new FutureTask<>(new RunnableForkJoin(linkFinderTask), linkFinderTask.getSite().getUrl());
        return future;
    }


    public IndexingResponse stopIndexing() {
        return new IndexingResponse();
    }
}