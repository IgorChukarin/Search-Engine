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
        List<String> queryWords = List.of(query.split(" "));
        List<String> queryLemmas = new ArrayList<>();
        for (String word : queryWords) {
            List<String> lemmas = lemmaProcessorService.findBaseForms(word);
            queryLemmas.addAll(lemmas);
        }

        HashMap<String, HashSet<String>> lemmaToPageWords = new HashMap<>();
        for (String word : pageWords) {
            List<String> wordLemmas = lemmaProcessorService.findBaseForms(word);
            for (String lemma : wordLemmas) {
                lemmaToPageWords.computeIfAbsent(lemma, k -> new HashSet<>()).add(word);
            }
        }

        HashSet<String> matchedWords = new HashSet<>();
        for (String queryLemma : queryLemmas) {
            matchedWords.addAll(lemmaToPageWords.get(queryLemma));
        }

        List<Integer> matchedWordIndices = new ArrayList<>();
        for (int i = 0; i < pageWords.size(); i++) {
            if (matchedWords.contains(pageWords.get(i))) {
                matchedWordIndices.add(i);
            }
        }

        StringBuilder snippetBuilder = new StringBuilder();
        int startIndex = matchedWordIndices.get(0);
        int endIndex = startIndex + 25;
        if (pageWords.size() < endIndex) {
            endIndex = pageWords.size() - 1;
        }
        for (int i = startIndex; i < endIndex; i++) {
            if (matchedWordIndices.contains(i)) {
                snippetBuilder.append("<b>").append(pageWords.get(i)).append("</b>");
            } else {
                snippetBuilder.append(pageWords.get(i));
            }
            if (i < pageWords.size() - 1) {
                snippetBuilder.append(" ");
            }
        }
        return snippetBuilder.toString();
    }
}
