package io.mattw.youtube.commentsuite.refresh;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.CommentThreadReplies;
import io.mattw.youtube.commentsuite.*;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
import io.mattw.youtube.commentsuite.db.YouTubeVideo;
import io.mattw.youtube.commentsuite.oauth2.OAuth2Manager;
import io.mattw.youtube.commentsuite.oauth2.OAuth2Tokens;
import io.mattw.youtube.commentsuite.oauth2.YouTubeAccount;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import io.mattw.youtube.commentsuite.util.StringTuple;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.mattw.youtube.commentsuite.refresh.ModerationStatus.PUBLISHED;

public class CommentThreadProducer extends ConsumerMultiProducer<YouTubeVideo> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(10);

    private final RefreshOptions options;
    private final RefreshCommentPages pages;
    private final YouTube youTube;
    private final CommentDatabase database;
    private final ModerationStatus moderationStatus;
    private final ConfigData configData = CommentSuite.getConfig().getDataObject();
    private final OAuth2Manager oAuth2Manager = CommentSuite.getOauth2Manager();

    public CommentThreadProducer(final RefreshOptions options, final RefreshCommentPages pages) {
        this(options, pages, PUBLISHED);
    }

    public CommentThreadProducer(final RefreshOptions options, final RefreshCommentPages pages, final ModerationStatus moderationStatus) {
        this.options = options;
        this.pages = pages;
        this.moderationStatus = moderationStatus;
        this.youTube = CommentSuite.getYouTube();
        this.database = CommentSuite.getDatabase();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private String getOauthToken(String channelId) {
        if (moderationStatus == PUBLISHED) {
            return null;
        }

        final YouTubeAccount account = configData.getAccount(channelId);
        final String accessToken = Optional.ofNullable(account)
                .map(YouTubeAccount::getTokens)
                .map(OAuth2Tokens::getAccessToken)
                .orElse(null);

        logger.debug(accessToken);

        return accessToken;
    }

    private void refreshOauth2(final String channelId) {
        if (moderationStatus == PUBLISHED) {
            return;
        }

        logger.debug("Refreshing OAuth2 Tokens for {}", channelId);

        try {
            oAuth2Manager.getNewAccessToken(configData.getAccount(channelId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void produce() {
        logger.debug("Starting CommentThreadProducer " + moderationStatus);

        while (shouldKeepAlive()) {
            final YouTubeVideo video = getBlockingQueue().poll();
            if (video == null) {
                awaitMillis(100);
                continue;
            }

            if (moderationStatus != PUBLISHED && !configData.isSignedIn(video.getChannelId())) {
                logger.warn("Authorization required for {} commentThreads on {} but not signed in", moderationStatus, video.getId());
                awaitMillis(100);
                continue;
            }

            send(video.getChannelId());

            int attempts = 1;
            CommentThreadListResponse response;
            String pageToken = "";
            int page = 1;
            final int maxAttempts = 5;
            do {
                try {
                    logger.info("{} - {} {}", video.getId(), moderationStatus, video.getTitle());
                    do {
                        logger.debug("{} {}", moderationStatus, attempts);

                        response = youTube.commentThreads()
                                .list(moderationStatus.getPart())
                                .setKey(CommentSuite.getYouTubeApiKey())
                                .setOauthToken(getOauthToken(video.getChannelId()))
                                .setVideoId(video.getId())
                                .setMaxResults(100L)
                                .setOrder(options.getCommentOrder().name())
                                .setPageToken(pageToken)
                                .setModerationStatus(moderationStatus.getApiValue())
                                .execute();

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
                                .map(YouTubeComment::new)
                                .filter(comment -> StringUtils.isNotEmpty(comment.getChannelId()) /* filter out G+ comments */)
                                .collect(Collectors.toList());
                        sendCollection(comments, YouTubeComment.class);

                        if (moderationStatus != PUBLISHED) {
                            final List<YouTubeComment> replies = items.stream()
                                    .map(CommentThread::getReplies)
                                    .map(CommentThreadReplies::getComments)
                                    .flatMap(List::stream)
                                    .map(comment -> new YouTubeComment(comment, video.getId()))
                                    .peek(comment -> comment.setModerationStatus(moderationStatus))
                                    .filter(comment -> StringUtils.isNotEmpty(comment.getChannelId()))
                                    .collect(Collectors.toList());

                            sendCollection(replies, YouTubeComment.class);
                        }

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
                                .filter(comment -> options.getReplyPages() != RefreshCommentPages.NONE)
                                .map(comment -> new StringTuple(comment.getId(), video.getId()))
                                .collect(Collectors.toList());
                        sendCollection(replyThreads, StringTuple.class);

                        awaitMillis(50);
                    } while (pageToken != null && page++ < pages.getPageCount() && !isHardShutdown());

                    break;
                } catch (Exception e) {
                    if (e instanceof GoogleJsonResponseException) {
                        final GoogleJsonResponseException ge = (GoogleJsonResponseException) e;

                        try {
                            database.videos().updateHttpCode(video.getId(), ge.getStatusCode());
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

                            sendMessage(Level.ERROR, e, message);

                            attempts++;
                        } else if (ge.getStatusCode() == 401 && moderationStatus != PUBLISHED) {
                            final String message = String.format("[%s/%s] Authorization failed [videoId=%s]",
                                    attempts,
                                    maxAttempts,
                                    video.getId());

                            sendMessage(Level.WARN, message);
                            sendMessage(Level.WARN, "Trying to refresh Oauth2 access token");

                            refreshOauth2(video.getChannelId());
                            CommentSuite.getConfig().save();

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

                            sendMessage(Level.ERROR, e, message);

                            break;
                        }
                    } else {
                        final String message = String.format("[%s/%s] %s [videoId=%s]",
                                attempts,
                                maxAttempts,
                                e.getClass().getSimpleName(),
                                video.getId());

                        sendMessage(Level.ERROR, e, message);
                        attempts++;
                    }
                }
            } while (attempts <= maxAttempts && !isHardShutdown());

            addProcessed(1);
            awaitMillis(500);
        }

        logger.debug("Ending CommentThreadProducer " + moderationStatus);
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
