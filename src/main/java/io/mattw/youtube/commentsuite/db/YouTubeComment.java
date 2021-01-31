package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadSnippet;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.refresh.ModerationStatus;
import io.mattw.youtube.commentsuite.util.DateUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.List;

public class YouTubeComment extends YouTubeObject implements Exportable {

    public static final Logger logger = LogManager.getLogger();

    private String commentText;
    private String channelId;
    private transient long published;
    private transient LocalDateTime publishedDateTime;
    private String commentDate;
    private String videoId;
    private long likes, replyCount;
    private boolean isReply;
    private String parentId;
    private ModerationStatus moderationStatus;

    // Field(s) used just for export to make things pretty.
    private YouTubeChannel author;
    private List<YouTubeComment> replies;

    /**
     * Constructor meant for replies.
     */
    public YouTubeComment(Comment item, String videoId) {
        super(item.getId(), null, null);
        this.setTypeId(YType.COMMENT);

        this.videoId = videoId;
        this.isReply = true;
        this.replyCount = -1;

        CommentSnippet snippet = item.getSnippet();
        this.commentText = snippet.getTextDisplay();
        this.published = snippet.getPublishedAt().getValue();
        this.publishedDateTime = DateUtils.epochMillisToDateTime(published);
        this.likes = snippet.getLikeCount();
        this.parentId = snippet.getParentId();

        if (snippet.getAuthorChannelId() != null) {
            this.channelId = getChannelIdFromObject(snippet.getAuthorChannelId());
        } else {
            logger.warn("There was no authorChannelId {}", ReflectionToStringBuilder.toString(item));
        }
    }

    /**
     * Constructor mean for top-level comments.
     */
    public YouTubeComment(CommentThread item) {
        super(item.getId(), null, null);
        this.setTypeId(YType.COMMENT);

        this.isReply = false;
        this.parentId = null;

        CommentThreadSnippet snippet = item.getSnippet();
        this.videoId = snippet.getVideoId();
        this.replyCount = snippet.getTotalReplyCount();

        CommentSnippet tlcSnippet = snippet.getTopLevelComment().getSnippet();
        this.commentText = tlcSnippet.getTextDisplay();
        this.published = tlcSnippet.getPublishedAt().getValue();
        this.publishedDateTime = DateUtils.epochMillisToDateTime(published);
        this.likes = tlcSnippet.getLikeCount();
        this.moderationStatus = ModerationStatus.fromApiValue(tlcSnippet.getModerationStatus());

        if (tlcSnippet.getAuthorChannelId() != null) {
            this.channelId = getChannelIdFromObject(tlcSnippet.getAuthorChannelId());
        } else {
            logger.warn("There was no authorChannelId {}", ReflectionToStringBuilder.toString(item));
        }
    }


    public YouTubeComment(String commentId) {
        super(commentId, null, null);
        setTypeId(YType.COMMENT);
        setModerationStatus(ModerationStatus.PUBLISHED);
    }

    public YouTubeChannel getChannel() {
        return CommentSuite.getDatabase().channels().getOrNull(channelId);
    }

    @Override
    public void prepForExport() {
        commentDate = publishedDateTime.toString();
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

    public ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public YouTubeComment setModerationStatus(String moderationStatus) {
        this.moderationStatus = ModerationStatus.fromApiValue(moderationStatus);
        return this;
    }

    public YouTubeComment setModerationStatus(ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
        return this;
    }

    public YouTubeChannel getAuthor() {
        return author;
    }

    public List<YouTubeComment> getReplies() {
        return replies;
    }

    /**
     * Overwrite channelId as null when set because it will be on the channel object for export.
     */
    public YouTubeComment setAuthor(YouTubeChannel author) {
        this.channelId = null;
        this.author = author;
        return this;
    }

    /**
     * List of comment replies to this parent comment.
     */
    public YouTubeComment setReplies(List<YouTubeComment> replies) {
        this.replies = replies;
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
                .replace("<br />", withNewLines ? "\r\n" : " ")
                .replaceAll("[̀-ͯ᪰-᫿᷀-᷿⃐-⃿︠-︯]", "");
    }
}
