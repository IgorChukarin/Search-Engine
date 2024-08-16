package searchengine.services.linkFinderClasses;

import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ControlThread implements Runnable {
    private Thread worker;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private int interval;

    public ControlThread(int sleepInterval, RunnableFuture<String> runnableFuture) {
        interval = sleepInterval;
        worker = new Thread(runnableFuture);
    }

    public void start() {
        worker.start();
    }

    public void interrupt() {
        running.set(false);
        worker.interrupt();
    }

    boolean isRunning() {
        return running.get();
    }

    boolean isStopped() {
        return stopped.get();
    }

    public void run() {
        running.set(true);
        stopped.set(false);
        while (running.get()) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println(
                        "Thread was interrupted, Failed to complete operation");
            }
            System.out.println("I'm working");
        }
        stopped.set(true);
    }
}
