package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class LemmaProcessor {

    public HashMap<String, Integer> countLemmas(String text) {
        HashMap<String, Integer> occurrences = new HashMap<>();
        String[] words = text.toLowerCase().split("[\\s\\p{Punct}]+");
        for (String word : words) {
            if (isServiceWord(word)) {
                continue;
            }
            List<String> baseForms = findBaseForms(word);
            for (String form : baseForms) {
                if (occurrences.containsKey(form)) {
                    occurrences.put(form, occurrences.get(form) + 1);
                } else {
                    occurrences.put(form, 1);
                }
            }
        }
        return occurrences;
    }


    private List<String> findBaseForms(String word) {
        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            return luceneMorphology.getNormalForms(word);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public boolean isServiceWord(String word) {
        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            List<String> wordBaseFormsInfo = luceneMorphology.getMorphInfo(word);
            for (String info : wordBaseFormsInfo) {
                if (info.contains("СОЮЗ") || info.contains("ЧАСТ") || info.contains("МЕЖД") || info.contains("ПРЕДЛ")) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void main(String[] args) {
        LemmaProcessor lemmaProcessor = new LemmaProcessor();
        String text = "Повторное появление леопарда в Осетии позволяет предположить, " +
                "что леопард постоянно обитает в некоторых районах Северного Кавказа.";
        HashMap<String, Integer> hm = lemmaProcessor.countLemmas(text);
        for (String key : hm.keySet()) {
            System.out.println(key + " - " + hm.get(key));
        }
    }
}


//TODO: обработать ""
//союз, част, межд
