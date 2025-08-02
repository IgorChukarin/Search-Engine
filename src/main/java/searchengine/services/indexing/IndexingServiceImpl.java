package searchengine.services.indexing;

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
import searchengine.services.repositoryService.PageService;
import searchengine.services.repositoryService.SiteService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@Setter
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SiteService siteService;
    private final PageService pageService;
    private final JsoupConfig jsoupConfig;
    private final SitesListConfig sitesList;
    private final IndexingResultHandler indexingResultHandler;
    private static volatile boolean isIndexing;


    @Override
    public Response startIndexing() {
        if (isIndexing) {
            return new NegativeResponse("Индексация уже запущена");
        }
        isIndexing = true;
        LinkFinderAction.unlockAction();
        try {
            deleteSitesData();
            List<LinkFinderAction> linkFinderActions = prepareSitesForIndexing();
            List<RunnableFuture<String>> runnableFutureTasks = createTasks(linkFinderActions);
            submitTasks(runnableFutureTasks);
            handleIndexingResults(runnableFutureTasks);
        }
        catch (Exception e) {
            return new NegativeResponse("Ошибка при запуске индексации");
        }
        return new PositiveResponse();
    }

    private void deleteSitesData() {
        for (SiteConfig siteConfig : sitesList.getSites()) {
            String url = siteConfig.getUrl();
            siteService.deleteByUrl(url);
        }
    }

    private List<LinkFinderAction> prepareSitesForIndexing() {
        List<LinkFinderAction> actions = new ArrayList<>();
        for (SiteConfig siteConfig : sitesList.getSites()) {
            String url = siteConfig.getUrl();
            Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), siteConfig.getUrl(), siteConfig.getName());
            siteService.save(site);
            LinkFinderAction linkFinderAction = new LinkFinderAction(site, url, siteService, pageService, jsoupConfig);
            actions.add(linkFinderAction);
        }
        return actions;
    }

    private List<RunnableFuture<String>> createTasks(List<LinkFinderAction> linkFinderActions) {
        List<RunnableFuture<String>> runnableFutureTasks = new ArrayList<>();
        for (LinkFinderAction linkFinderAction : linkFinderActions) {
            RunnableFuture<String> runnableFutureTask = createFutureTask(linkFinderAction);
            runnableFutureTasks.add(runnableFutureTask);
        }
        return runnableFutureTasks;
    }

    private RunnableFuture<String> createFutureTask(LinkFinderAction linkFinderAction) {
        RunnableFuture<String> future = new FutureTask<>(new RunnableForkJoin(linkFinderAction), linkFinderAction.getSite().getUrl());
        return future;
    }

    private void submitTasks(List<RunnableFuture<String>> runnableFutureTasks) {
        ExecutorService taskExecutor = Executors.newFixedThreadPool(4);
        runnableFutureTasks.forEach(taskExecutor::execute);
        taskExecutor.shutdown();
    }

    private void handleIndexingResults(List<RunnableFuture<String>> runnableFutureTasks) {
        ExecutorService monitoringExecutor = Executors.newSingleThreadExecutor();
        indexingResultHandler.setRunnableFutureList(runnableFutureTasks);
        monitoringExecutor.execute(indexingResultHandler);
        monitoringExecutor.shutdown();
    }


    @Override
    public Response stopIndexing() {
        if (!isIndexing) {
            return new NegativeResponse("Индексация не запущена");
        }
        isIndexing = false;
        LinkFinderAction.lockAction();
        changeUnfinishedSiteStatus();
        return new PositiveResponse();
    }

    private void changeUnfinishedSiteStatus() {
        for (SiteConfig siteConfig : sitesList.getSites()) {
            String url = siteConfig.getUrl();
            Site site = siteService.findByUrl(url);
            site.setLastError("Индексация остановлена пользователем");
            if (site.getStatus() == SiteStatus.INDEXING) {
                site.setStatus(SiteStatus.FAILED);
            }
            siteService.save(site);
        }
    }


    @Override
    public boolean isIndexing() {
        return isIndexing;
    }
}