package io.mattw.youtube.commentsuite.refresh;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.apache.commons.lang3.ObjectUtils.anyNull;

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
    private Map<Class<?>, List<ConsumerMultiProducer<?>>> consumersByClass = new HashMap<>();
    private Map<String, List<ConsumerMultiProducer<?>>> consumersByKey = new HashMap<>();
    private BlockingQueue<C> blockingQueue = new LinkedBlockingQueue<>();
    private boolean startProduceOnFirstAccept = false;
    private boolean didProduceOnFirstAccept = false;
    private boolean hardShutdown = false;
    private AtomicLong totalAccepted = new AtomicLong();
    private AtomicLong totalProcessed = new AtomicLong();
    private AtomicLong progressWeight = new AtomicLong(1);
    private AtomicLong estimatedQuota = new AtomicLong();
    private TriConsumer<Level, Throwable, String> messageFunc;

    /**
     * @param consumer consuemr
     * @param clazz needed for sendCollection()
     * @param <P> consumer must consume type of clazz
     */
    public <P> void produceTo(ConsumerMultiProducer<P> consumer, Class<P> clazz) {
        consumersByClass.computeIfPresent(clazz, (key, value) -> {
            value.add(consumer);
            return value;
        });
        consumersByClass.computeIfAbsent(clazz, (key) -> {
            List<ConsumerMultiProducer<?>> list = new ArrayList<>();
            list.add(consumer);
            return list;
        });
    }

    /**
     * @param consumer consuemr
     * @param clazz needed for sendCollection()
     * @param <P> consumer must consume type of clazz
     */
    public <P> void produceTo(ConsumerMultiProducer<P> consumer, Class<P> clazz, String key) {
        consumersByKey.computeIfPresent(key, (key1, value) -> {
            value.add(consumer);
            return value;
        });
        consumersByKey.computeIfAbsent(key, (key1) -> {
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
        if (objects == null || objects.isEmpty()) {
            return;
        }

        totalAccepted.addAndGet(objects.size());
        blockingQueue.addAll(objects);

        produceOnFirstMeaningfulAccept();
    }

    public void accept(C object) {
        if (object == null) {
            return;
        }

        totalAccepted.addAndGet(1);
        blockingQueue.add(object);

        produceOnFirstMeaningfulAccept();
    }

    /**
     * Will only start producing on the first non-null non-empty accept,
     */
    private synchronized void produceOnFirstMeaningfulAccept() {
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

    public boolean isNotHardShutdown() {
        return !hardShutdown;
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

    public <P> void sendCollection(Collection<P> objects, Class<P> clazz) {
        if (!consumersByClass.containsKey(clazz) || objects.isEmpty()) {
            return;
        }
        for (ConsumerMultiProducer<?> consumer : consumersByClass.get(clazz)) {
            ((ConsumerMultiProducer<P>) consumer).accept(objects);
        }
    }

    public <P> void send(P object) {
        if (object == null) {
            return;
        }
        if (!consumersByClass.containsKey(object.getClass())) {
            return;
        }
        for (ConsumerMultiProducer<?> consumer : consumersByClass.get(object.getClass())) {
            ((ConsumerMultiProducer<P>) consumer).accept(object);
        }
    }

    public <P> void sendCollection(Collection<P> objects, Class<P> clazz, String key) {
        if (anyNull(objects, clazz, key)) {
            return;
        }
        if (!consumersByKey.containsKey(key) || objects.isEmpty()) {
            return;
        }
        for (ConsumerMultiProducer<?> consumer : consumersByKey.get(key)) {
            ((ConsumerMultiProducer<P>) consumer).accept(objects);
        }
    }

    public <P> void send(P object, String key) {
        if (!consumersByKey.containsKey(key)) {
            return;
        }
        for (ConsumerMultiProducer<?> consumer : consumersByKey.get(key)) {
            ((ConsumerMultiProducer<P>) consumer).accept(object);
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

    public void onCompletion() {
        // Default nothing on completion
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

    public AtomicLong getEstimatedQuota() {
        return estimatedQuota;
    }

    public void addProcessed(long amount) {
        getTotalProcessed().addAndGet(amount);
    }

    public BlockingQueue<C> getBlockingQueue() {
        return blockingQueue;
    }

    public List<ConsumerMultiProducer<?>> getKeepAliveWith() {
        return keepAliveWith;
    }

    public boolean isStartProduceOnFirstAccept() {
        return startProduceOnFirstAccept;
    }

    public void setStartProduceOnFirstAccept(boolean startProduceOnFirstAccept) {
        this.startProduceOnFirstAccept = startProduceOnFirstAccept;
    }

    public <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static String getFirstReasonCode(GoogleJsonResponseException e) {
        return Optional.ofNullable(e)
                .map(GoogleJsonResponseException::getDetails)
                .map(GoogleJsonError::getErrors)
                .orElse(Collections.emptyList())
                .stream()
                .map(GoogleJsonError.ErrorInfo::getReason)
                .findFirst()
                .orElse(StringUtils.EMPTY);
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
