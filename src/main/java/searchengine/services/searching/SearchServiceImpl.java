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
import searchengine.services.repositoryService.SearchIndexService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService{

    private final LemmaProcessorService lemmaProcessorService;
    private final LemmaService lemmaService;
    private final SearchIndexRepository searchIndexRepository;
    private final SearchIndexService searchIndexService;
    private final PageParserService pageParser;


    public SearchServiceImpl(LemmaProcessorService lemmaProcessorService, LemmaService lemmaService, SearchIndexRepository searchIndexRepository, SearchIndexService searchIndexService, PageParserService pageParser) {
        this.lemmaProcessorService = lemmaProcessorService;
        this.lemmaService = lemmaService;
        this.searchIndexRepository = searchIndexRepository;
        this.searchIndexService = searchIndexService;
        this.pageParser = pageParser;
    }

    @Override
    public Response search(String query, String site, int offset, int limit) {
        if (query == null || query.isBlank()) {
            return new NegativeResponse("Задан пустой поисковый запрос");
        }

        Response emptySearchResponse = new PositiveSearchResponse(0, Collections.emptyList());
        List<Lemma> lemmasMatchedWithDatabase = matchQueryLemmasWithDatabase(query);
        if (lemmasMatchedWithDatabase.isEmpty()) {
            return emptySearchResponse;
        }

        lemmasMatchedWithDatabase.sort(Comparator.comparingInt(Lemma::getFrequency));
        Set<Page> matchedPages = matchPages(lemmasMatchedWithDatabase);

        if (site != null) {
            matchedPages.removeIf(page -> !page.getSite().getUrl().equals(site));
        }

        if (matchedPages.isEmpty()) {
            return emptySearchResponse;
        }

        Map<Page, Float> pageToRelativeRelevance = calculateRelevance(matchedPages, lemmasMatchedWithDatabase, query);
        List<Map.Entry<Page, Float>> sortedPages = sortPagesByRelevance(pageToRelativeRelevance);
        List<SearchDataDto> searchDataList = createSearchData(sortedPages, query);
        return new PositiveSearchResponse(searchDataList.size(), searchDataList);
    }


    private List<Lemma> matchQueryLemmasWithDatabase(String query) {
        List<String> russianWords = lemmaProcessorService.getRussianWords(query.toLowerCase());
        HashSet<String> russianLemmas = convertWordsIntoLemmas(russianWords);
        List<Lemma> databaseMatchedLemmas = new ArrayList<>();
        for (String lemma : russianLemmas) {
            List<Lemma> foundLemmas = lemmaService.findAllByLemma(lemma);
            databaseMatchedLemmas.addAll(foundLemmas);
        }
        return databaseMatchedLemmas;
    }


    private HashSet<String> convertWordsIntoLemmas(List<String> words) {
        HashSet<String> lemmas = new HashSet<>();
        for (String word : words) {
            List<String> wordLemmas = lemmaProcessorService.findBaseForms(word.toLowerCase());
            lemmas.addAll(wordLemmas);
        }
        return lemmas;
    }


    private Set<Page> matchPages(List<Lemma> lemmas) {
        Lemma firstLemma = lemmas.get(0);

//        Set<Page> matchedPages = searchIndexRepository.findAllByLemma(firstLemma)
        Set<Page> matchedPages = searchIndexService.findAllByLemma_Lemma(firstLemma.getLemma())
                .stream()
                .map(SearchIndex::getPage)
                .collect(Collectors.toSet());

        if (lemmas.size() == 1) {
            return matchedPages;
        }

        for (Page page : matchedPages) {
            if (!pageContainsLemmas(page, lemmas)) {
                matchedPages.remove(page);
            }
        }
        return matchedPages;
    }


    private boolean pageContainsLemmas(Page page, List<Lemma> lemmas) {
        for (Lemma lemma : lemmas) {
            if(!searchIndexRepository.existsByLemma_LemmaAndPage_Id(lemma.getLemma(), page.getId())) {
                return false;
            }
        }
        return true;
    }


    private Map<Page, Float> calculateRelevance(Set<Page> matchedPages, List<Lemma> sortedLemmas, String query) {
        RelevanceData relevanceData = getAbsoluteAndMaxRelevance(matchedPages, sortedLemmas);
        return getRelativeRelevance(relevanceData.getPageToAbsoluteRelevance(), relevanceData.getMaxRelevance());
    }


    private RelevanceData getAbsoluteAndMaxRelevance(Set<Page> matchedPages, List<Lemma> matchedLemmas) {
        Map<Page, Float> pageToRelevance = new HashMap<>();
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
            pageToRelevance.put(page, relevance);
        }
        return new RelevanceData(pageToRelevance, maxRelevance);
    }


    private Map<Page, Float> getRelativeRelevance(Map<Page, Float> pageToAbsoluteRelevance, float maxRelevance) {
        Map<Page, Float> relativeRelevance = new HashMap<>();
        for (Map.Entry<Page, Float> entry : pageToAbsoluteRelevance.entrySet()) {
            Page page = entry.getKey();
            float relevance = entry.getValue();
            float normalizedValue = relevance / maxRelevance;
            relativeRelevance.put(page, normalizedValue);
        }
        return relativeRelevance;
    }


    private List<Map.Entry<Page, Float>> sortPagesByRelevance(Map<Page, Float> pageToRelativeRelevance) {
        List<Map.Entry<Page, Float>> sortedPages = new ArrayList<>(pageToRelativeRelevance.entrySet());
        sortedPages.sort(Map.Entry.<Page, Float>comparingByValue().reversed());
        return sortedPages;
    }


    private List<SearchDataDto> createSearchData(List<Map.Entry<Page, Float>> sortedPages, String query) {
        List<SearchDataDto> searchDataDtos = new ArrayList<>();
        for (Map.Entry<Page, Float> entry : sortedPages) {
            Page page = entry.getKey();
            float relevance = entry.getValue();
            SearchDataDto searchDataDto = new SearchDataDto();
            searchDataDto.setRelevance(relevance);
            searchDataDto.setUri(page.getPath());
            searchDataDto.setSite(page.getSite().getUrl().substring(0, page.getSite().getUrl().length() - 1));
            searchDataDto.setSiteName(page.getSite().getName());

            String title = pageParser.getTitle(page.getContent());
            searchDataDto.setTitle(title != null ? title : "Title не найден");

            List<String> pageWords = lemmaProcessorService.getRussianWords(page.getContent());

            String snippet = pageParser.getSnippet(query, pageWords);
            searchDataDto.setSnippet(snippet != null ? snippet : "Snippet не найден");

            searchDataDtos.add(searchDataDto);
        }
        return searchDataDtos;
    }
}
