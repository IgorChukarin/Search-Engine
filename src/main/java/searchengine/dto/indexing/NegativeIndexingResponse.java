package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NegativeIndexingResponse {
    private boolean result;
    private String error;
}
