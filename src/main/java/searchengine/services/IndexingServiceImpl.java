package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.indexing.Response;
import searchengine.dto.indexing.NegativeResponse;
import searchengine.dto.indexing.PositiveResponse;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.services.RepositoryServices.PageService;
import searchengine.services.RepositoryServices.SiteService;
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
    private final SiteService siteService;
    private final PageService pageService;
    private final JsoupConfig jsoupConfig;
    private final SitesListConfig sitesList;
    private final IndexingResultHandler indexingResultHandler;
    private static volatile boolean isIndexing;


    @Override
    public Response startIndexing() {
        if (!canStartIndexing()) {
            return new NegativeResponse("Индексация уже запущена");
        }
        LinkFinderAction.unlockAction();
        try {
            List<LinkFinderAction> actions = prepareSitesForIndexing();
            List<RunnableFuture<String>> runnableFutureTasks = createAndSubmitTasks(actions);
            handleIndexingResults(runnableFutureTasks);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new NegativeResponse("Ошибка в indexingService");
        }
        return new PositiveResponse();
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
        ExecutorService taskExecutor = Executors.newFixedThreadPool(4);
        runnableFutureTasks.forEach(taskExecutor::execute);
        taskExecutor.shutdown();
        return runnableFutureTasks;
    }


    private RunnableFuture<String> createFutureTask(LinkFinderAction linkFinderAction) {
        RunnableFuture<String> future = new FutureTask<>(new RunnableForkJoin(linkFinderAction), linkFinderAction.getSite().getUrl());
        return future;
    }


    private void handleIndexingResults(List<RunnableFuture<String>> runnableFutureTasks) {
        ExecutorService monitoringExecutor = Executors.newSingleThreadExecutor();
        indexingResultHandler.setRunnableFutureList(runnableFutureTasks);
        monitoringExecutor.execute(indexingResultHandler);
        monitoringExecutor.shutdown();
    }


    @Override
    public Response stopIndexing() {
        if (!canStopIndexing()) {
            return new NegativeResponse("Индексация не запущена");
        }
        LinkFinderAction.lockAction();
        changeUnfinishedSiteStatus();
        return new PositiveResponse();
    }


    private boolean canStopIndexing() {
        if (!isIndexing) {
            return false;
        } else {
            isIndexing = false;
            return true;
        }
    }


    private void changeUnfinishedSiteStatus() {
        for (SiteConfig siteConfig : sitesList.getSites()) {
            String url = siteConfig.getUrl();
            Site site = siteService.findByUrl(url);
            if (site.getStatus() == SiteStatus.INDEXING) {
                site.setStatus(SiteStatus.FAILED);
            }
            siteService.save(site);
        }
    }
}