package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.services.RepositoryServices.LemmaService;
import searchengine.services.RepositoryServices.PageService;
import searchengine.services.RepositoryServices.SiteService;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesListConfig sites;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final SiteService siteService;

    @Override
    public StatisticsResponse getStatistics() {
        if (siteService.count() == 0) {
            return getDefaultStatistics();
        }
        StatisticsData data = new StatisticsData();
        TotalStatistics total = getTotalStatistics();
        List<DetailedStatisticsItem> detailed = getDetailedStatistics();
        data.setTotal(total);
        data.setDetailed(detailed);
        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }


    private TotalStatistics getTotalStatistics() {
        TotalStatistics total = new TotalStatistics();
        int totalPages = (int) pageService.count();
        int totalLemmas = (int) lemmaService.count();
        int totalSites = sites.getSites().size();
        total.setPages(totalPages);
        total.setLemmas(totalLemmas);
        total.setSites(totalSites);
        total.setIndexing(true);
        return total;
    }


    private List<DetailedStatisticsItem> getDetailedStatistics() {
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            SiteConfig siteConfig = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            Site site = siteService.findByUrl(siteConfig.getUrl());
            int siteId = site.getId();
            int pages = pageService.countPagesBySiteId(siteId);
            int lemmas = lemmaService.countBySiteId(siteId);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setName(siteConfig.getName());
            item.setUrl(siteConfig.getUrl());
            item.setStatus(site.getStatus().toString());
            item.setError(site.getLastError());
            Instant instant = site.getStatusTime().atZone(ZoneId.of("Europe/Moscow")).toInstant();
            long millis = instant.toEpochMilli();
            item.setStatusTime(millis);
            detailed.add(item);
        }
        return detailed;
    }


    private StatisticsResponse getDefaultStatistics() {
        StatisticsData data = new StatisticsData();
        TotalStatistics total = new TotalStatistics();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        data.setTotal(total);
        data.setDetailed(detailed);
        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
