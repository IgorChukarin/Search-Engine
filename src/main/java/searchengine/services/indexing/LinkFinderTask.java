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
import searchengine.services.repositoryService.PageService;
import searchengine.services.repositoryService.SiteService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

    private final int indexingDelay = 1000;

    private final LinkFilter linkFilter = new DefaultLinkFilter();


    @Override
    protected String compute() {
        if (isLocked) {
            return site.getUrl() + " indexationStopped";
        }
        updateSiteStatusTime();
        try {
            Thread.sleep(indexingDelay);
            Connection.Response response = connectByUrl(currentLink);
            Document document = response.parse();
            boolean linkIsSaved = saveCurrentLinkIfNotExists(response, document);
            if (!linkIsSaved) return site.getUrl() + " linkExists";
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
            return site.getUrl() + " indexationFailed";
        }
        return isLocked ? site.getUrl() + " indexationStopped" : site.getUrl() + " indexationSucceed";
    }


    private Connection.Response connectByUrl(String url) throws IOException {
        try {
            return Jsoup.connect(url)
                    .userAgent(jsoupConfig.getUserAgent())
                    .referrer(jsoupConfig.getReferrer())
                    .execute();
        } catch (IOException e) {
            System.out.println("Failed to connect to " + url + ": " + e.getMessage());
            throw e;
        }
    }


    private boolean saveCurrentLinkIfNotExists(Connection.Response response, Document document) {
        String path = normalizePath(currentLink);
        Integer code = response.statusCode();
        String content = document.toString();
        Page page = new Page();
        page.setPath(path);
        page.setCode(code);
        page.setContent(content);
        page.setSite(site);
        synchronized (pageService) {
            return pageService.saveIfNotExist(page);
        }
    }


    private String normalizePath(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();

            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }

            return path.isEmpty() ? "/" : path;
        } catch (URISyntaxException e) {
            return "/";
        }
    }


    private void saveUnreachablePages(String url, int statusCode) {
        String path = url.equals(site.getUrl()) ? "/" : url.substring(site.getUrl().length());
        Page page = new Page();
        page.setPath(path);
        page.setCode(statusCode);
        page.setSite(site);
        synchronized (pageService) {
            pageService.saveIfNotExist(page);
        }
    }


    public List<String> findNestedLinks(Document document) {
        Elements elements = document.select("a[href]");
        List<String> nestedLinks = new ArrayList<>();
        for (Element element : elements) {
            String link = element.attr("href");

            if (linkFilter.shouldFilter(link, site)) {
                continue;
            }

            String normalizedLink = normalizeLink(currentLink, link);
            if (normalizedLink == null || pageService.existsByPathAndSiteId(link, site.getId())) {
                continue;
            }

            nestedLinks.add(normalizedLink);
        }
        return nestedLinks;
    }


    private String normalizeLink(String currentLink, String link) {
        try {
            URI base = new URI(currentLink);
            URI resolved = base.resolve(link);
            return resolved.toString();
        } catch (URISyntaxException e) {
            return null;
        }
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