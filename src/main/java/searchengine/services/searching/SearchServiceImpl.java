package searchengine.services.searching;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.SearchData;
import searchengine.dto.indexing.SearchResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.repository.SearchIndexRepository;
import searchengine.services.repositoryService.LemmaService;
import searchengine.services.lemmaProcessing.LemmaProcessorService;

import java.util.*;

@Service
public class SearchServiceImpl implements SearchService{

    private final LemmaProcessorService lemmaProcessorService;
    private final LemmaService lemmaService;
    private final SearchIndexRepository searchIndexRepository;
    private final PageParserService pageParser;


    public SearchServiceImpl(LemmaProcessorService lemmaProcessorService, LemmaService lemmaService, SearchIndexRepository searchIndexRepository, PageParserService pageParser) {
        this.lemmaProcessorService = lemmaProcessorService;
        this.lemmaService = lemmaService;
        this.searchIndexRepository = searchIndexRepository;
        this.pageParser = pageParser;
    }

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        List<String> words = lemmaProcessorService.getRussianWords(query);
        List<String> lemmas = translateWordsIntoLemmas(words);
        List<Lemma> matchedLemmas = matchLemmas(lemmas);
        if (matchedLemmas.isEmpty() || lemmas.size() != matchedLemmas.size()) return null;
        matchedLemmas.sort(Comparator.comparingInt(Lemma::getFrequency));
        List<SearchIndex> filteredIndices = filterIndices(matchedLemmas);
        RelevanceData relevanceData = calculateRelevance(filteredIndices, matchedLemmas);
        Map<Page, Float> pageRelevance = relevanceData.getPageRelevance();
        float maxRelevance = relevanceData.getMaxRelevance();
        Map<Page, Float> normalizedRelevance = normalizeRelevance(pageRelevance, maxRelevance);
        List<Map.Entry<Page, Float>> sortedPages = sortPagesByRelevance(normalizedRelevance);
        List<SearchData> searchDataList = createSearchData(sortedPages, query, lemmas);
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

    public List<SearchIndex> filterIndices(List<Lemma> matchedLemmas) {
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
        return searchIndices;
    }

    public RelevanceData calculateRelevance(List<SearchIndex> filteredIndices, List<Lemma> matchedLemmas) {
        Map<Page, Float> pageRelevance = new HashMap<>();
        float maxRelevance = 0.0F;
        for (SearchIndex searchIndex : filteredIndices) {
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
        return new RelevanceData(pageRelevance, maxRelevance);
    }

    private Map<Page, Float> normalizeRelevance(Map<Page, Float> pageRelevance, float maxRelevance) {
        Map<Page, Float> normalizedRelevance = new HashMap<>();
        for (Map.Entry<Page, Float> entry : pageRelevance.entrySet()) {
            Page page = entry.getKey();
            float relevance = entry.getValue();
            float normalizedValue = relevance / maxRelevance;
            normalizedRelevance.put(page, normalizedValue);
        }
        return normalizedRelevance;
    }

    private List<Map.Entry<Page, Float>> sortPagesByRelevance(Map<Page, Float> normalizedRelevance) {
        List<Map.Entry<Page, Float>> sortedPages = new ArrayList<>(normalizedRelevance.entrySet());
        sortedPages.sort(Map.Entry.<Page, Float>comparingByValue().reversed());
        return sortedPages;
    }

    private List<SearchData> createSearchData(List<Map.Entry<Page, Float>> sortedPages, String query, List<String> lemmas) {
        List<SearchData> searchDataList = new ArrayList<>();
        for (Map.Entry<Page, Float> entry : sortedPages) {
            Page page = entry.getKey();
            float relevance = entry.getValue();
            SearchData searchData = new SearchData();
            searchData.setRelevance(relevance);
            searchData.setUri(page.getPath());
            searchData.setSite(page.getSite().getUrl());
            searchData.setSiteName(page.getSite().getName());


            String title = pageParser.getTitle(page.getContent());
            searchData.setTitle(title != null ? title : "Title не найден");


            List<String> pageWords = lemmaProcessorService.getRussianWords(page.getContent());
            List<String> pageLemmas = translateWordsIntoLemmas(pageWords);

            String snippet = pageParser.getSnippet(query, pageWords);
            searchData.setSnippet(snippet != null ? snippet : "Snippet не найден");

            searchDataList.add(searchData);
        }
        return searchDataList;
    }
}
