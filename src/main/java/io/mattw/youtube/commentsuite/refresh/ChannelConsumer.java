package io.mattw.youtube.commentsuite.refresh;

import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import io.mattw.youtube.commentsuite.util.ElapsedTime;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChannelConsumer extends ConsumerMultiProducer<YouTubeChannel> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(4);

    private final CommentDatabase database;

    private final Set<String> concurrentTotalViewerSet = ConcurrentHashMap.newKeySet();
    private final Set<String> concurrentNewViewerSet = ConcurrentHashMap.newKeySet();

    public ChannelConsumer() {
        this.database = FXMLSuite.getDatabase();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting ChannelConsumer");

        final ElapsedTime elapsedTime = new ElapsedTime();
        final List<YouTubeChannel> channels = new ArrayList<>();
        while (shouldKeepAlive()) {
            final YouTubeChannel channel = getBlockingQueue().poll();
            if (channel == null) {
                awaitMillis(100);
                continue;
            } else {
                channels.add(channel);
                addProcessed(1);
            }

            if (channels.size() >= 1000 || (elapsedTime.getElapsed().toMillis() >= 1200 && !channels.isEmpty())) {
                insertChannels(channels);
                elapsedTime.setNow();
            }

            awaitMillis(5);
        }

        if (!channels.isEmpty()) {
            insertChannels(channels);
        }

        logger.debug("Ending ChannelConsumer {}", shouldKeepAlive());
    }

    private void insertChannels(final List<YouTubeChannel> channels) {
        try {
            final List<String> channelIds = channels.stream()
                    .map(YouTubeChannel::getId)
                    .collect(Collectors.toList());

            final Set<String> notYetExisting = database.findChannelsNotExisting(channelIds)
                    .stream()
                    .filter(id -> !concurrentTotalViewerSet.contains(id))
                    .collect(Collectors.toSet());

            concurrentNewViewerSet.addAll(notYetExisting);
            concurrentTotalViewerSet.addAll(channelIds);

            database.insertChannels(channels);
            channels.clear();
        } catch (SQLException e) {
            logger.error("Error on channel submit", e);
        }
    }

    public int getTotalChannels() {
        return concurrentTotalViewerSet.size();
    }

    public int getNewChannels() {
        return concurrentNewViewerSet.size();
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
