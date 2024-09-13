package searchengine.dto.indexing;

import lombok.*;

@Getter
@Setter
public class NegativeIndexingResponse extends IndexingResponse{
    private String error;

    public NegativeIndexingResponse(String error) {
        super(false);
        this.error = error;
    }
}
