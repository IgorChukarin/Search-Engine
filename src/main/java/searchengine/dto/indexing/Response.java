package searchengine.dto.indexing;

import lombok.Data;

@Data
public abstract class Response {
    private final boolean result;

    public Response(boolean result) {
        this.result = result;
    }
}
