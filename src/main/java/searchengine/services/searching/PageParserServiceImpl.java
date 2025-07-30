package searchengine.services.searching;

import org.springframework.stereotype.Service;

@Service
public class PageParserServiceImpl implements PageParserService {

    @Override
    public String getTitle(String pageContent) {
        int titleStart = pageContent.indexOf("<title>");
        if (titleStart != -1) {
            int titleEnd = pageContent.indexOf("</title>", titleStart);
            if (titleEnd != -1) {
                return pageContent.substring(titleStart + 7, titleEnd).trim();
            }
        }
        return null;
    }

    @Override
    public String getSnippet(String pageContent) {
        return null;
    }
}
