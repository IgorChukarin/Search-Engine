package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SitesListConfig sitesList;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    private final SiteRepository siteRepository;

    @Override
    public IndexingResponse startIndexing() {
        List<LinkFinderTask> tasks = new ArrayList<>();
        for (SiteConfig site : sitesList.getSites()) {
            String url = site.getUrl();
            System.out.println(siteRepository.deleteByUrl(url));
            Integer depth = 0;
            Set<String> cache = new HashSet<>();
            LinkFinderTask task = new LinkFinderTask(url, url, depth, cache);
            tasks.add(task);
        }
        IndexingResponse indexingResponse = new IndexingResponse();
        indexingResponse.setResult(true);
        return indexingResponse;
    }
}
