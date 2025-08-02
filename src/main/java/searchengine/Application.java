package searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

//TODO:
// Multi threading in search control
// LinkFinderAction - URL, URI
// Many threads + FJP each?
// Decouple создвние записи со статусом indexing
// ForkJoinPool - общий
// Test reindexing page
// Потокобезопасность сохранения лемм и индексов
