package searchengine.services.indexing;

import java.util.concurrent.ForkJoinPool;

public class RunnableForkJoin implements Runnable{
    private final LinkFinderAction linkFinderAction;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(4);

    public RunnableForkJoin(LinkFinderAction linkFinderAction) {
        this.linkFinderAction = linkFinderAction;
    }

    @Override
    public void run() {
        forkJoinPool.invoke(linkFinderAction);
        forkJoinPool.shutdown();
    }
}
