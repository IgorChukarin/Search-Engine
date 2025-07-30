package searchengine.services.lemmaProcessing;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
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
        for (Page page : pages) {
            new Thread(() -> processPage(page)).start();
        }
        return new PositiveResponse();
    }


    private void processPage(Page page) {
        System.out.println("Start");
        String content = page.getContent();
        HashMap<String, Integer> lemmaOccurrences = countRussianLemmas(content);
        for (String extractedLemma : lemmaOccurrences.keySet()) {
            Integer siteId = page.getSite().getId();
            if (lemmaService.existsByLemmaAndSiteId(extractedLemma, siteId)) {
                Lemma lemma = lemmaService.findByLemmaAndSiteId(extractedLemma, siteId);
                Integer occurrences = lemma.getFrequency();
                lemma.setFrequency(occurrences + lemmaOccurrences.get(extractedLemma));
                lemmaService.save(lemma);
                float rank = lemmaOccurrences.get(extractedLemma);
                saveSearchIndex(lemma, page, rank);
            } else {
                Lemma lemma = new Lemma();
                lemma.setLemma(extractedLemma);
                lemma.setSite(page.getSite());
                lemma.setFrequency(lemmaOccurrences.get(extractedLemma));
                lemmaService.save(lemma);
                float rank = lemmaOccurrences.get(extractedLemma);
                saveSearchIndex(lemma, page, rank);
            }
        }
        System.out.println("end");
    }


    private HashMap<String, Integer> countRussianLemmas(String content) {
        Document document = Jsoup.parse(content);
        String text = document.text().toLowerCase();
        List<String> russianWordsWithServiceWords = extractRussianWords(text);
        List<String> russianWords = removeServiceWords(russianWordsWithServiceWords);
        HashMap<String, Integer> lemmasOccurrences = new HashMap<>();
        for (String word : russianWords) {
            List<String> baseForms = findBaseForms(word);
            for (String form : baseForms) {
                if (lemmasOccurrences.containsKey(form)) {
                    lemmasOccurrences.put(form, lemmasOccurrences.get(form) + 1);
                } else {
                    lemmasOccurrences.put(form, 1);
                }
            }
        }
        return lemmasOccurrences;
    }


    private void saveSearchIndex(Lemma lemma, Page page, float rank) {
        SearchIndex searchIndex = new SearchIndex();
        searchIndex.setLemma(lemma);
        searchIndex.setPage(page);
        searchIndex.setIndexRank(rank);
        searchIndexService.save(searchIndex);
    }


    public List<String> extractRussianWords(String text) {
        Pattern russianWordPattern = Pattern.compile("[а-яА-ЯёЁ]+");
        Matcher matcher = russianWordPattern.matcher(text);
        List<String> russianWords = new ArrayList<>();
        while (matcher.find()) {
            russianWords.add(matcher.group());
        }
        return russianWords;
    }


    private List<String> removeServiceWords(List<String> russianWords) {
        List<String> words = new ArrayList<>();
        for (String word : russianWords) {
            if (!isServiceWord(word)) {
                words.add(word);
            }
        }
        return words;
    }


    public boolean isServiceWord(String word) {
        List<String> wordBaseFormsInfo = russianLuceneMorphology.getMorphInfo(word);
        for (String info : wordBaseFormsInfo) {
            if (info.contains("СОЮЗ") ||
                    info.contains("ЧАСТ") ||
                    info.contains("МЕЖД") ||
                    info.contains("ПРЕДЛ") ||
                    info.contains("МС")) {
                return true;
            }
        }
        return false;
    }


    public List<String> findBaseForms(String word) {
        return russianLuceneMorphology.getNormalForms(word.toLowerCase());
    }
}