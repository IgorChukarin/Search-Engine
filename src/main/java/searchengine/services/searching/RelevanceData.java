package searchengine.services.searching;

import lombok.Getter;
import searchengine.model.Page;

import java.util.Map;

@Getter
public class RelevanceData {
    private final Map<Page, Float> pageRelevance;
    private final float maxRelevance;

    public RelevanceData(Map<Page, Float> pageRelevance, float maxRelevance) {
        this.pageRelevance = pageRelevance;
        this.maxRelevance = maxRelevance;
    }

}
