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
import static org.apache.commons.lang3.StringUtils.isBlank;

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

        return Optional.ofNullable(configData.getAccount(channelId))
                .map(YouTubeAccount::getTokens)
                .map(OAuth2Tokens::getAccessToken)
                .orElse(null);
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
        if (pages == RefreshCommentPages.NONE) {
            logger.debug("Skipping CommentThreadProducer {} pages=NONE", moderationStatus);
            addProcessed(getBlockingQueue().size());
            getBlockingQueue().clear();
            return;
        }

        logger.debug("Starting CommentThreadProducer {}", moderationStatus);

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

            int attempt = 1;
            CommentThreadListResponse response;
            String pageToken = "";
            int page = 1;
            final int maxAttempts = 5;

            threadLoop:
            do {
                try {
                    do {
                        logger.info("{} Try #{} {} {}", moderationStatus, attempt, video.getId(), video.getTitle());

                        final String oauthToken = getOauthToken(video.getChannelId());
                        response = youTube.commentThreads()
                                .list(moderationStatus.getPart())
                                .setKey(CommentSuite.getYouTubeApiKey())
                                .setOauthToken(oauthToken)
                                .setVideoId(video.getId())
                                .setMaxResults(100L)
                                .setOrder(options.getCommentOrder().name())
                                .setPageToken(pageToken)
                                .setModerationStatus(moderationStatus.getApiValue())
                                .execute();

                        pageToken = response.getNextPageToken();

                        if (moderationStatus != PUBLISHED) {
                            logger.debug(response);
                        }

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
                                .filter(comment -> StringUtils.isNotEmpty(comment.getChannelId()))
                                .collect(Collectors.toList());
                        sendCollection(comments, YouTubeComment.class);

                        final List<YouTubeChannel> channels = items.stream()
                                .filter(distinctByKey(CommentThread::getId))
                                .map(YouTubeChannel::new)
                                .collect(Collectors.toList());
                        sendCollection(channels, YouTubeChannel.class);

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

                        if (moderationStatus != PUBLISHED) {
                            final List<Comment> threadReplies = items.stream()
                                    .map(CommentThread::getReplies)
                                    .map(CommentThreadReplies::getComments)
                                    .flatMap(List::stream)
                                    .collect(Collectors.toList());

                            final List<YouTubeComment> replies = threadReplies.stream()
                                    .map(comment -> new YouTubeComment(comment, video.getId()))
                                    .filter(comment -> StringUtils.isNotEmpty(comment.getChannelId()))
                                    .collect(Collectors.toList());

                            sendCollection(replies, YouTubeComment.class);

                            final List<YouTubeChannel> channels2 = threadReplies.stream()
                                    .filter(distinctByKey(Comment::getId))
                                    .map(YouTubeChannel::new)
                                    .collect(Collectors.toList());
                            sendCollection(channels2, YouTubeChannel.class);
                        }

                        awaitMillis(50);
                    } while (pageToken != null && page++ < pages.getPageCount() && !isHardShutdown());

                    logger.info("{} Completed {} {}", moderationStatus, video.getId(), video.getTitle());

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
                                if (moderationStatus == PUBLISHED) {
                                    sendMessage(Level.FATAL, "API Quota Exceeded");
                                    break threadLoop;
                                }


                                final String authQuotaMsg = String.format("[%s/%s] Auth Quota Exceeded  [videoId=%s]",
                                        attempt,
                                        maxAttempts,
                                        video.getId());

                                sendMessage(Level.ERROR, authQuotaMsg);
                                awaitMillis(15000);

                                attempt++;
                                break;

                            case "authError":
                                final String authMsg = String.format("[%s/%s] Authorization failed [videoId=%s]",
                                        attempt,
                                        maxAttempts,
                                        video.getId());

                                sendMessage(Level.WARN, authMsg);
                                sendMessage(Level.WARN, "Trying to refresh Oauth2 access token");

                                refreshOauth2(video.getChannelId());

                                // For some reason without a few seconds wait the 'Auth Quota Exceeded'
                                // error above will occur on the next request despite using the new token.
                                awaitMillis(15000);

                                attempt++;
                                break;

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
            } while (attempt <= maxAttempts && !isHardShutdown());

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
