package io.mattw.youtube.commentsuite.refresh;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
import io.mattw.youtube.commentsuite.db.YouTubeVideo;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import io.mattw.youtube.commentsuite.util.StringTuple;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CommentThreadProducer extends ConsumerMultiProducer<YouTubeVideo> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(10);

    private final RefreshOptions options;
    private final YouTube youTube;
    private final CommentDatabase database;

    public CommentThreadProducer(RefreshOptions options) {
        this.options = options;
        this.youTube = FXMLSuite.getYouTube();
        this.database = FXMLSuite.getDatabase();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting CommentThreadProducer");

        while (shouldKeepAlive()) {
            final YouTubeVideo video = getBlockingQueue().poll();
            if (video == null) {
                awaitMillis(100);
                continue;
            }

            send(video.getChannelId());

            int attempts = 0;
            CommentThreadListResponse ctl;
            String pageToken = "";
            int page = 1;
            RefreshCommentPages commentPages = options.getCommentPages();
            int maxAttempts = 5;
            do {
                try {
                    logger.info("{} - {}", video.getId(), video.getTitle());
                    do {
                        ctl = youTube.commentThreads()
                                .list("snippet")
                                .setKey(FXMLSuite.getYouTubeApiKey())
                                .setVideoId(video.getId())
                                .setMaxResults(50L)
                                .setOrder(options.getCommentOrder().name())
                                .setPageToken(pageToken)
                                .execute();

                        pageToken = ctl.getNextPageToken();

                        try {
                            // Maybe comments were re-enabled if we got a 403 in the past.

                            database.updateVideoHttpCode(video.getId(), 200);
                        } catch (SQLException sqle) {
                            logger.error("Failed to update video http response code", sqle);
                        }

                        if (!ctl.getItems().isEmpty()) {
                            List<YouTubeComment> comments = ctl.getItems().stream()
                                    .map(YouTubeComment::new)
                                    .filter(comment -> StringUtils.isNotEmpty(comment.getChannelId())/* filter out G+ comments */)
                                    .collect(Collectors.toList());

                            comments.forEach(c -> {
                                if (c.getReplyCount() > 0 && options.getReplyPages() != RefreshReplyPages.NONE) {
//                                    incrTotalProgress(1);
//                                    updateProgress();

                                    /*
                                     * Tuple to hold commentThreadId and videoId for consuming threads.
                                     *
                                     * VideoId's are not included in comment thread / comments.list. Using tuple
                                     * to pair a comment thread id with the video it was found on.
                                     */
                                    send(new StringTuple(c.getId(), video.getId()));
                                }
                            });

                            sendCollection(comments, YouTubeComment.class);

                            final List<String> channelIds = comments.stream()
                                    .map(YouTubeComment::getChannelId)
                                    .distinct()
                                    .collect(Collectors.toList());

                            sendCollection(channelIds, String.class);
                        }

                        awaitMillis(50);
                    } while (pageToken != null && page++ < commentPages.getPageCount() && shouldKeepAlive());
                    break;
                } catch (IOException e) {
                    if (e instanceof GoogleJsonResponseException) {
                        GoogleJsonResponseException ge = (GoogleJsonResponseException) e;

                        try {
                            database.updateVideoHttpCode(video.getId(), ge.getStatusCode());
                        } catch (SQLException sqle) {
                            logger.error("Failed to update video http response code", sqle);
                        }

                        GoogleJsonError.ErrorInfo firstError = ge.getDetails()
                                .getErrors()
                                .get(0);

                        if (ge.getStatusCode() == 400) {
                            String message = String.format("[%s/%s] %s %s [videoId=%s]", attempts, maxAttempts,
                                    ge.getStatusCode(), firstError.getReason(), video.getId());

                            //appendError(message);
                            logger.warn(message, e);

                            attempts++;
                        } else if (ge.getStatusCode() == 403) {
                            String message = String.format("Comments Disabled [videoId=%s]", video.getId());

                            //appendError(message);
                            logger.warn(message);

                            break;
                        } else {
                            String message = String.format("Error %s %s [videoId=%s]",
                                    ge.getStatusCode(), firstError.getReason(), video.getId());

                            //appendError(message);
                            logger.warn(message, e);

                            break;
                        }
                    } else {
                        String message = String.format("[%s/%s] %s [videoId=%s]", attempts, maxAttempts, e.getClass().getSimpleName(), video.getId());

                        attempts++;
                        //appendError(message);
                        logger.warn(message, e);
                    }
                }
            } while (attempts < maxAttempts && shouldKeepAlive());

            addProcessed(1);
            awaitMillis(500);
        }

        logger.debug("Ending CommentThreadProducer");
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
