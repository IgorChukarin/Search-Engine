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
public class LinkFinderTask extends RecursiveAction {
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
        List<LinkFinderTask> taskList = new ArrayList<>();
        for (String nestedLink : nestedLinks) {
            LinkFinderTask task = new LinkFinderTask(site, root, nestedLink, cache, pageRepository, siteRepository);
            task.fork();
            taskList.add(task);
        }
        for (LinkFinderTask task : taskList) {
            task.join();
        }
    }

    public List<String> findNestedLinks(String currentLink) {
        String fullLinkRegex = currentLink + pathRegex;
        List<String> nestedLinks = new ArrayList<>();
        try {
            Thread.sleep(2000);
            Connection.Response response = Jsoup.connect(currentLink)
                    .userAgent("ChukarinSearchBot")
                    .referrer("http://www.google.com")
                    .execute();
            Document document = response.parse();

            String path = currentLink == root ? "/" : currentLink.substring(root.length());
            Page page = new Page();
            page.setPath(path);
            page.setCode(response.statusCode());
            page.setContent(document.toString());
            page.setSite(site);
            pageRepository.save(page);

            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            Elements elements = document.select("a[href]");
            for (Element element : elements) {
                String link = element.attr("href");
                if (!cache.add(link)) {
                    continue;
                }
                if (link.matches(imageRegex)) {
                    continue;
                }
                if (link.matches(fullLinkRegex)) {
                    nestedLinks.add(link);
                }
                else if (link.matches(pathRegex)) {
                    nestedLinks.add(root + link);
                }
            }
        } catch (IOException | InterruptedException e) {
            site.setLastError("Возникла ошибка: " + e.getMessage());
            siteRepository.save(site);
            throw new RuntimeException(e);
        }
        return nestedLinks;
    }
}