package searchengine.services;

import java.util.List;
import java.util.concurrent.RunnableFuture;

public interface IndexingResultHandler extends Runnable {
    void setRunnableFutureList(List<RunnableFuture<String>> runnableFutureList);
}
