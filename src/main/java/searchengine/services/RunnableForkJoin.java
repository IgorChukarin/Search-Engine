package searchengine.services;

import java.util.concurrent.ForkJoinPool;

public class RunnableForkJoin implements Runnable{
    private LinkFinderAction linkFinderAction;
    private ForkJoinPool forkJoinPool = new ForkJoinPool();

    public RunnableForkJoin(LinkFinderAction linkFinderAction) {
        this.linkFinderAction = linkFinderAction;
    }

    @Override
    public void run() {
        forkJoinPool.invoke(linkFinderAction);
    }
}
