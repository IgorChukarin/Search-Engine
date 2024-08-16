package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;
import searchengine.services.linkFinderClasses.ControlThread;
import searchengine.services.linkFinderClasses.LinkFinderAction;
import searchengine.services.linkFinderClasses.RunnableForkJoin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{
    private final SitesListConfig sitesList;
    private final SiteService siteService;
    private final PageService pageService;
    private final JsoupConfig jsoupConfig;
    private volatile boolean isIndexing = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final IndexingResultHandler indexingResultHandler;

    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();
        if (isIndexing) {
            return new IndexingResponse(false, "Индексация уже запущена");
        } else {
            isIndexing = true;
        }
        try {
            List<LinkFinderAction> actions = new ArrayList<>();
            for (SiteConfig siteConfig : sitesList.getSites()) {
                String url = siteConfig.getUrl();
                siteService.deleteByUrl(url);
                Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), siteConfig.getUrl(), siteConfig.getName());
                siteService.save(site);
                LinkFinderAction linkFinderAction = new LinkFinderAction(new AtomicBoolean(false), site, url, url, siteService, pageService, jsoupConfig);
                actions.add(linkFinderAction);
            }

            List<RunnableFuture<String>> futureTasks = new ArrayList<>();
            for (LinkFinderAction action : actions) {
                RunnableFuture<String> runnableFutureTask = createFutureTask(action);
                futureTasks.add(runnableFutureTask);
            }


            //futureTasks.forEach(executor::execute); // future task можно превратить в thread

            RunnableFuture<String> futureTask = futureTasks.get(0);
            ControlThread controlThread = new ControlThread(1000, futureTask);
            controlThread.start();


            indexingResultHandler.setRunnableFutureList(futureTasks);
            executor.execute(indexingResultHandler);
            executor.shutdown();

            indexingResponse.setResult(true);
        }
        catch (Exception e) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Ошибка в indexingService");
            e.printStackTrace();
        }
        return indexingResponse;
    }



    @Override
    public IndexingResponse stopIndexing() {

        return new IndexingResponse();
    }

    private RunnableFuture<String> createFutureTask(LinkFinderAction linkFinderAction) {
        RunnableFuture<String> future = new FutureTask<>(new RunnableForkJoin(linkFinderAction), linkFinderAction.getSite().getUrl());
        return future;
    }
}