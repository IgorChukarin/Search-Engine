package searchengine.services.Search;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.Response;
import searchengine.dto.indexing.SearchData;
import searchengine.dto.indexing.SearchResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.repositories.SearchIndexRepository;
import searchengine.services.RepositoryServices.LemmaService;
import searchengine.services.RepositoryServices.SearchIndexService;
import searchengine.services.lemmaProcessingClasses.LemmaProcessorService;

import java.util.*;

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
    public SearchResponse search(String query, String site, int offset, int limit) {
        List<String> words = lemmaProcessorService.extractRussianWords(query);
        List<String> lemmas = translateWordsIntoLemmas(words);
        List<Lemma> matchedLemmas = matchLemmas(lemmas);
        if (matchedLemmas.isEmpty() || lemmas.size() != matchedLemmas.size()) return null;
        matchedLemmas.sort(Comparator.comparingInt(Lemma::getFrequency));
        Lemma firstLemma = matchedLemmas.get(0);
        List<SearchIndex> searchIndices = searchIndexRepository.findAllByLemma(firstLemma);
        for (Lemma lemma : matchedLemmas) {
            Iterator<SearchIndex> searchIndexIterator = searchIndices.iterator();
            while (searchIndexIterator.hasNext()) {
                Page page = searchIndexIterator.next().getPage();
                if (!searchIndexRepository.existsByLemmaAndPage(lemma, page)) {
                    searchIndexIterator.remove();
                }
            }
        }

        Map<Page, Float> pageRelevance = new HashMap<>();
        float maxRelevance = 0.0F;
        for (SearchIndex searchIndex : searchIndices) {
            Page page = searchIndex.getPage();
            float relevance = 0.0F;
            for (Lemma lemma : matchedLemmas) {
                Optional<SearchIndex> optionalSearchIndex = searchIndexRepository.findByLemmaAndPage(lemma, page);
                if (optionalSearchIndex.isPresent()) {
                    relevance += optionalSearchIndex.get().getIndexRank();
                }
            }
            pageRelevance.put(page, relevance);
            if (relevance > maxRelevance) {
                maxRelevance = relevance;
            }
        }
        Map<Page, Float> normalizedRelevance = new HashMap<>();
        for (Map.Entry<Page, Float> entry : pageRelevance.entrySet()) {
            Page page = entry.getKey();
            float relevance = entry.getValue();
            float normalizedValue = relevance / maxRelevance;
            normalizedRelevance.put(page, normalizedValue);
        }

        List<Map.Entry<Page, Float>> sortedPages = new ArrayList<>(normalizedRelevance.entrySet());
        sortedPages.sort(Map.Entry.<Page, Float>comparingByValue().reversed());

        List<SearchData> searchDataList = new ArrayList<>();
        for (Map.Entry<Page, Float> entry : sortedPages) {
            Page page = entry.getKey();
            float relevance = entry.getValue();

            SearchData searchData = new SearchData();
            searchData.setRelevance(relevance);
            searchData.setUri(page.getPath());
            searchData.setSite(page.getSite().getUrl());
            searchData.setSiteName(page.getSite().getName());
            searchData.setSnippet("SNIPPET");
            searchData.setTitle("TITLE");
            searchDataList.add(searchData);
        }
        return new SearchResponse(searchDataList.size(), searchDataList);
    }


    public List<String> translateWordsIntoLemmas(List<String> words) {
        List<String> lemmas = new ArrayList<>();
        for (String word : words) {
            List<String> wordLemmas = lemmaProcessorService.findBaseForms(word);
            lemmas.addAll(wordLemmas);
        }
        return lemmas;
    }


    public List<Lemma> matchLemmas(List<String> lemmas) {
        List<Lemma> storedLemmas = new ArrayList<>();
        for (String lemmaWord : lemmas) {
            List<Lemma> foundLemmas = lemmaService.findAllByLemma(lemmaWord);
            storedLemmas.addAll(foundLemmas);
        }
        return storedLemmas;
    }
}
