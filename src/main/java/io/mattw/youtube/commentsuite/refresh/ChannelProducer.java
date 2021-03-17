package io.mattw.youtube.commentsuite.refresh;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import io.mattw.youtube.commentsuite.util.ElapsedTime;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ChannelProducer extends ConsumerMultiProducer<String> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(5);

    private final Set<String> concurrentChannelSet = ConcurrentHashMap.newKeySet();
    private final AtomicLong duplicateSkipped = new AtomicLong();

    private final YouTube youTube;

    public ChannelProducer() {
        this.youTube = CommentSuite.getYouTube();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting ChannelProducer");

        try {
            final ElapsedTime elapsedTime = new ElapsedTime();
            final List<String> channelIds = new ArrayList<>();
            while (shouldKeepAlive()) {
                final String channelId = getBlockingQueue().poll();
                if (channelId == null) {
                    awaitMillis(5);
                    continue;
                } else {
                    addProcessed(1);

                    if (concurrentChannelSet.contains(channelId)) {
                        duplicateSkipped.addAndGet(1);
                        continue;
                    }

                    concurrentChannelSet.add(channelId);
                    channelIds.add(channelId);
                }

                if (channelIds.size() >= 50 || (elapsedTime.getElapsed().toMillis() > 3000 && !channelIds.isEmpty())) {
                    produceChannels(channelIds);
                }

                awaitMillis(5);
            }

            if (!channelIds.isEmpty()) {
                produceChannels(channelIds);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.debug("Ending ChannelProducer");
    }

    private void produceChannels(final List<String> channelIds) {
        try {
            final ChannelListResponse cl = youTube.channels()
                    .list("snippet")
                    .setKey(CommentSuite.getYouTubeApiKey())
                    .setId(String.join(",", channelIds))
                    .setMaxResults(50L)
                    .execute();

            getEstimatedQuota().incrementAndGet();

            if (cl == null || cl.getItems() == null) {
                // This seems to occur when a 'Show Channel' is input
                // as the api returns no info about the channel id.
                logger.warn("No channels were returned for input {}", channelIds);
                return;
            }

            final List<YouTubeChannel> channels = cl.getItems()
                    .stream()
                    .map(YouTubeChannel::from)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            sendCollection(channels, YouTubeChannel.class);

            channelIds.clear();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 400) {
                logger.warn(e.getDetails().getMessage());
                logger.warn("filter parameters [id={}]", String.join(",", channelIds));
            } else {
                sendMessage(Level.ERROR, e, "Failed during query for channels");
            }
        } catch (Exception e) {
            logger.error("Error on channel grab", e);
        }
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }

    public AtomicLong getDuplicateSkipped() {
        return duplicateSkipped;
    }

    @Override
    public void onCompletion() {
        if (duplicateSkipped.get() > 0) {
            sendMessage(Level.INFO, null, String.format("Ignored %d duplicate channelIds", duplicateSkipped.get()));
        }
    }

}
