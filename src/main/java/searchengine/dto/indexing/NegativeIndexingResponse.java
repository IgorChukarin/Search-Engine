package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class NegativeIndexingResponse extends IndexingResponse{
    private String error;

    public NegativeIndexingResponse(String error) {
        super(false);
        this.error = error;
    }
}
