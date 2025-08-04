package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PositiveSearchResponse extends PositiveResponse {
    private int count;
    private List<SearchData> data;

    public PositiveSearchResponse(int count, List<SearchData> data) {
        this.count = count;
        this.data = data;
    }
}
