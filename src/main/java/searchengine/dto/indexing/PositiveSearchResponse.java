package searchengine.dto.indexing;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.searching.SearchDataDto;

import java.util.List;

@Getter
@Setter
public class PositiveSearchResponse extends PositiveResponse {
    private int count;
    private List<SearchDataDto> data;

    public PositiveSearchResponse(int count, List<SearchDataDto> data) {
        this.count = count;
        this.data = data;
    }
}
