package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchData> data;

    public SearchResponse(int count, List<SearchData> data) {
        this.result = true;
        this.count = count;
        this.data = data;
    }
}
