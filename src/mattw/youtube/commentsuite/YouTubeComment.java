package mattw.youtube.commentsuite;

import mattw.youtube.datav3.resources.CommentThreadsList;
import mattw.youtube.datav3.resources.CommentsList;

import java.util.Date;

public class YouTubeComment extends YouTubeObject {

    private String text;
    private Date date;
    private String videoId;
    private String channelId = "";
    private int likes, replies;
    private boolean isReply;
    private String parentId;

    public YouTubeComment(CommentsList.Item item, String videoId) {
        super(item.getId(), null, null, false);
        this.typeId = 3;
        this.text = item.snippet.textDisplay;
        this.date = item.snippet.publishedAt;
        this.videoId = videoId; // this.videoId = item.snippet.videoId;
        this.likes = item.snippet.likeCount;
        this.replies = -1;
        this.isReply = true;
        this.parentId = item.snippet.parentId;
        if(item.snippet.authorChannelId != null && item.snippet.authorChannelId.value != null) {
            this.channelId = item.snippet.authorChannelId.value;
        } else {
            System.out.println("Null channel");
        }
    }

    public YouTubeComment(CommentThreadsList.Item item) {
        super(item.getId(), null, null, false);
        this.typeId = 3;
        this.videoId = item.snippet.videoId;
        this.replies = item.snippet.totalReplyCount;
        CommentsList.Item tlc = item.snippet.topLevelComment;
        this.text = tlc.snippet.textDisplay;
        this.date = tlc.snippet.publishedAt;
        this.likes = tlc.snippet.likeCount;
        this.isReply = false;
        this.parentId = null;
        if(tlc.snippet.authorChannelId != null && tlc.snippet.authorChannelId.value != null) {
            this.channelId = tlc.snippet.authorChannelId.value;
        } else {
           System.out.println("Null channel");
        }
    }

    /**
     * Used for database init.
     */
    public YouTubeComment(String commentId, String text, long date, String videoId, String channelId, int likes, int replies, boolean isReply, String parentId) {
        super(commentId, null, null, false);
        this.typeId = 3;
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
    public YouTubeChannel getChannel() { return CommentSuite.db().getChannel(channelId); }
    public int getLikes() { return likes; }
    public int getReplyCount() { return replies; }
    public boolean isReply() { return isReply; }
    public String getParentId() { return parentId; }
}
