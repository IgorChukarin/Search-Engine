package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.JsoupConfig;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.services.repositoryService.PageService;
import searchengine.services.repositoryService.SiteService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public class LinkFinderTask extends RecursiveTask<String> {

    private static volatile boolean isLocked;

    private Site site;
    private String currentLink;

    private final SiteService siteService;
    private final PageService pageService;
    private final JsoupConfig jsoupConfig;

    private final String pathRegex = "^/[^#]+$";
    private final String imageRegex = "^.*\\.(jpe?g|png|gif|bmp)$";
    private final int indexingDelay = 1000;

    private static final List<Page> PageBuffer = new ArrayList<>();


    @Override
    protected String compute() {
        if (isLocked) {
            return site.getUrl() + " indexationStopped";
        }
        try {
            Thread.sleep(indexingDelay);
            updateSiteStatusTime();
            Connection.Response response = connectByUrl(currentLink);
            Document document = response.parse();
            saveCurrentLinkIfNotExists(response, document);
            List<String> nestedLinks = findNestedLinks(document);
            List<LinkFinderTask> actionList = new ArrayList<>();
            for (String nestedLink : nestedLinks) {
                LinkFinderTask action = new LinkFinderTask(site, nestedLink, siteService, pageService, jsoupConfig);
                actionList.add(action);
                action.fork();
            }
            for (LinkFinderTask action : actionList) {
                action.join();
            }
        } catch (IOException | InterruptedException e) {
            String exception = e.getClass().toString();
            System.out.println(e.getMessage());
            if (exception.contains("UnknownHostException")) {
                site.setLastError("Не удалось подключиться к сайту");
                siteService.save(site);
                return site.getUrl() + " indexationFailed";
            }
        }
        return isLocked ? site.getUrl() + " indexationStopped" : site.getUrl() + " indexationSucceed";
    }


    private Connection.Response connectByUrl(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(jsoupConfig.getUserAgent())
                .referrer(jsoupConfig.getReferrer())
                .execute();
    }


    private void saveCurrentLinkIfNotExists(Connection.Response response, Document document) {
        String root = site.getUrl();
        String path = currentLink.equals(root) ? "/" : currentLink.substring(root.length());
        Integer code = response.statusCode();
        String content = document.toString();
        Page page = new Page();
        page.setPath(path);
        page.setCode(code);
        page.setContent(content);
        page.setSite(site);
        synchronized (pageService) {
            pageService.saveIfNotExist(page);
        }
    }


    public List<String> findNestedLinks(Document document) {
        Elements elements = document.select("a[href]");
        List<String> nestedLinks = new ArrayList<>();
        String root = site.getUrl();
        for (Element element : elements) {
            String link = element.attr("href");
            if (shouldFilterLink(link)) {
                continue;
            }
            if (!pageService.existsByPathAndSiteId(link, site.getId())) {
                nestedLinks.add(root.concat(link));
            }
        }
        return nestedLinks;
    }


    private boolean shouldFilterLink(String link) {
        return link.matches(imageRegex) || !link.matches(pathRegex) || link.endsWith(".pdf")
                || link.endsWith(".webp");
    }


    private void updateSiteStatusTime() {
        site.setStatusTime(LocalDateTime.now());
        siteService.save(site);
    }


    public static void lockAction() {
        isLocked = true;
    }


    public static void unlockAction() {
        isLocked = false;
    }
}