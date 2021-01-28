package io.mattw.youtube.commentsuite.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Structure to make managing a group of threads running the same task in an ExecutorService easier.
 *
 */
public class ExecutorGroup {

    private ExecutorService service;
    private List<Future<?>> futures = new ArrayList<>();
    private int threadCount;
    private boolean began;

    /**
     * Default constructor
     *
     * @param threadCount number of threads to create
     */
    public ExecutorGroup(int threadCount) {
        this.threadCount = threadCount;

        if (threadCount == 1) {
            service = Executors.newSingleThreadExecutor();
        } else {
            service = Executors.newFixedThreadPool(threadCount);
        }
    }

    /**
     * Submits the same runnable {@link ExecutorGroup#threadCount} times and shuts the service down.
     *
     * @param runnable stateless runnable object
     */
    public void submitAndShutdown(Runnable runnable) {
        for (int i = 0; i < threadCount; i++) {
            futures.add(service.submit(runnable));
        }

        service.shutdown();
    }

    /**
     * Waits for all threads to complete.
     */
    public void await() throws InterruptedException {
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    /**
     * Check if any threads are still working.
     */
    public boolean isStillWorking() {
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                return true;
            }
        }
        return false;
    }

}
