package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class LinkFinderTask extends RecursiveTask<String> {
    private String root;
    private String link;
    private int depth;
    private Set<String> cache;

    public LinkFinderTask(String root, String link, int depth, Set<String> cache) {
        this.root = root;
        this.link = link;
        this.depth = depth;
        this.cache = cache;
    }

    @Override
    protected String compute() {
        System.out.println("\t".repeat(depth) + link);
        String linkTree = "\t".repeat(depth) + link + "\n";
        Set<String> subLinks = findNestedLinks(link);
        depth += 1;
        List<LinkFinderTask> taskList = new ArrayList<>();
        for (String subLink : subLinks) {
            if (cache.add(subLink)) {
                LinkFinderTask task = new LinkFinderTask(root, subLink, depth, cache);
                task.fork();
                taskList.add(task);
            }
        }
        depth = 0;
        for (LinkFinderTask task : taskList) {
            linkTree += task.join();
        }
        return linkTree;
    }

    public Set<String> findNestedLinks(String url) {
        String fullLinkRegex = url + "/[^#]+";
        String linkRegex = "/[^#]+";
        String imageRegex = ".*\\.(jpeg|jpg|png|gif|bmp)";
        Set<String> nestedLinks = new LinkedHashSet<>();
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("ChukarinSearchBot")
                    .referrer("http://www.google.com")
                    .get();
            Thread.sleep(2000);
            Elements links = document.select("a[href]");
            for (Element link : links) {
                String href = link.attr("href");
                if (href.matches(imageRegex)) {
                    continue;
                } else if (href.matches(fullLinkRegex)) {
                    nestedLinks.add(href);
                } else if (href.matches(linkRegex)) {
                    nestedLinks.add(root + href);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return nestedLinks;
    }
}