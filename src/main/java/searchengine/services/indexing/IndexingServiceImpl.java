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
    private static volatile boolean isIndexing;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(4);


    @Override
    public Response startIndexing() {
        if (isIndexing) {
            return new NegativeResponse("Индексация уже запущена");
        }
        isIndexing = true;
        deleteSitesData();
        LinkFinderTask.unlockAction();
        List<LinkFinderTask> linkFinderTasks = prepareSitesForIndexing();
        invokeTasks(linkFinderTasks);
        return new PositiveResponse();
    }


    private void deleteSitesData() {
        for (SiteConfig siteConfig : sitesList.getSites()) {
            String url = siteConfig.getUrl();
            siteService.deleteByUrl(url);
        }
    }


    private List<LinkFinderTask> prepareSitesForIndexing() {
        List<LinkFinderTask> linkFinderTasks = new ArrayList<>();
        for (SiteConfig siteConfig : sitesList.getSites()) {
            String url = siteConfig.getUrl();
            Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), siteConfig.getUrl(), siteConfig.getName());
            siteService.save(site);
            LinkFinderTask linkFinderTask = new LinkFinderTask(site, url, siteService, pageService, jsoupConfig);
            linkFinderTasks.add(linkFinderTask);
        }
        return linkFinderTasks;
    }


    private void invokeTasks(List<LinkFinderTask> linkFinderTasks) {
        forkJoinPool.submit(() -> {
            for (LinkFinderTask action : linkFinderTasks) {
                action.fork();
            }
            for (LinkFinderTask action : linkFinderTasks) {
                String result = action.join();
                setSiteStatus(result);
            }
            isIndexing = false;
            LinkFinderTask.lockAction();
        });
    }


    private void setSiteStatus(String message) {
        String[] parts = message.split(" ");
        String url = parts[0];
        String status = parts[1];
        Site site = siteService.findByUrl(url);
        System.out.println("Site: " + site.getUrl() + ", Status: " + status);
        switch (status) {
            case "indexationSucceed" -> site.setStatus(SiteStatus.INDEXED);
            case "indexationStopped" -> {
                site.setLastError("Индексация остановлена пользователем");
                site.setStatus(SiteStatus.FAILED);
            }
            case "indexationFailed" -> {
                site.setLastError("Не удалось подключиться к сайту");
                site.setStatus(SiteStatus.FAILED);
            }
        }
        siteService.save(site);
    }


    @Override
    public Response stopIndexing() {
        if (!isIndexing) {
            return new NegativeResponse("Индексация не запущена");
        }
        isIndexing = false;
        LinkFinderTask.lockAction();
        return new PositiveResponse();
    }


    @Override
    public boolean isIndexing() {
        return isIndexing;
    }
}