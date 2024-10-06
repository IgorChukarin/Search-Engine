package searchengine.services.Search;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.Response;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.repositories.SearchIndexRepository;
import searchengine.services.RepositoryServices.LemmaService;
import searchengine.services.RepositoryServices.SearchIndexService;
import searchengine.services.lemmaProcessingClasses.LemmaProcessorService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

@Service
public class SearchServiceImpl implements SearchService{

    private final LemmaProcessorService lemmaProcessorService;
    private final LemmaService lemmaService;
    private final SearchIndexService searchIndexService;
    private final SearchIndexRepository searchIndexRepository;

    public SearchServiceImpl(LemmaProcessorService lemmaProcessorService, LemmaService lemmaService, SearchIndexService searchIndexService, SearchIndexRepository searchIndexRepository) {
        this.lemmaProcessorService = lemmaProcessorService;
        this.lemmaService = lemmaService;
        this.searchIndexService = searchIndexService;
        this.searchIndexRepository = searchIndexRepository;
    }

    @Override
    public Response search(String query, String site, int offset, int limit) {
        List<String> words = lemmaProcessorService.extractRussianWords(query);
        List<String> lemmas = translateWordsIntoLemmas(words);
        List<Lemma> storedLemmas = matchStoredLemmas(lemmas);

        if (storedLemmas.isEmpty()) {
            return null;
        }

        TreeMap<Integer, Lemma> lemmasRating = rateLemmasByFrequency(storedLemmas);
        Lemma firstLemma = lemmasRating.firstEntry().getValue();
        List<SearchIndex> searchIndices = searchIndexRepository.findAllByLemma(firstLemma);
        lemmasRating.pollFirstEntry();

        for (Integer key : lemmasRating.keySet()) {
            Lemma lemma = lemmasRating.get(key);
            Iterator<SearchIndex> searchIndexIterator = searchIndices.iterator();
            while(searchIndexIterator.hasNext()) {
                Page page = searchIndexIterator.next().getPage();
                if (!searchIndexRepository.existsByLemmaAndPage(lemma, page)) {
                    searchIndexIterator.remove();
                }
            }
        }

        for (SearchIndex searchIndex : searchIndices) {
            System.out.println(searchIndex.getPage().getId());
        }
        return null;
    }


    public List<String> translateWordsIntoLemmas(List<String> words) {
        List<String> lemmas = new ArrayList<>();
        for (String word : words) {
            List<String> wordLemmas = lemmaProcessorService.findBaseForms(word);
            lemmas.addAll(wordLemmas);
        }
        return lemmas;
    }


    public List<Lemma> matchStoredLemmas(List<String> lemmas) {
        List<Lemma> storedLemmas = new ArrayList<>();
        for (String lemmaWord : lemmas) {
            List<Lemma> foundLemmas = lemmaService.findAllByLemma(lemmaWord);
            storedLemmas.addAll(foundLemmas);
        }
        return storedLemmas;
    }


    public TreeMap<Integer, Lemma> rateLemmasByFrequency(List<Lemma> storedLemmas) {
        TreeMap<Integer, Lemma> lemmaRating = new TreeMap<>();
        for (Lemma lemmaObject : storedLemmas) {
            Integer frequency = lemmaObject.getFrequency();
            lemmaRating.put(frequency, lemmaObject);
        }
        return lemmaRating;
    }
}
