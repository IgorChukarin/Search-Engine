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
        LinkFinderTask.unlockAction();
        try {
            deleteSitesData();
            List<LinkFinderTask> linkFinderTasks = prepareSitesForIndexing();
            invokeTasks(linkFinderTasks);
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
        });
    }


    private void setSiteStatus(String message) {
        System.out.println("Indexing result: " + message);
        String url = message.split(" ")[0];
        Site site = siteService.findByUrl(url);
        String status = message.split(" ")[1];
        if (status.equals("indexationSucceed")) {
            site.setStatus(SiteStatus.INDEXED);
        } else {
            site.setStatus(SiteStatus.FAILED);
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