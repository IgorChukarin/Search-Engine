package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
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

    private final String pathRegex = "^/[^#]+$";
    private final String imageRegex = "^.*\\.(jpe?g|png|gif|bmp)$";


    @Override
    protected void compute() {
        List<String> nestedLinks = findNestedLinks(currentLink);
        List<LinkFinderAction> taskList = new ArrayList<>();
        for (String nestedLink : nestedLinks) {
            LinkFinderAction task = new LinkFinderAction(site, root, nestedLink, cache, pageRepository, siteRepository);
            task.fork();
            taskList.add(task);
        }
        for (LinkFinderAction task : taskList) {
            task.join();
        }
    }


    public List<String> findNestedLinks(String currentLink) {
        try {
            Thread.sleep(1000);
            Connection.Response response = connectByUrl(currentLink);
            Document document = response.parse();
            Elements elements = document.select("a[href]");
            List<String> nestedLinks = extractLinks(elements);
            saveCurrentPage(document, response);
            updateSiteStatusTime();
            return nestedLinks;
        }
        catch (IOException | InterruptedException e) {
            site.setLastError("Возникла ошибка: " + e.getMessage());
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
            if (link.matches(imageRegex)) {
                continue;
            }
            if (!link.matches(pathRegex)) {
                continue;
            }
            if (cache.add(link)) {
                extractedLinks.add(root.concat(link));
            }
        }
        return extractedLinks;
    }


    private void saveCurrentPage(Document document, Connection.Response response) {
        String path = currentLink.equals(root) ? "/" : currentLink.substring(root.length());
        Page page = new Page();
        page.setPath(path);
        page.setCode(response.statusCode());
        page.setContent(document.toString());
        page.setSite(site);
        pageRepository.save(page);
    }


    private void updateSiteStatusTime() {
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }
}