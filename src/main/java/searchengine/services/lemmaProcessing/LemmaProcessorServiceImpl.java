package searchengine.services.lemmaProcessing;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.Response;
import searchengine.dto.indexing.NegativeResponse;
import searchengine.dto.indexing.PositiveResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.services.repositoryService.LemmaService;
import searchengine.services.repositoryService.PageService;
import searchengine.services.repositoryService.SearchIndexService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LemmaProcessorServiceImpl implements LemmaProcessorService {

    private final PageService pageService;
    private final LemmaService lemmaService;
    private final SearchIndexService searchIndexService;
    private final RussianLuceneMorphology russianLuceneMorphology;


    public LemmaProcessorServiceImpl(PageService pageService, LemmaService lemmaService, SearchIndexService searchIndexService) {
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.searchIndexService = searchIndexService;
        try {
            this.russianLuceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось инициализировать RussianLuceneMorphology", e);
        }
    }


    @Override
    public Response IndexPage(String url) {
        List<Page> pages = pageService.findAllByPath(url);
        if (pages.isEmpty()) {
            return new NegativeResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (Page page : pages) {
            executorService.submit(() -> processPage(page));
        }
        executorService.shutdown();
        return new PositiveResponse();
    }


    private void processPage(Page page) {
        String content = page.getContent();
        Document document = Jsoup.parse(content);
        String text = document.text().toLowerCase();
        List<String> russianWords = getRussianWords(text);
        List<String> lemmas = getLemmas(russianWords);
        HashMap<String, Integer> lemmaRanks = countLemmaPageRank(lemmas);
        saveLemmasAndIndices(lemmaRanks, page);
    }


    @Override
    public List<String> getRussianWords(String text) {
        String RUSSIAN_WORD_REGEX = "[а-яА-ЯёЁ]+";
        Pattern russianWordPattern = Pattern.compile(RUSSIAN_WORD_REGEX);
        Matcher matcher = russianWordPattern.matcher(text);
        List<String> russianWords = new ArrayList<>();
        while (matcher.find()) {
            String russianWord = matcher.group();
            if (!isServiceWord(russianWord.toLowerCase())) {
                russianWords.add(russianWord);
            }
        }
        return russianWords;
    }


    private boolean isServiceWord(String word) {
        List<String> wordBaseFormsInfo = russianLuceneMorphology.getMorphInfo(word);
        for (String info : wordBaseFormsInfo) {
            if (
                    info.contains("СОЮЗ") ||
                    info.contains("ЧАСТ") ||
                    info.contains("МЕЖД") ||
                    info.contains("ПРЕДЛ") ||
                    info.contains("МС")
            ) {
                return true;
            }
        }
        return false;
    }


    private List<String> getLemmas(List<String> russianWords) {
        List<String> lemmas = new ArrayList<>();
        for (String word : russianWords) {
            List<String> baseForms = findBaseForms(word);
            lemmas.addAll(baseForms);
        }
        return lemmas;
    }


    @Override
    public List<String> findBaseForms(String word) {
        return russianLuceneMorphology.getNormalForms(word.toLowerCase());
    }


    private HashMap<String, Integer> countLemmaPageRank(List<String> lemmas) {
        HashMap<String, Integer> lemmaPageRanks = new HashMap<>();
        for (String lemma : lemmas) {
            if (lemmaPageRanks.containsKey(lemma)) {
                lemmaPageRanks.put(lemma, lemmaPageRanks.get(lemma) + 1);
            } else {
                lemmaPageRanks.put(lemma, 1);
            }
        }
        return lemmaPageRanks;
    }


    private void saveLemmasAndIndices(HashMap<String, Integer> lemmaRanks, Page page) {
        List<Lemma> lemmasToSave = new ArrayList<>();
        List<SearchIndex> searchIndicesToSave = new ArrayList<>();
        int siteId = page.getSite().getId();
        for (String lemma : lemmaRanks.keySet()) {
            Lemma lemmaEntity = getOrCreateLemma(lemma, siteId, page, lemmasToSave);
            float rank = lemmaRanks.get(lemma);
            SearchIndex searchIndex = createSearchIndex(lemmaEntity, page, rank);
            searchIndicesToSave.add(searchIndex);
        }

        lemmaService.saveAll(lemmasToSave);
        searchIndexService.saveAll(searchIndicesToSave);
    }


    private Lemma getOrCreateLemma(String lemma, int siteId, Page page, List<Lemma> lemmasToSave) {
        Lemma lemmaEntity;
        if (lemmaService.existsByLemmaAndSiteId(lemma, siteId)) {
            lemmaEntity = lemmaService.findByLemmaAndSiteId(lemma, siteId);
            lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
        } else {
            lemmaEntity = new Lemma();
            lemmaEntity.setLemma(lemma);
            lemmaEntity.setSite(page.getSite());
            lemmaEntity.setFrequency(1);
        }
        lemmasToSave.add(lemmaEntity);
        return lemmaEntity;
    }


    private SearchIndex createSearchIndex(Lemma lemma, Page page, float rank) {
        SearchIndex searchIndex = new SearchIndex();
        searchIndex.setLemma(lemma);
        searchIndex.setPage(page);
        searchIndex.setIndexRank(rank);
        return searchIndex;
    }
}