package searchengine.services.searching;

import java.util.List;

public interface PageParserService {
    String getTitle(String pageContent);
    String getSnippet(String query, List<String> pageWords);
}
