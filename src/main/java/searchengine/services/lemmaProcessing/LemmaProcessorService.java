package searchengine.services.lemmaProcessing;

import searchengine.dto.indexing.Response;

import java.util.List;

public interface LemmaProcessorService {
    Response IndexPage(String url);
    List<String> getRussianWords(String text);
    List<String> findBaseForms(String word);
}
