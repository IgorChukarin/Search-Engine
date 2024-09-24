package searchengine.dto.indexing;

import java.util.List;

public class SearchResponse extends Response {
    private int count;
    private List<SearchData> data;

    public SearchResponse(int count, List<SearchData> data) {
        super(true);
        this.count = count;
        this.data = data;
    }
}
