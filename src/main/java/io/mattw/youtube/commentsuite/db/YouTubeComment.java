package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;
import io.mattw.youtube.commentsuite.FXMLSuite;
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
    private long publishedAt;
    private String videoId;
    private long likes, replyCount;
    private boolean isReply;
    private String parentId;

    // Field(s) used just for export to make things pretty.
    private YouTubeChannel author;
    private List<YouTubeComment> replies;

    public YouTubeComment(Comment item, String videoId) {
        super(item.getId(), null, null);
        this.setTypeId(YType.COMMENT);
        this.commentText = item.getSnippet().getTextDisplay();
        this.publishedAt = item.getSnippet().getPublishedAt().getValue();
        this.videoId = videoId; // this.videoId = item.snippet.videoId;
        this.likes = item.getSnippet().getLikeCount();
        this.replyCount = -1;
        this.isReply = true;
        this.parentId = item.getSnippet().getParentId();

        if (item.getSnippet().getAuthorChannelId() != null) {
            this.channelId = getChannelIdFromObject(item.getSnippet().getAuthorChannelId());
        } else {
            logger.debug("Null channel {}", item);
        }
    }

    public YouTubeComment(CommentThread item) {
        super(item.getId(), null, null);
        this.setTypeId(YType.COMMENT);
        this.videoId = item.getSnippet().getVideoId();
        this.replyCount = item.getSnippet().getTotalReplyCount();
        Comment tlc = item.getSnippet().getTopLevelComment();
        this.commentText = tlc.getSnippet().getTextDisplay();
        this.publishedAt = tlc.getSnippet().getPublishedAt().getValue();
        this.likes = tlc.getSnippet().getLikeCount();
        this.isReply = false;
        this.parentId = null;
        if (tlc.getSnippet().getAuthorChannelId() != null) {
            this.channelId = getChannelIdFromObject(tlc.getSnippet().getAuthorChannelId());
        } else {
            logger.debug("Null channel {}", item);
        }
    }

    /**
     * Used for database init.
     */
    public YouTubeComment(String commentId, String text, long publishedAt, String videoId, String channelId, int likes, int replies, boolean isReply, String parentId) {
        super(commentId, null, null);
        this.setTypeId(YType.COMMENT);
        this.commentText = text;
        this.publishedAt = publishedAt;
        this.videoId = videoId;
        this.channelId = channelId;
        this.likes = likes;
        this.replyCount = replies;
        this.isReply = isReply;
        this.parentId = parentId;
    }

    public String getCommentText() {
        return commentText;
    }

    public long getPublishedAt() {
        return publishedAt;
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
