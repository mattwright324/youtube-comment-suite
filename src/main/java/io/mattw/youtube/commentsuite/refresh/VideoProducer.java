package io.mattw.youtube.commentsuite.refresh;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.VideoListResponse;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeVideo;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
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
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class VideoProducer extends ConsumerMultiProducer<String> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(1);

    private final RefreshOptions options;
    private final YouTube youTube;
    private final CommentDatabase database;

    public VideoProducer(RefreshOptions options) {
        this.options = options;
        this.youTube = FXMLSuite.getYouTube();
        this.database = FXMLSuite.getDatabase();
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
            logger.error(e);
        }

        logger.debug("Ending VideoProducer");
    }

    private void queryAndInsert(final List<String> videoIds) throws IOException, SQLException {
        logger.debug("Grabbing Video Data [ids={}]", videoIds);

        final YouTube.Videos.List yvl = youTube.videos()
                .list("snippet,statistics")
                .setKey(FXMLSuite.getYouTubeApiKey())
                .setMaxResults(50L)
                .setId(String.join(",", videoIds))
                .setPageToken("");

        final VideoListResponse vl = yvl.execute();
        if (vl.getItems().isEmpty()) {
            return;
        }

        final List<YouTubeVideo> videos = vl.getItems().stream()
                .map(YouTubeVideo::new)
                .collect(Collectors.toList());

        database.insertVideos(videos);

        final RefreshTimeframe timeframe = options.getTimeframe();
        if (timeframe == RefreshTimeframe.NONE || options.getCommentPages() == RefreshCommentPages.NONE) {
            videos.clear();
        } else if (timeframe != RefreshTimeframe.ALL) {
            videos.removeIf(video -> {
                LocalDate periodDate = LocalDate.now().minus(timeframe.getTimeframe());
                LocalDate publishDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(video.getPublishedDate()), ZoneId.systemDefault()).toLocalDate();

                return publishDate.isBefore(periodDate);
            });
        }

        sendCollection(videos, YouTubeVideo.class);
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
