package io.mattw.youtube.commentsuite.refresh;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.CommentThreadReplies;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
import io.mattw.youtube.commentsuite.db.YouTubeVideo;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import io.mattw.youtube.commentsuite.util.StringTuple;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommentThreadProducer extends ConsumerMultiProducer<YouTubeVideo> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(15);

    private final RefreshOptions options;
    private final RefreshCommentPages pages;
    private final YouTube youTube;
    private final CommentDatabase database;

    public CommentThreadProducer(final RefreshOptions options, final RefreshCommentPages pages) {
        this.options = options;
        this.pages = pages;
        this.youTube = CommentSuite.getYouTube();
        this.database = CommentSuite.getDatabase();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        if (pages == RefreshCommentPages.NONE) {
            logger.debug("Skipping CommentThreadProducer pages=NONE");
            addProcessed(getBlockingQueue().size());
            getBlockingQueue().clear();
            return;
        }

        logger.debug("Starting CommentThreadProducer");

        while (shouldKeepAlive()) {
            final YouTubeVideo video = getBlockingQueue().poll();
            if (video == null) {
                awaitMillis(100);
                continue;
            }

            logger.debug(video);

            send(video.getChannelId());

            int attempt = 1;
            CommentThreadListResponse response;
            String pageToken = "";
            int page = 1;
            final int maxAttempts = options.getMaxRetryAttempts();

            threadLoop:
            do {
                try {
                    do {
                        logger.info("Try #{} {} {}", attempt, video.getId(), video.getTitle());

                        response = youTube.commentThreads()
                                .list("snippet,replies")
                                .setKey(CommentSuite.getYouTubeApiKey())
                                .setVideoId(video.getId())
                                .setMaxResults(100L)
                                .setOrder(options.getCommentOrder().name())
                                .setPageToken(pageToken)
                                .execute();

                        getEstimatedQuota().incrementAndGet();

                        pageToken = response.getNextPageToken();

                        try {
                            // Maybe comments were re-enabled if we got a 403 in the past.
                            database.videos().updateHttpCode(video.getId(), 200);
                        } catch (SQLException sqle) {
                            logger.error("Failed to update video http response code", sqle);
                        }

                        final List<CommentThread> items = response.getItems();
                        if (items.isEmpty()) {
                            continue;
                        }

                        final List<YouTubeComment> comments = items.stream()
                                .map(YouTubeComment::from)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toList());
                        sendCollection(comments, YouTubeComment.class);

                        final List<YouTubeChannel> channels = items.stream()
                                .filter(distinctByKey(CommentThread::getId))
                                .map(YouTubeChannel::from)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toList());
                        sendCollection(channels, YouTubeChannel.class);

                        // Published-thread replies work differently from moderated/review-thread replies
                        // May or may not get all replies back, published threads should have up to 5 replies
                        final List<Comment> someReplies = items.stream()
                                .flatMap(thread -> Optional.ofNullable(thread.getReplies())
                                        .map(CommentThreadReplies::getComments)
                                        .map(Collection::stream)
                                        .orElse(Stream.empty()))
                                .collect(Collectors.toList());

                        final List<YouTubeComment> someReplyComments = someReplies.stream()
                                .map(reply -> YouTubeComment.from(reply, video.getId()))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toList());
                        sendCollection(someReplyComments, YouTubeComment.class);

                        final List<YouTubeChannel> someReplyChannels = someReplies.stream()
                                .map(YouTubeChannel::from)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .filter(distinctByKey(YouTubeChannel::getId))
                                .collect(Collectors.toList());
                        sendCollection(someReplyChannels, YouTubeChannel.class);

                        // Pass on the commentThreads that we couldn't get all replies from (threads with more than 5 replies)
                        final List<StringTuple> replyThreads = items.stream()
                                .filter(thread -> !thread.getSnippet().getTotalReplyCount().equals(Optional.ofNullable(thread.getReplies())
                                                .map(CommentThreadReplies::getComments)
                                                .map(List::size)
                                                .map(Long::valueOf)
                                                .orElse(0L)))
                                .map(thread -> new StringTuple(thread.getId(), video.getId()))
                                .collect(Collectors.toList());
                        sendCollection(replyThreads, StringTuple.class);

                        if (RefreshCommentOrder.TIME == options.getCommentOrder() && options.isCommentPagesSmart()) {
                            final List<String> threadIds = comments.stream()
                                    .map(YouTubeComment::getId)
                                    .collect(Collectors.toList());
                            if (database.countCommentsNotExisting(threadIds) == 0) {
                                logger.info("No new comment threads, stopping pagination early {}", video.getId());
                                break;
                            }
                        }

                        awaitMillis(50);
                    } while (pageToken != null && page++ < pages.getPageCount() && isNotHardShutdown());

                    logger.info("Completed {} {}", video.getId(), video.getTitle());

                    break;
                } catch (Exception e) {
                    if (e instanceof GoogleJsonResponseException) {
                        final GoogleJsonResponseException ge = (GoogleJsonResponseException) e;
                        final String reasonCode = getFirstReasonCode(ge);

                        try {
                            database.videos().updateHttpCode(video.getId(), ge.getStatusCode());
                        } catch (SQLException sqle) {
                            logger.error("Failed to update video http response code", sqle);
                        }

                        switch (reasonCode) {
                            case "quotaExceeded":
                                sendMessage(Level.FATAL, "API Quota Exceeded");
                                break threadLoop;

                            case "commentsDisabled":
                                final String disableMsg = String.format("Comments Disabled [videoId=%s]", video.getId());
                                sendMessage(Level.WARN, disableMsg);
                                break threadLoop;

                            case "forbidden":
                            case "channelNotFound":
                            case "commentThreadNotFound":
                            case "videoNotFound":
                                final String notFound = String.format("%s [videoId=%s]", reasonCode, video.getId());
                                sendMessage(Level.WARN, notFound);
                                break threadLoop;

                            default:
                                final String otherMsg = String.format("[%s/%s] %s [videoId=%s]",
                                        attempt,
                                        maxAttempts,
                                        reasonCode,
                                        video.getId());

                                sendMessage(Level.ERROR, e, otherMsg);
                                attempt++;
                                break;
                        }
                    } else {
                        final String message = String.format("[%s/%s] %s [videoId=%s]",
                                attempt,
                                maxAttempts,
                                e.getClass().getSimpleName(),
                                video.getId());

                        sendMessage(Level.ERROR, e, message);
                        attempt++;
                    }
                }
            } while (attempt <= maxAttempts && isNotHardShutdown());

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
