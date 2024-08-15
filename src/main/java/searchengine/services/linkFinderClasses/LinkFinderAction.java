package searchengine.services.linkFinderClasses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import searchengine.config.JsoupConfig;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public class LinkFinderAction extends RecursiveAction {
    private AtomicBoolean stopAction;

    private Site site;
    private String root;
    private String currentLink;

    private final SiteService siteService;
    private final PageService pageService;
    private final JsoupConfig jsoupConfig;

    private final String pathRegex = "^/[^#]+$";
    private final String imageRegex = "^.*\\.(jpe?g|png|gif|bmp)$";
    

    @Override
    protected void compute() {
        System.out.println(currentLink + " " + stopAction);
        if (stopAction.get()) {
            return;
        }
        List<String> nestedLinks = findNestedLinks(currentLink);
        List<LinkFinderAction> actionList = new ArrayList<>();
        for (String nestedLink : nestedLinks) {
            if (stopAction.get()) {
                break;
            }
            LinkFinderAction action = new LinkFinderAction(new AtomicBoolean(false), site, root, nestedLink, siteService, pageService, jsoupConfig);
            action.fork();
            actionList.add(action);
        }
        for (LinkFinderAction action : actionList) {
            action.join();
        }
    }


    public List<String> findNestedLinks(String currentLink) {
        if (stopAction.get()) {
            return new ArrayList<>();
        }
        try {
            Thread.sleep(1000);
            Connection.Response response = connectByUrl(currentLink);
            Document document = response.parse();

            String path = currentLink.equals(root) ? "/" : currentLink.substring(root.length());
            Integer code = response.statusCode();
            String content = document.toString();

            synchronized (pageService) {
                if (stopAction.get()) {
                    return new ArrayList<>();
                }
                pageService.save(path, code, content, site);
            }

            Elements elements = document.select("a[href]");
            List<String> nestedLinks = extractLinks(elements);
            updateSiteStatusTime();
            return nestedLinks;
        }
        catch (IOException | InterruptedException e) {
            String exception = e.getClass().toString();
            System.out.println(exception);
            if (exception.contains("UnknownHostException")) {
                site.setLastError("Не удалось подключиться к сайту");
            }
            site.setStatus(SiteStatus.FAILED);
            siteService.save(site);
            throw new RuntimeException(e);
        }
    }


    private Connection.Response connectByUrl(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(jsoupConfig.getUserAgent())
                .referrer(jsoupConfig.getReferrer())
                .execute();
    }


    private ArrayList<String> extractLinks(Elements elements) {
        ArrayList<String> extractedLinks = new ArrayList<>();
        for (Element element : elements) {
            String link = element.attr("href");
            if (link.matches(imageRegex) || !link.matches(pathRegex)) {
                continue;
            }
            if (!pageService.existsByPathAndSiteId(link, site.getId())) {
                extractedLinks.add(root.concat(link));
            }
        }
        return extractedLinks;
    }

    private void updateSiteStatusTime() {
        site.setStatusTime(LocalDateTime.now());
        siteService.save(site);
    }
}