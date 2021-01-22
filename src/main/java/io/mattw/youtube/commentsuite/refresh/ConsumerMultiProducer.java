package io.mattw.youtube.commentsuite.refresh;

import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ConsumerMultiProducer<C> {

    private static final Logger logger = LogManager.getLogger();

    private List<ConsumerMultiProducer<?>> keepAliveWith = new ArrayList<>();
    private Map<Class<?>, List<ConsumerMultiProducer<?>>> consumers = new HashMap<>();
    private BlockingQueue<C> blockingQueue = new LinkedBlockingQueue<>();
    private boolean startProduceOnFirstAccept = false;
    private boolean didProduceOnFirstAccept = false;
    private boolean hardShutdown = false;
    private AtomicLong totalAccepted = new AtomicLong();
    private AtomicLong totalProcessed = new AtomicLong();
    private AtomicLong progressWeight = new AtomicLong(1);

    public <K> void produceTo(ConsumerMultiProducer<K> consumer, Class<K> clazz) {
        consumers.computeIfPresent(clazz, (key, value) -> {
            value.add(consumer);
            return value;
        });
        consumers.computeIfAbsent(clazz, (key) -> {
            List<ConsumerMultiProducer<?>> list = new ArrayList<>();
            list.add(consumer);
            return list;
        });
    }

    public abstract void startProducing();

    public abstract ExecutorGroup getExecutorGroup();

    public void accept(Collection<C> objects) {
        totalAccepted.addAndGet(objects.size());

        blockingQueue.addAll(objects);

        produceOnFirstAccept();
    }

    public void accept(C object) {
        totalAccepted.addAndGet(1);

        blockingQueue.add(object);

        produceOnFirstAccept();
    }

    private synchronized void produceOnFirstAccept() {
        if (startProduceOnFirstAccept && !didProduceOnFirstAccept) {
            didProduceOnFirstAccept = true;
            startProducing();
        }
    }

    public void keepAliveWith(ConsumerMultiProducer<?>... consumers) {
        keepAliveWith.addAll(Arrays.asList(consumers));
    }

    public void setHardShutdown(boolean hardShutdown) {
        this.hardShutdown = hardShutdown;
    }

    public boolean shouldKeepAlive() {
        return !hardShutdown && (!blockingQueue.isEmpty() ||
                keepAliveWith.stream().anyMatch(ConsumerMultiProducer::shouldKeepAlive) ||
                keepAliveWith.stream().map(ConsumerMultiProducer::getExecutorGroup).anyMatch(ExecutorGroup::isStillWorking));
    }

    public void awaitMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public <K> void sendCollection(Collection<K> objects, Class<K> clazz) {
        for (ConsumerMultiProducer<?> consumer : consumers.get(clazz)) {
            ((ConsumerMultiProducer<K>) consumer).accept(objects);
        }
    }

    public <K> void send(K object) {
        for (ConsumerMultiProducer<?> consumer : consumers.get(object.getClass())) {
            ((ConsumerMultiProducer<K>) consumer).accept(object);
        }
    }

    public AtomicLong getTotalAccepted() {
        return totalAccepted;
    }

    public AtomicLong getTotalProcessed() {
        return totalProcessed;
    }

    public AtomicLong getProgressWeight() {
        return progressWeight;
    }

    public void addProcessed(long amount) {
        getTotalProcessed().addAndGet(amount);
    }

    public BlockingQueue<C> getBlockingQueue() {
        return blockingQueue;
    }

    public boolean isStartProduceOnFirstAccept() {
        return startProduceOnFirstAccept;
    }

    public void setStartProduceOnFirstAccept(boolean startProduceOnFirstAccept) {
        this.startProduceOnFirstAccept = startProduceOnFirstAccept;
    }
}
