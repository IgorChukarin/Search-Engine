package searchengine.services.searching;

public interface PageParserService {
    String getTitle(String pageContent);
    String getSnippet(String pageContent);
}
