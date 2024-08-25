package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.services.linkFinderClasses.LinkFinderAction;
import searchengine.services.linkFinderClasses.RunnableForkJoin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@Setter
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{
    private final SitesListConfig sitesList;
    private final SiteService siteService;
    private final PageService pageService;
    private final JsoupConfig jsoupConfig;
    private static boolean isIndexing;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final IndexingResultHandler indexingResultHandler;


    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();
        if (!canStartIndexing()) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        try {
            List<LinkFinderAction> actions = prepareSitesForIndexing();
            List<RunnableFuture<String>> runnableFutureTasks = createAndSubmitTasks(actions);
            handleIndexingResults(runnableFutureTasks);
            indexingResponse.setResult(true);
        }
        catch (Exception e) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Ошибка в indexingService");
            e.printStackTrace();
        }
        return indexingResponse;
    }


    private boolean canStartIndexing() {
        if (isIndexing) {
            return false;
        } else {
            isIndexing = true;
            return true;
        }
    }


    private List<LinkFinderAction> prepareSitesForIndexing() {
        List<LinkFinderAction> actions = new ArrayList<>();
        for (SiteConfig siteConfig : sitesList.getSites()) {
            String url = siteConfig.getUrl();
            siteService.deleteByUrl(url);
            Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), siteConfig.getUrl(), siteConfig.getName());
            siteService.save(site);
            LinkFinderAction linkFinderAction = new LinkFinderAction(site, url, url, siteService, pageService, jsoupConfig);
            actions.add(linkFinderAction);
        }
        return actions;
    }


    private List<RunnableFuture<String>> createAndSubmitTasks(List<LinkFinderAction> actions) {
        List<RunnableFuture<String>> runnableFutureTasks = new ArrayList<>();
        for (LinkFinderAction action : actions) {
            RunnableFuture<String> runnableFutureTask = createFutureTask(action);
            runnableFutureTasks.add(runnableFutureTask);
        }
        runnableFutureTasks.forEach(executor::execute);
        return runnableFutureTasks;
    }


    private void handleIndexingResults(List<RunnableFuture<String>> runnableFutureTasks) {
        indexingResultHandler.setRunnableFutureList(runnableFutureTasks);
        executor.execute(indexingResultHandler);
        executor.shutdown();
    }


    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();
        LinkFinderAction.stopFinding();
        for (SiteConfig siteConfig : sitesList.getSites()) {
            String url = siteConfig.getUrl();
            Site site = siteService.findByUrl(url);
            if (site.getStatus() != SiteStatus.INDEXED) {
                site.setStatus(SiteStatus.FAILED);
            }
            siteService.save(site);
        }
        isIndexing = false;
        indexingResponse.setResult(true);
        return indexingResponse;
    }


    private RunnableFuture<String> createFutureTask(LinkFinderAction linkFinderAction) {
        RunnableFuture<String> future = new FutureTask<>(new RunnableForkJoin(linkFinderAction), linkFinderAction.getSite().getUrl());
        return future;
    }
}