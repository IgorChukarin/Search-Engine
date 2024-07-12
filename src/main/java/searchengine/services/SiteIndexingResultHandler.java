package searchengine.services;

import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

public class SiteIndexingResultHandler implements Runnable{

    private final List<RunnableFuture<String>> runnableFutureList;
    private final SiteRepository siteRepository;

    public SiteIndexingResultHandler(List<RunnableFuture<String>> runnableFutureList, SiteRepository siteRepository) {
        this.runnableFutureList = runnableFutureList;
        this.siteRepository = siteRepository;
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
