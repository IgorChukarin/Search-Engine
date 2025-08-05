package searchengine.services.searching;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.*;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.repository.SearchIndexRepository;
import searchengine.services.repositoryService.LemmaService;
import searchengine.services.lemmaProcessing.LemmaProcessorService;

import java.util.*;
import java.util.stream.Collectors;

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
    public Response search(String query, String site, int offset, int limit) {
        if (query == null || query.isBlank()) {
            return new NegativeResponse("Задан пустой поисковый запрос");
        }

        List<String> queryWords = lemmaProcessorService.getRussianWords(query.toLowerCase());
        HashSet<String> queryLemmas = translateWordsIntoLemmas(queryWords);
        List<Lemma> matchedLemmas = matchLemmasFromDataBase(queryLemmas);
        if (matchedLemmas.size() != queryLemmas.size()) {
            return new PositiveSearchResponse(0, Collections.emptyList());
        }
        List<Lemma> sortedLemmas = new ArrayList<>(matchedLemmas);
        sortedLemmas.sort(Comparator.comparingInt(Lemma::getFrequency));
        Set<Page> matchedPages = matchPages(sortedLemmas);
        if (matchedPages.isEmpty()) {
            return new PositiveSearchResponse(0, Collections.emptyList());
        }
        if (site != null) {
            matchedPages.removeIf(page -> !page.getSite().getUrl().equals(site));
        }
        RelevanceData relevanceData = getAbsoluteRelevance(matchedPages, sortedLemmas);
        Map<Page, Float> pageRelevance = relevanceData.getPageRelevance();
        float maxRelevance = relevanceData.getMaxRelevance();
        Map<Page, Float> relativeRelevance = normalizeRelevance(pageRelevance, maxRelevance);
        List<Map.Entry<Page, Float>> sortedPages = sortPagesByRelevance(relativeRelevance);
        List<SearchData> searchDataList = createSearchData(sortedPages, query);
        return new PositiveSearchResponse(searchDataList.size(), searchDataList);
    }


    public HashSet<String> translateWordsIntoLemmas(List<String> words) {
        HashSet<String> lemmas = new HashSet<>();
        for (String word : words) {
            List<String> wordLemmas = lemmaProcessorService.findBaseForms(word.toLowerCase());
            lemmas.addAll(wordLemmas);
        }
        return lemmas;
    }


    public List<Lemma> matchLemmasFromDataBase(HashSet<String> queryLemmas) {
        List<Lemma> indexedLemmas = new ArrayList<>();
        for (String queryLemma : queryLemmas) {
            List<Lemma> foundLemmas = lemmaService.findAllByLemma(queryLemma);
            indexedLemmas.addAll(foundLemmas);
        }
        return indexedLemmas;
    }


    public Set<Page> matchPages(List<Lemma> sortedLemmas) {
        if (sortedLemmas.isEmpty()) {
            return Collections.emptySet();
        }

        Lemma firstLemma = sortedLemmas.get(0);
        Set<Page> matchedPages = searchIndexRepository.findAllByLemma(firstLemma)
                .stream()
                .map(SearchIndex::getPage)
                .collect(Collectors.toSet());

        if (sortedLemmas.size() == 1) {
            return matchedPages;
        }

        for (int i = 1; i < sortedLemmas.size(); i++) {
            Lemma lemma = sortedLemmas.get(i);
            matchedPages.removeIf(page -> !searchIndexRepository.existsByLemmaAndPage(lemma, page));
        }
        return matchedPages;
    }


    public RelevanceData getAbsoluteRelevance(Set<Page> matchedPages, List<Lemma> matchedLemmas) {
        Map<Page, Float> pageRelevance = new HashMap<>();
        float maxRelevance = 0;
        for (Page page : matchedPages) {
            float relevance = 0;
            for (Lemma lemma : matchedLemmas) {
                Optional<SearchIndex> searchIndex = searchIndexRepository.findByLemmaAndPage(lemma, page);
                if (searchIndex.isPresent()) {
                    relevance += searchIndex.get().getIndexRank();
                }
            }
            if (relevance > maxRelevance) {
                maxRelevance = relevance;
            }
            pageRelevance.put(page, relevance);
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


    private List<SearchData> createSearchData(List<Map.Entry<Page, Float>> sortedPages, String query) {
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

            String snippet = pageParser.getSnippet(query, pageWords);
            searchData.setSnippet(snippet != null ? snippet : "Snippet не найден");

            searchDataList.add(searchData);
        }
        return searchDataList;
    }
}
