package io.mattw.youtube.commentsuite.refresh;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
import io.mattw.youtube.commentsuite.db.YouTubeVideo;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import io.mattw.youtube.commentsuite.util.StringTuple;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CommentThreadProducer extends ConsumerMultiProducer<YouTubeVideo> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(10);

    private final RefreshOptions options;
    private final YouTube youTube;
    private final CommentDatabase database;

    public CommentThreadProducer(final RefreshOptions options) {
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
            CommentThreadListResponse response;
            String pageToken = "";
            int page = 1;
            RefreshCommentPages commentPages = options.getCommentPages();
            final int maxAttempts = 5;
            do {
                try {
                    logger.info("{} - {}", video.getId(), video.getTitle());
                    do {
                        response = youTube.commentThreads()
                                .list("snippet")
                                .setKey(FXMLSuite.getYouTubeApiKey())
                                .setVideoId(video.getId())
                                .setMaxResults(50L)
                                .setOrder(options.getCommentOrder().name())
                                .setPageToken(pageToken)
                                .execute();

                        pageToken = response.getNextPageToken();

                        try {
                            // Maybe comments were re-enabled if we got a 403 in the past.
                            database.updateVideoHttpCode(video.getId(), 200);
                        } catch (SQLException sqle) {
                            logger.error("Failed to update video http response code", sqle);
                        }

                        final List<CommentThread> items = response.getItems();
                        if (items.isEmpty()) {
                            continue;
                        }

                        final List<YouTubeComment> comments = items.stream()
                                .map(YouTubeComment::new)
                                .filter(comment -> StringUtils.isNotEmpty(comment.getChannelId()) /* filter out G+ comments */)
                                .collect(Collectors.toList());
                        sendCollection(comments, YouTubeComment.class);

                        final List<String> channelIds = comments.stream()
                                .map(YouTubeComment::getChannelId)
                                .distinct()
                                .collect(Collectors.toList());
                        sendCollection(channelIds, String.class);

                        /*
                         * Tuple to hold commentThreadId and videoId for consuming threads.
                         *
                         * VideoId's are not included in comment thread / comments.list. Using tuple
                         * to pair a comment thread id with the video it was found on.
                         */
                        final List<StringTuple> replyThreads = comments.stream()
                                .filter(comment -> comment.getReplyCount() > 0)
                                .filter(comment -> options.getReplyPages() != RefreshReplyPages.NONE)
                                .map(comment -> new StringTuple(comment.getId(), video.getId()))
                                .collect(Collectors.toList());
                        sendCollection(replyThreads, StringTuple.class);

                        awaitMillis(50);
                    } while (pageToken != null && page++ < commentPages.getPageCount() && !isHardShutdown());

                    break;
                } catch (Exception e) {
                    if (e instanceof GoogleJsonResponseException) {
                        final GoogleJsonResponseException ge = (GoogleJsonResponseException) e;

                        try {
                            database.updateVideoHttpCode(video.getId(), ge.getStatusCode());
                        } catch (SQLException sqle) {
                            logger.error("Failed to update video http response code", sqle);
                        }

                        final GoogleJsonError.ErrorInfo firstError = ge.getDetails()
                                .getErrors()
                                .get(0);

                        if (ge.getStatusCode() == 400) {
                            final String message = String.format("[%s/%s] %s %s [videoId=%s]", attempts, maxAttempts,
                                    ge.getStatusCode(),
                                    firstError.getReason(),
                                    video.getId());

                            sendMessage(Level.WARN, e, message);

                            attempts++;
                        } else if (ge.getStatusCode() == 403) {
                            final String message = String.format("Comments Disabled [videoId=%s]", video.getId());

                            sendMessage(Level.WARN, message);

                            break;
                        } else {
                            final String message = String.format("Error %s %s [videoId=%s]",
                                    ge.getStatusCode(),
                                    firstError.getReason(),
                                    video.getId());

                            sendMessage(Level.WARN, e, message);

                            break;
                        }
                    } else {
                        final String message = String.format("[%s/%s] %s [videoId=%s]",
                                attempts,
                                maxAttempts,
                                e.getClass().getSimpleName(),
                                video.getId());

                        sendMessage(Level.WARN, e, message);
                        attempts++;
                    }
                }
            } while (attempts < maxAttempts && !isHardShutdown());

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
