package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jsoup-connection")
public class JsoupConfig {
    private String userAgent;
    private String referrer;
}
