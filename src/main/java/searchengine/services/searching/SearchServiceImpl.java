package searchengine.services.searching;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.*;
import searchengine.dto.searching.SearchDataDto;
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

        List<Lemma> lemmasMatchedWithDatabase = matchQueryLemmasWithDatabase(query);
        if (lemmasMatchedWithDatabase.isEmpty()) {
            return new PositiveSearchResponse(0, Collections.emptyList());
        }

        lemmasMatchedWithDatabase.sort(Comparator.comparingInt(Lemma::getFrequency));
        Set<Page> matchedPages = matchPages(lemmasMatchedWithDatabase);

        if (site != null) {
            matchedPages.removeIf(page -> !page.getSite().getUrl().equals(site));
        }

        if (matchedPages.isEmpty()) {
            return new PositiveSearchResponse(0, Collections.emptyList());
        }


        List<SearchDataDto> searchDataList = calculateRelevance(matchedPages, lemmasMatchedWithDatabase, query);
        return new PositiveSearchResponse(searchDataList.size(), searchDataList);
    }


    private List<Lemma> matchQueryLemmasWithDatabase(String query) {
        List<String> russianWords = lemmaProcessorService.getRussianWords(query.toLowerCase());
        HashSet<String> russianLemmas = translateWordsIntoLemmas(russianWords);
        List<Lemma> databaseMatchedLemmas = new ArrayList<>();
        for (String lemma : russianLemmas) {
            List<Lemma> foundLemmas = lemmaService.findAllByLemma(lemma);
            databaseMatchedLemmas.addAll(foundLemmas);
        }
        return databaseMatchedLemmas;
    }


    private HashSet<String> translateWordsIntoLemmas(List<String> words) {
        HashSet<String> lemmas = new HashSet<>();
        for (String word : words) {
            List<String> wordLemmas = lemmaProcessorService.findBaseForms(word.toLowerCase());
            lemmas.addAll(wordLemmas);
        }
        return lemmas;
    }


    private Set<Page> filterPagesWithDifferentSite(Set<Page> pages, String site) {
        return pages.stream()
                .filter(page -> page.getSite().getUrl().equals(site))
                .collect(Collectors.toSet());
    }



    private Set<Page> matchPages(List<Lemma> lemmas) {
        Lemma firstLemma = lemmas.get(0);
        Set<Page> matchedPages = searchIndexRepository.findAllByLemma(firstLemma)
                .stream()
                .map(SearchIndex::getPage)
                .collect(Collectors.toSet());

        if (lemmas.size() == 1) {
            return matchedPages;
        }

        for (int i = 1; i < lemmas.size(); i++) {

            if (lemmas.get(i).getLemma().equals(firstLemma.getLemma())) {
                Set<Page> pages = searchIndexRepository.findAllByLemma(lemmas.get(i))
                        .stream()
                        .map(SearchIndex::getPage)
                        .collect(Collectors.toSet());
                matchedPages.addAll(pages);
            }
//            Lemma lemma = lemmas.get(i);
//            matchedPages.removeIf(page -> !searchIndexRepository.existsByLemmaAndPage(lemma, page));
        }
        return matchedPages;
    }


    private List<SearchDataDto> calculateRelevance(Set<Page> matchedPages, List<Lemma> sortedLemmas, String query) {
        RelevanceData relevanceData = getAbsoluteRelevance(matchedPages, sortedLemmas);
        Map<Page, Float> relativeRelevance = normalizeRelevance(relevanceData.getPageRelevance(), relevanceData.getMaxRelevance());
        List<Map.Entry<Page, Float>> sortedPages = sortPagesByRelevance(relativeRelevance);
        return createSearchData(sortedPages, query);
    }


    private RelevanceData getAbsoluteRelevance(Set<Page> matchedPages, List<Lemma> matchedLemmas) {
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


    private List<SearchDataDto> createSearchData(List<Map.Entry<Page, Float>> sortedPages, String query) {
        List<SearchDataDto> searchDataList = new ArrayList<>();
        for (Map.Entry<Page, Float> entry : sortedPages) {
            Page page = entry.getKey();
            float relevance = entry.getValue();
            SearchDataDto searchData = new SearchDataDto();
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
