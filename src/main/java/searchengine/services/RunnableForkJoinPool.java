package searchengine.services;

import java.util.concurrent.ForkJoinPool;

public class RunnableForkJoinPool implements Runnable{
    private LinkFinderTask linkFinderTask;
    private ForkJoinPool forkJoinPool = new ForkJoinPool();

    public RunnableForkJoinPool(LinkFinderTask linkFinderTask) {
        this.linkFinderTask = linkFinderTask;
    }

    @Override
    public void run() {
        forkJoinPool.invoke(linkFinderTask);
    }
}
