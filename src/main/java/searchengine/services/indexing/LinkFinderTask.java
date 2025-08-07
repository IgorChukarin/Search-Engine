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
        try {
            return Jsoup.connect(url)
                    .userAgent(jsoupConfig.getUserAgent())
                    .referrer(jsoupConfig.getReferrer())
                    .execute();
        } catch (org.jsoup.HttpStatusException e) {
            saveUnreachablePages(url, e.getStatusCode());
            throw e;
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


    private void saveCurrentLinkIfNotExists(Connection.Response response, Document document) {
        String rootUrl = site.getUrl();
        String path = currentLink.equals(rootUrl) ? "/" : currentLink.substring(rootUrl.length());
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
            if (!shouldFilterLink(link) && !pageService.existsByPathAndSiteId(link, site.getId())) {
                System.out.println("Link: " + link);
                System.out.println("Full link: " + root.concat(link));
                nestedLinks.add(root.concat(link));
            }
        }
        return nestedLinks;
    }


    private boolean shouldFilterLink(String link) {
        String lowerCaseLink = link.toLowerCase();
        return isAnchorOrScript(lowerCaseLink) || isTelOrMail(lowerCaseLink) || isImage(lowerCaseLink)
                || isFile(lowerCaseLink) || isAnotherSite(link) || hasExternalRedirect(lowerCaseLink);
    }

    private boolean isAnchorOrScript(String link) {
        return link.contains("#") || link.startsWith("javascript:");
    }

    private boolean isTelOrMail(String link) {
        return link.startsWith("tel:") || link.startsWith("mailto:");
    }

    private boolean isImage(String link) {
        return link.endsWith(".jpg") || link.endsWith(".jpeg") || link.endsWith(".png")
                || link.endsWith(".gif") || link.endsWith(".bmp") || link.endsWith(".svg")
                || link.endsWith(".webp") || link.endsWith(".tiff") || link.endsWith(".ico");
    }

    private boolean isFile(String link) {
        return link.endsWith(".pdf") || link.endsWith(".doc") || link.endsWith(".docx")
                || link.endsWith(".xls") || link.endsWith(".xlsx") || link.endsWith(".zip")
                || link.endsWith(".rar") || link.endsWith(".7z") || link.endsWith(".tar")
                || link.endsWith(".gz") || link.endsWith(".exe") || link.endsWith(".mp3")
                || link.endsWith(".mp4") || link.endsWith(".avi") || link.endsWith(".mov");
    }

    private boolean isAnotherSite(String link) {
        try {
            URI linkUri = new URI(link);
            URI rootUri = new URI(site.getUrl());
            String linkHost = linkUri.getHost();
            String rootHost = rootUri.getHost();
            if (linkHost == null) {
                return false;
            }
            return !linkHost.equalsIgnoreCase(rootHost);

        } catch (URISyntaxException e) {
            return true;
        }
    }

    private boolean hasExternalRedirect(String link) {
        try {
            URI uri = new URI(link);
            String query = uri.getQuery();
            if (query != null && (query.contains("http://") || query.contains("https://"))) {
                return true;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        return false;
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