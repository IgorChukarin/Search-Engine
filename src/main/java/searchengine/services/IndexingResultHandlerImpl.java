package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingResultHandler;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

@Service
@RequiredArgsConstructor
public class IndexingResultHandlerImpl implements IndexingResultHandler {

    private List<RunnableFuture<String>> runnableFutureList;
    private final SiteRepository siteRepository;

    public void setRunnableFutureList(List<RunnableFuture<String>> runnableFutureList) {
        this.runnableFutureList = runnableFutureList;
    }

    @Override
    public void run() {
        int completedTask = 0;
        while (completedTask != runnableFutureList.size()) {
            for (Iterator<RunnableFuture<String>> futureIterator = runnableFutureList.iterator();
            futureIterator.hasNext();) {
                RunnableFuture<String> future = futureIterator.next();
                if (future.isDone()) {
                    completedTask++;
                    try {
                        String siteUrl = future.get();
                        Site site = siteRepository.findByUrl(siteUrl);
                        site.setStatus(SiteStatus.INDEXED);
                        siteRepository.save(site);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
