package searchengine.services.lemmaProcessingClasses;

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
import searchengine.services.RepositoryServices.LemmaService;
import searchengine.services.RepositoryServices.PageService;
import searchengine.services.RepositoryServices.SearchIndexService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LemmaProcessorServiceImpl implements LemmaProcessorService {

    private final PageService pageService;
    private final LemmaService lemmaService;
    private final SearchIndexService searchIndexService;


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
        String content = page.getContent();
        HashMap<String, Integer> lemmaOccurrences = countRussianLemmas(content);
        for (String key : lemmaOccurrences.keySet()) {
            Integer siteId = page.getSite().getId();
            if (lemmaService.existsByLemmaAndSiteId(key, siteId)) {
                Lemma lemma = lemmaService.findByLemmaAndSiteId(key, siteId);
                Integer occurrences = lemma.getFrequency();
                lemma.setFrequency(occurrences + 1);
                lemmaService.save(lemma);
                float rank = lemmaOccurrences.get(key);
                saveSearchIndex(lemma, page, rank);
            } else {
                Lemma lemma = new Lemma();
                lemma.setLemma(key);
                lemma.setSite(page.getSite());
                lemma.setFrequency(1);
                lemmaService.save(lemma);
                float rank = lemmaOccurrences.get(key);
                saveSearchIndex(lemma, page, rank);
            }
        }
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
        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            List<String> wordBaseFormsInfo = luceneMorphology.getMorphInfo(word);
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
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<String> findBaseForms(String word) {
        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            return luceneMorphology.getNormalForms(word.toLowerCase());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}