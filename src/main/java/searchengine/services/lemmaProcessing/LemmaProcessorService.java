package searchengine.services.lemmaProcessing;

import searchengine.dto.indexing.Response;
import searchengine.model.Page;

import java.util.List;

public interface LemmaProcessorService {
    Response indexPage(String url);
    void indexPage(Page page);
    List<String> getRussianWords(String text);
    List<String> findBaseForms(String word);
}
