package io.mattw.youtube.commentsuite.refresh;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import io.mattw.youtube.commentsuite.util.ElapsedTime;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelProducer extends ConsumerMultiProducer<String> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(20);

    private final YouTube youTube;

    public ChannelProducer() {
        this.youTube = FXMLSuite.getYouTube();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting ChannelProducer");

        final ElapsedTime elapsedTime = new ElapsedTime();
        final List<String> channelIds = new ArrayList<>();
        while (shouldKeepAlive()) {
            final String channelId = getBlockingQueue().poll();
            if (channelId == null) {
                awaitMillis(100);
                continue;
            } else {
                channelIds.add(channelId);
                addProcessed(1);
            }

            if (channelIds.size() >= 50 || (elapsedTime.getElapsed().toMillis() > 500 && !channelIds.isEmpty())) {
                produceChannels(channelIds);
            }

            awaitMillis(5);
        }

        if (!channelIds.isEmpty()) {
            produceChannels(channelIds);
        }

        logger.debug("Ending ChannelProducer");
    }

    private void produceChannels(final List<String> channelIds) {
        try {
            final ChannelListResponse cl = youTube.channels()
                    .list("snippet")
                    .setKey(FXMLSuite.getYouTubeApiKey())
                    .setId(String.join(",", channelIds))
                    .setMaxResults(50L)
                    .execute();

            final List<YouTubeChannel> channels = cl.getItems()
                    .stream()
                    .map(YouTubeChannel::new)
                    .collect(Collectors.toList());

            sendCollection(channels, YouTubeChannel.class);

            channelIds.clear();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 400) {
                logger.warn(e.getDetails().getMessage());
                logger.warn("filter parameters [id={}]", String.join(",", channelIds));
            } else {
                e.printStackTrace();
            }
        } catch (IOException e) {
            logger.error("Error on channel grab", e);
        }
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
