package io.mattw.youtube.commentsuite.refresh;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.VideoListResponse;
import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeVideo;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class VideoProducer extends ConsumerMultiProducer<String> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(1);

    private final AtomicLong timeframeSkipped = new AtomicLong();
    private final AtomicLong timeframeKept = new AtomicLong();

    private final RefreshOptions options;
    private final YouTube youTube;
    private final CommentDatabase database;
    private final ConfigData configData;

    public VideoProducer(RefreshOptions options) {
        this.options = options;
        this.youTube = CommentSuite.getYouTube();
        this.database = CommentSuite.getDatabase();
        this.configData = CommentSuite.getConfig().getDataObject();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting VideoProducer");

        try {
            final BlockingQueue<String> queue = getBlockingQueue();
            final List<String> videoIds = new ArrayList<>();
            while (shouldKeepAlive()) {
                videoIds.add(queue.poll());

                if (videoIds.size() == 50) {
                    queryAndInsert(videoIds);
                    addProcessed(videoIds.size());
                    videoIds.clear();
                }
            }
            queryAndInsert(videoIds);
            addProcessed(videoIds.size());
        } catch (IOException | SQLException e) {
            sendMessage(Level.ERROR, e, "Failed to query for videos");
        }

        logger.debug("Ending VideoProducer");
    }

    private void queryAndInsert(final List<String> videoIds) throws IOException, SQLException {
        logger.debug("Grabbing Video Data [ids={}]", videoIds);

        final YouTube.Videos.List response = youTube.videos()
                .list("snippet,statistics")
                .setKey(CommentSuite.getYouTubeApiKey())
                .setMaxResults(50L)
                .setId(String.join(",", videoIds));

        getEstimatedQuota().incrementAndGet();

        final VideoListResponse vl = response.execute();
        if (vl.getItems().isEmpty()) {
            return;
        }

        final List<YouTubeVideo> videos = vl.getItems().stream()
                .map(YouTubeVideo::from)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        database.videos().insertAll(videos);

        final RefreshTimeframe timeframe = options.getTimeframe();
        if (timeframe == RefreshTimeframe.NONE) {
            timeframeSkipped.addAndGet(videos.size());
            videos.clear();
        } else if (timeframe != RefreshTimeframe.ALL) {
            videos.removeIf(video -> {
                final LocalDate periodDate = LocalDate.now().minus(timeframe.getTimeframe());
                final LocalDate publishDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(video.getPublished()), ZoneId.systemDefault()).toLocalDate();

                final boolean skip = publishDate.isBefore(periodDate);
                if (skip) {
                    timeframeSkipped.incrementAndGet();
                }

                return skip;
            });
            timeframeKept.addAndGet(videos.size());
        } else {
            timeframeKept.addAndGet(videos.size());
        }

        sendCollection(videos, YouTubeVideo.class);
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }

    @Override
    public void onCompletion() {
        if (timeframeSkipped.get() > 0) {
            sendMessage(Level.INFO, null, String.format("Skipped %d video(s) outside timeframe", timeframeSkipped.get()));
            sendMessage(Level.INFO, null, String.format("Checking %d video(s) in timeframe", timeframeKept.get()));
        }
    }

    public AtomicLong getTimeframeSkipped() {
        return timeframeSkipped;
    }
}
