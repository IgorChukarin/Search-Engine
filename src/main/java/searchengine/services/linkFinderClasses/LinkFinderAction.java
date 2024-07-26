package searchengine.services.linkFinderClasses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.PageService;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public class LinkFinderAction extends RecursiveAction {
    private Site site;
    private String root;
    private String currentLink;
    private Set<String> cache;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    private final PageService pageService;

    private final String pathRegex = "^/[^#]+$";
    private final String imageRegex = "^.*\\.(jpe?g|png|gif|bmp)$";
    

    @Override
    protected void compute() {
        List<String> nestedLinks = findNestedLinks(currentLink);
        List<LinkFinderAction> actionList = new ArrayList<>();
        for (String nestedLink : nestedLinks) {
            LinkFinderAction action = new LinkFinderAction(site, root, nestedLink, cache, pageRepository, siteRepository, pageService);
            action.fork();
            actionList.add(action);
        }
        for (LinkFinderAction action : actionList) {
            action.join();
        }
    }


    public List<String> findNestedLinks(String currentLink) {
        try {
            Thread.sleep(1000);
            Connection.Response response = connectByUrl(currentLink);
            Document document = response.parse();

            String path = currentLink.equals(root) ? "/" : currentLink.substring(root.length());
            Integer code = response.statusCode();
            String content = document.toString();
            pageService.save(path, code, content, site);

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
            siteRepository.save(site);
            throw new RuntimeException(e);
        }
    }


    private Connection.Response connectByUrl(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("ChukarinSearchBot")
                .referrer("http://www.google.com")
                .execute();
    }


    private ArrayList<String> extractLinks(Elements elements) {
        ArrayList<String> extractedLinks = new ArrayList<>();
        for (Element element : elements) {
            String link = element.attr("href");
            if (link.matches(imageRegex) || !link.matches(pathRegex)) {
                continue;
            }
            if (cache.add(link)) {
                extractedLinks.add(root.concat(link));
            }
        }
        return extractedLinks;
    }

    private void updateSiteStatusTime() {
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }
}