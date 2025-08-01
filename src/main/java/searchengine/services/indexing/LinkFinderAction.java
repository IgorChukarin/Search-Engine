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
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.services.repositoryService.PageService;
import searchengine.services.repositoryService.SiteService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public class LinkFinderAction extends RecursiveAction {

    private static volatile boolean isLocked;

    private Site site;
    private String root;
    private String currentLink;

    private final SiteService siteService;
    private final PageService pageService;
    private final JsoupConfig jsoupConfig;

    private final String pathRegex = "^/[^#]+$";
    private final String imageRegex = "^.*\\.(jpe?g|png|gif|bmp)$";
    private final int indexingDelay = 1000;
    

    @Override
    protected void compute() {
        if (isLocked) {
            return;
        }
        try {
            Thread.sleep(indexingDelay);
            Connection.Response response = connectByUrl(currentLink);
            Document document = response.parse();
            saveCurrentLinkIfNotExists(response, document);
            List<String> nestedLinks = findNestedLinks(document);
            List<LinkFinderAction> actionList = new ArrayList<>();
            for (String nestedLink : nestedLinks) {
                LinkFinderAction action = new LinkFinderAction(site, root, nestedLink, siteService, pageService, jsoupConfig);
                actionList.add(action);
                action.fork();
            }
            for (LinkFinderAction action : actionList) {
                action.join();
            }
            updateSiteStatusTime();
        } catch (IOException | InterruptedException e) {
            String exception = e.getClass().toString();
            System.out.println(exception);
            if (exception.contains("UnknownHostException")) {
                site.setLastError("Не удалось подключиться к сайту");
            }
            site.setStatus(SiteStatus.FAILED);
            siteService.save(site);
        } finally {
            updateSiteStatusTime();
        }
    }

    private Connection.Response connectByUrl(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(jsoupConfig.getUserAgent())
                .referrer(jsoupConfig.getReferrer())
                .execute();
    }

    private void saveCurrentLinkIfNotExists(Connection.Response response, Document document) {
        String path = currentLink.equals(root) ? "/" : currentLink.substring(root.length());
        Integer code = response.statusCode();
        String content = document.toString();
        synchronized (pageService) {
            pageService.saveIfNotExist(path, code, content, site);
        }
    }

    public List<String> findNestedLinks(Document document) {
        Elements elements = document.select("a[href]");
        List<String> nestedLinks = new ArrayList<>();
        for (Element element : elements) {
            String link = element.attr("href");
            if (link.matches(imageRegex) || !link.matches(pathRegex)) {
                continue;
            }
            if (!pageService.existsByPathAndSiteId(link, site.getId())) {
                nestedLinks.add(root.concat(link));
            }
        }
        return nestedLinks;
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