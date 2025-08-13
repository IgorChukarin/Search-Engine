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
import searchengine.model.Site;
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
    public Response indexPage(String url) {
        List<Page> pages = pageService.findAllByPath(url);
        if (pages.isEmpty()) {
            return new NegativeResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (Page page : pages) {
            executorService.submit(() -> extractLemmasAndIndices(page));
        }
        executorService.shutdown();
        return new PositiveResponse();
    }


    private void extractLemmasAndIndices(Page page) {
        String content = page.getContent();
        Document document = Jsoup.parse(content);
        String text = document.text().toLowerCase();
        List<String> russianWords = getRussianWords(text);
        List<String> lemmas = getLemmas(russianWords);
        HashMap<String, Integer> lemmasIndexRanks = countLemmasIndexRanks(lemmas);
        saveLemmasAndIndices(lemmasIndexRanks, page);
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
            List<String> wordBaseForms = findBaseForms(word);
            lemmas.addAll(wordBaseForms);
        }
        return lemmas;
    }


    @Override
    public List<String> findBaseForms(String word) {
        return russianLuceneMorphology.getNormalForms(word.toLowerCase());
    }


    private HashMap<String, Integer> countLemmasIndexRanks(List<String> lemmas) {
        HashMap<String, Integer> lemmasIndexRanks = new HashMap<>();
        for (String lemma : lemmas) {
            if (lemmasIndexRanks.containsKey(lemma)) {
                int currentRank = lemmasIndexRanks.get(lemma);
                lemmasIndexRanks.put(lemma, currentRank + 1);
            } else {
                lemmasIndexRanks.put(lemma, 1);
            }
        }
        return lemmasIndexRanks;
    }


    private void saveLemmasAndIndices(HashMap<String, Integer> lemmasIndexRanks, Page page) {
        List<Lemma> lemmasToSave = new ArrayList<>();
        List<SearchIndex> searchIndicesToSave = new ArrayList<>();
        for (String lemma : lemmasIndexRanks.keySet()) {

            Site site = page.getSite();
            Lemma lemmaEntity = getOrCreateLemma(lemma, site);

            float lemmaIndexRank = lemmasIndexRanks.get(lemma);
            SearchIndex searchIndex = createSearchIndex(lemmaEntity, page, lemmaIndexRank);

            lemmasToSave.add(lemmaEntity);
            searchIndicesToSave.add(searchIndex);
        }
        lemmaService.saveAll(lemmasToSave);
        searchIndexService.saveAll(searchIndicesToSave);
    }


    private Lemma getOrCreateLemma(String lemma, Site site) {
        Lemma lemmaEntity = lemmaService.findByLemmaAndSiteId(lemma, site.getId());
        if (lemmaEntity != null) {
            int currentFrequency = lemmaEntity.getFrequency();
            lemmaEntity.setFrequency(currentFrequency + 1);
        } else {
            lemmaEntity = new Lemma();
            lemmaEntity.setLemma(lemma);
            lemmaEntity.setSite(site);
            lemmaEntity.setFrequency(1);
        }
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