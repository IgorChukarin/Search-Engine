package searchengine.services.indexing;

import searchengine.model.Site;

public interface LinkFilter {
    boolean shouldFilter(String link, Site site);
}
