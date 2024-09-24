package searchengine.dto.indexing;

import lombok.*;

@Getter
@Setter
public class NegativeResponse extends Response {
    private String error;

    public NegativeResponse(String error) {
        super(false);
        this.error = error;
    }
}
