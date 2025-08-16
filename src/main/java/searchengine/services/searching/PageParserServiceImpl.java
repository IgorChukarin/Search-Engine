package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.services.lemmaProcessing.LemmaProcessorService;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PageParserServiceImpl implements PageParserService {

    private final LemmaProcessorService lemmaProcessorService;

    @Override
    public String getTitle(String pageContent) {
        int titleStart = pageContent.indexOf("<title>");
        if (titleStart != -1) {
            int titleEnd = pageContent.indexOf("</title>", titleStart);
            if (titleEnd != -1) {
                return pageContent.substring(titleStart + 7, titleEnd).trim();
            }
        }
        return null;
    }


    @Override
    public String getSnippet(String query, List<String> pageWords) {
        List<String> queryLemmas = extractQueryLemmas(query);
        HashMap<String, HashSet<String>> lemmaToPageWords = mapPageWordsToLemmas(pageWords);
        HashSet<String> matchedWords = findMatchedWords(queryLemmas, lemmaToPageWords);
        List<Integer> matchedWordIndices = findMatchedWordIndices(pageWords, matchedWords); // тут проеб капитальный
        return buildSnippet(pageWords, matchedWordIndices);
    }


    private List<String> extractQueryLemmas(String query) {
        List<String> queryLemmas = new ArrayList<>();
        List<String> queryWords = List.of(query.split(" "));
        for (String word : queryWords) {
            List<String> lemmas = lemmaProcessorService.findBaseForms(word);
            queryLemmas.addAll(lemmas);
        }
        return queryLemmas;
    }


    private HashMap<String, HashSet<String>> mapPageWordsToLemmas(List<String> pageWords) {
        HashMap<String, HashSet<String>> lemmaToPageWords = new HashMap<>();
        for (String word : pageWords) {
            List<String> wordLemmas = lemmaProcessorService.findBaseForms(word);
            for (String lemma : wordLemmas) {
                lemmaToPageWords.computeIfAbsent(lemma, k -> new HashSet<>()).add(word);
            }
        }
        return lemmaToPageWords;
    }


    private HashSet<String> findMatchedWords(List<String> queryLemmas, HashMap<String, HashSet<String>> lemmaToPageWords) {
        HashSet<String> matchedWords = new HashSet<>();
        for (String queryLemma : queryLemmas) {
            matchedWords.addAll(lemmaToPageWords.getOrDefault(queryLemma, new HashSet<>()));
        }
        return matchedWords;
    }


    private List<Integer> findMatchedWordIndices(List<String> pageWords, HashSet<String> matchedWords) {
        List<Integer> matchedWordIndices = new ArrayList<>();
        for (int i = 0; i < pageWords.size(); i++) {
            if (matchedWords.contains(pageWords.get(i))) {
                matchedWordIndices.add(i);
            }
        }
        return matchedWordIndices;
    }


    private String buildSnippet(List<String> pageWords, List<Integer> matchedWordIndices) {
        StringBuilder snippetBuilder = new StringBuilder();
        int startIndex = matchedWordIndices.get(0);
        int endIndex = Math.min(startIndex + 25, pageWords.size());
        for (int i = startIndex; i < endIndex; i++) {
            if (matchedWordIndices.contains(i)) {
                snippetBuilder.append("<b>").append(pageWords.get(i)).append("</b>");
            } else {
                snippetBuilder.append(pageWords.get(i));
            }
            if (i < endIndex - 1) {
                snippetBuilder.append(" ");
            }
        }
        return snippetBuilder.toString();
    }
}
