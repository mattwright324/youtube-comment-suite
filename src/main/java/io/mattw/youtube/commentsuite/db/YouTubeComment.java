package io.mattw.youtube.commentsuite.db;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadSnippet;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.util.DateUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class YouTubeComment implements Linkable {

    public static final Logger logger = LogManager.getLogger();

    private String id;
    private YouTubeType type = YouTubeType.COMMENT;
    private String commentText;
    private String channelId;
    private transient long published;
    private transient LocalDateTime publishedDateTime;
    private String commentDate;
    private String videoId;
    private long likes, replyCount;
    private boolean isReply;
    private String parentId;
    private final String moderationStatus = "published";
    private List<String> tags;

    public YouTubeChannel getChannel() {
        return CommentSuite.getDatabase().channels().getOrNull(channelId);
    }

    public String getId() {
        return id;
    }

    public YouTubeComment setId(String id) {
        this.id = id;
        return this;
    }

    public YouTubeType getType() {
        return type;
    }

    public YouTubeComment setType(YouTubeType type) {
        this.type = type;
        return this;
    }

    public String getCommentText() {
        return commentText;
    }

    public YouTubeComment setCommentText(String commentText) {
        this.commentText = commentText;
        return this;
    }

    public String getChannelId() {
        return channelId;
    }

    public YouTubeComment setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public long getPublished() {
        return published;
    }

    public YouTubeComment setPublished(long published) {
        this.published = published;
        setPublishedDateTime(DateUtils.epochMillisToDateTime(published));
        return this;
    }

    public LocalDateTime getPublishedDateTime() {
        return publishedDateTime;
    }

    public YouTubeComment setPublishedDateTime(LocalDateTime publishedDateTime) {
        this.publishedDateTime = publishedDateTime;
        return this;
    }

    public String getCommentDate() {
        return commentDate;
    }

    public YouTubeComment setCommentDate(String commentDate) {
        this.commentDate = commentDate;
        return this;
    }

    public String getVideoId() {
        return videoId;
    }

    public YouTubeComment setVideoId(String videoId) {
        this.videoId = videoId;
        return this;
    }

    public long getLikes() {
        return likes;
    }

    public YouTubeComment setLikes(long likes) {
        this.likes = likes;
        return this;
    }

    public long getReplyCount() {
        return replyCount;
    }

    public YouTubeComment setReplyCount(long replyCount) {
        this.replyCount = replyCount;
        return this;
    }

    public boolean isReply() {
        return isReply;
    }

    public YouTubeComment setReply(boolean reply) {
        isReply = reply;
        return this;
    }

    public String getParentId() {
        return parentId;
    }

    public YouTubeComment setParentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getModerationStatus() {
        return moderationStatus;
    }

    public List<String> getTags() {
        return tags;
    }

    public YouTubeComment setTags(String tags) {
        if (tags != null) {
            this.tags = new ArrayList<>();
            this.tags.addAll(Arrays.asList(tags.split(",")));
        }
        return this;
    }

    public YouTubeComment setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Processes commentText for cleaner output when displayed in show/reply modal and comment list view when searching.
     * - Replaces html line breaks with newline characters.
     * - Removes Zalgo characters (https://codegolf.stackexchange.com/a/142699)
     * - Unescapes HTML-escaped characters
     *
     * @return cleaned commentText
     */
    public String getCleanText(boolean withNewLines) {
        return StringEscapeUtils.unescapeHtml4(commentText)
                .replaceAll("<br[ /]*>", withNewLines ? "\r\n" : " ")
                .replaceAll("[̀-ͯ᪰-᫿᷀-᷿⃐-⃿︠-︯]", "");
    }

    @Override
    public String toYouTubeLink() {
        return String.format("https://www.youtube.com/watch?v=%s&lc=%s", videoId, id);
    }

    public static Optional<YouTubeComment> from(final CommentThread commentThread) {
        final Comment topLevelComment = Optional.ofNullable(commentThread)
                .map(CommentThread::getSnippet)
                .map(CommentThreadSnippet::getTopLevelComment)
                .orElse(null);

        return from(commentThread, topLevelComment, null);
    }

    public static Optional<YouTubeComment> from(final Comment comment, final String videoId) {
        return from(null, comment, videoId);
    }

    private static Optional<YouTubeComment> from(final CommentThread commentThread, final Comment comment, final String videoId) {
        final String id = Optional.ofNullable(commentThread)
                .map(CommentThread::getId)
                .orElse(Optional.ofNullable(comment)
                        .map(Comment::getId)
                        .orElse(null));

        final boolean isReply = isNotBlank(videoId);
        final String video = Optional.ofNullable(videoId)
                .orElse(Optional.ofNullable(commentThread)
                        .map(CommentThread::getSnippet)
                        .map(CommentThreadSnippet::getVideoId)
                        .orElse(null));
        final long replyCount = Optional.ofNullable(commentThread)
                .map(CommentThread::getSnippet)
                .map(CommentThreadSnippet::getTotalReplyCount)
                .orElse(-1L);
        final Optional<CommentSnippet> snippet = Optional.ofNullable(comment)
                .map(Comment::getSnippet);
        final String commentText = snippet.map(CommentSnippet::getTextDisplay).orElse(null);
        final long likes = snippet.map(CommentSnippet::getLikeCount).orElse(0L);
        final String parentId = snippet.map(CommentSnippet::getParentId).orElse(null);
        final String channelId = snippet.map(CommentSnippet::getAuthorChannelId)
                .map(YouTubeChannel::getChannelIdFromObject)
                .orElse(null);
        final long published = snippet.map(CommentSnippet::getPublishedAt)
                .map(DateTime::getValue)
                .orElse(0L);

        if (isAnyBlank(id, channelId)) {
            logger.debug("Invalid commentThread={} comment={} videoId={}", commentThread, comment, videoId);
            return Optional.empty();
        }

        return Optional.of(new YouTubeComment()
                .setId(id)
                .setType(YouTubeType.COMMENT)
                .setCommentText(commentText)
                .setPublished(published)
                .setLikes(likes)
                .setParentId(parentId)
                .setChannelId(channelId)
                .setVideoId(video)
                .setReplyCount(replyCount)
                .setReply(isReply)
        );
    }

}
