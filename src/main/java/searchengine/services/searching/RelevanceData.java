package searchengine.services.searching;

import searchengine.model.Page;

import java.util.Map;

public class RelevanceData {
    private final Map<Page, Float> pageRelevance;
    private final float maxRelevance;

    public RelevanceData(Map<Page, Float> pageRelevance, float maxRelevance) {
        this.pageRelevance = pageRelevance;
        this.maxRelevance = maxRelevance;
    }

    public Map<Page, Float> getPageRelevance() {
        return pageRelevance;
    }

    public float getMaxRelevance() {
        return maxRelevance;
    }
}
