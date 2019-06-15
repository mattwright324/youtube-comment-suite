package io.mattw.youtube.commentsuite.db;

import com.google.api.client.util.ArrayMap;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;
import io.mattw.youtube.commentsuite.FXMLSuite;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Date;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class YouTubeComment extends YouTubeObject {

    private String text;
    private Date date;
    private String videoId;
    private String channelId = "";
    private long likes, replies;
    private boolean isReply;
    private String parentId;

    public YouTubeComment(Comment item, String videoId) {
        super(item.getId(), null, null, false);
        this.setTypeId(YType.COMMENT);
        this.text = item.getSnippet().getTextDisplay();
        this.date = new Date(item.getSnippet().getPublishedAt().getValue());
        this.videoId = videoId; // this.videoId = item.snippet.videoId;
        this.likes = item.getSnippet().getLikeCount();
        this.replies = -1;
        this.isReply = true;
        this.parentId = item.getSnippet().getParentId();
        if(item.getSnippet().getAuthorChannelId() != null) {
            this.channelId = getChannelIdFromObject(item.getSnippet().getAuthorChannelId());
        } else {
            System.out.println("Null channel");
        }
    }

    public YouTubeComment(CommentThread item) {
        super(item.getId(), null, null, false);
        this.setTypeId(YType.COMMENT);
        this.videoId = item.getSnippet().getVideoId();
        this.replies = item.getSnippet().getTotalReplyCount();
        Comment tlc = item.getSnippet().getTopLevelComment();
        this.text = tlc.getSnippet().getTextDisplay();
        this.date = new Date(tlc.getSnippet().getPublishedAt().getValue());
        this.likes = tlc.getSnippet().getLikeCount();
        this.isReply = false;
        this.parentId = null;
        if(tlc.getSnippet().getAuthorChannelId() != null) {
            this.channelId = getChannelIdFromObject(tlc.getSnippet().getAuthorChannelId());
        } else {
           System.out.println("Null channel");
        }
    }

    /**
     * Used for database init.
     */
    public YouTubeComment(String commentId, String text, long date, String videoId, String channelId, int likes, int replies, boolean isReply, String parentId) {
        super(commentId, null, null, false);
        this.setTypeId(YType.COMMENT);
        this.text = text;
        this.date = new Date(date);
        this.videoId = videoId;
        this.channelId = channelId;
        this.likes = likes;
        this.replies = replies;
        this.isReply = isReply;
        this.parentId = parentId;
    }

    public String getText() { return text; }
    public Date getDate() { return date; }
    public String getVideoId() { return videoId; }
    public String getChannelId() { return channelId; }
    public YouTubeChannel getChannel() { return FXMLSuite.getDatabase().getChannel(channelId); }
    public long getLikes() { return likes; }
    public long getReplyCount() { return replies; }
    public boolean isReply() { return isReply; }
    public String getParentId() { return parentId; }

    /**
     * Processes text for cleaner output when displayed in show/reply modal and comment list view when searching.
     *  - Replaces html line breaks with newline characters.
     *  - Removes Zalgo characters (https://codegolf.stackexchange.com/a/142699)
     *  - Unescapes HTML-escaped characters
     *
     * @return cleaned text
     */
    public String getCleanText(boolean withNewLines) {
        return StringEscapeUtils.unescapeHtml4(text)
                .replace("<br />", withNewLines ? "\r\n" : " ")
                .replaceAll("[̀-ͯ᪰-᫿᷀-᷿⃐-⃿︠-︯]","");
    }

    /**
     * For some reason this value returns as an ArrayMap? Cast and return correct value from it.
     */
    public String getChannelIdFromObject(Object authorChannelId) {
        if(authorChannelId instanceof ArrayMap) {
            ArrayMap<String,String> value = (ArrayMap) authorChannelId;

            return value.get("value");
        }
        return authorChannelId.toString();
    }
}
