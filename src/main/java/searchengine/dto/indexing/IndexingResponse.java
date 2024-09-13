package searchengine.dto.indexing;

import lombok.Data;

@Data
public abstract class IndexingResponse {
    private final boolean result;

    public IndexingResponse(boolean result) {
        this.result = result;
    }
}
