package io.mattw.youtube.commentsuite.refresh;

import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The ConsumerMultiProducer is always a consumer of one type and optionally can
 * produce multiple different types for other ConsumerMultiProducers.
 *
 * While it can be more generic, it is made more specific to this use case with
 * ExecutorGroups for multithreaded processing of queue objects.
 *
 * @param <C> consuming type
 */
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
    private TriConsumer<Level, Throwable, String> messageFunc;

    /**
     * @param consumer consuemr
     * @param clazz needed for sendCollection()
     * @param <K> consumer must consume type of clazz
     */
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

    /**
     * Start producing using the ExecutorGroup
     */
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

    /**
     * Keep the ExecutorGroup alive when the producer(s) it depends on are still working.
     */
    public void keepAliveWith(ConsumerMultiProducer<?>... consumers) {
        keepAliveWith.addAll(Arrays.asList(consumers));
    }

    /**
     * Signal to the ExecutorGroup threads to end at the next best possible time.
     */
    public void setHardShutdown(boolean hardShutdown) {
        this.hardShutdown = hardShutdown;
    }

    public boolean isHardShutdown() {
        return hardShutdown;
    }

    /**
     * Keep the ExecutorGroup alive
     */
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
        if (!consumers.containsKey(clazz)) {
            return;
        }
        for (ConsumerMultiProducer<?> consumer : consumers.get(clazz)) {
            ((ConsumerMultiProducer<K>) consumer).accept(objects);
        }
    }

    public <K> void send(K object) {
        if (!consumers.containsKey(object.getClass())) {
            return;
        }
        for (ConsumerMultiProducer<?> consumer : consumers.get(object.getClass())) {
            ((ConsumerMultiProducer<K>) consumer).accept(object);
        }
    }

    public void setMessageFunc(TriConsumer<Level, Throwable, String> messageFunc) {
        this.messageFunc = messageFunc;
    }

    public void sendMessage(Level level, String message) {
        sendMessage(level, null, message);
    }

    public void sendMessage(Level level, Throwable error) {
        sendMessage(level, error, null);
    }

    public void sendMessage(Level level, Throwable error, String message) {
        if (messageFunc != null) {
            messageFunc.accept(level, error, message);
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

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "executorGroup=" + getExecutorGroup().isStillWorking() +
                ", blockingQueue=" + blockingQueue.size() +
                ", totalAccepted=" + totalAccepted +
                ", totalProcessed=" + totalProcessed +
                '}';
    }
}
