package searchengine.services.Search;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.Response;
import searchengine.services.lemmaProcessingClasses.LemmaProcessorService;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService{

    private final LemmaProcessorService lemmaProcessorService;

    public SearchServiceImpl(LemmaProcessorService lemmaProcessorService) {
        this.lemmaProcessorService = lemmaProcessorService;
    }

    @Override
    public Response search(String query, String site, int offset, int limit) {
        List<String> words = lemmaProcessorService.extractRussianWords(query);
        List<String> lemmas = new ArrayList<>();
        for (String word : words) {
            List<String> wordLemmas = lemmaProcessorService.findBaseForms(word);
            lemmas.addAll(wordLemmas);
        }
        
        return null;
    }
}
