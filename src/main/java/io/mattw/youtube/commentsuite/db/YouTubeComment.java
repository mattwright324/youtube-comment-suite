package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadSnippet;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.util.DateUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * @author mattwright324
 */
public class YouTubeComment extends YouTubeObject {

    public static final Logger logger = LogManager.getLogger();

    private String commentText;
    private String channelId;
    private transient long published;
    private String commentDate;
    private String videoId;
    private long likes, replyCount;
    private boolean isReply;
    private String parentId;

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
        this.likes = tlcSnippet.getLikeCount();

        if (tlcSnippet.getAuthorChannelId() != null) {
            this.channelId = getChannelIdFromObject(tlcSnippet.getAuthorChannelId());
        } else {
            logger.warn("There was no authorChannelId {}", ReflectionToStringBuilder.toString(item));
        }
    }

    /**
     * Constructor used for initialization from the database.
     */
    public YouTubeComment(String commentId, String text, long published, String videoId, String channelId, int likes, int replies, boolean isReply, String parentId) {
        super(commentId, null, null);
        this.setTypeId(YType.COMMENT);

        this.commentText = text;
        this.published = published;
        this.videoId = videoId;
        this.channelId = channelId;
        this.likes = likes;
        this.replyCount = replies;
        this.isReply = isReply;
        this.parentId = parentId;
    }

    public void prepForExport() {
        commentDate = DateUtils.epochMillisToDateTime(published).toString();
    }

    public String getCommentText() {
        return commentText;
    }

    public long getPublished() {
        return published;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getChannelId() {
        return channelId;
    }

    public YouTubeChannel getChannel() {
        return FXMLSuite.getDatabase().getChannel(channelId);
    }

    public long getLikes() {
        return likes;
    }

    public long getReplyCount() {
        return replyCount;
    }

    public boolean isReply() {
        return isReply;
    }

    public String getParentId() {
        return parentId;
    }

    public YouTubeChannel getAuthor() {
        return author;
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
