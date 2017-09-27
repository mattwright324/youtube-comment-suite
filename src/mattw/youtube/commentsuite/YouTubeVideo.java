package mattw.youtube.commentsuite;

import mattw.youtube.datav3.resources.VideosList;

public class YouTubeVideo extends YouTubeObject {

    private String channelId;
    private String description;
    private long publishDate, grabDate;
    private long comments, likes, dislikes, views;
    private int httpCode;

    public YouTubeVideo(VideosList.Item itemSnip, VideosList.Item itemStat) {
        super(itemSnip.getId(), itemSnip.snippet.title, itemSnip.snippet.thumbnails.medium.url.toString(), false);
        this.typeId = 0;
        this.channelId = itemSnip.snippet.channelId;
        this.description = itemSnip.snippet.description;
        this.publishDate = itemSnip.snippet.publishedAt.getTime();
        this.views = itemStat.statistics.viewCount;
        this.likes = itemStat.statistics.likeCount;
        this.dislikes = itemStat.statistics.dislikeCount;
        this.comments = itemStat.statistics.commentCount;
        this.grabDate = System.currentTimeMillis();
    }

    /**
     * Used for database init.
     */
    public YouTubeVideo(String videoId, String channelId, String title, String description, String thumbUrl, long publishDate, long grabDate, long comments, long likes, long dislikes, long views, int httpCode) {
        super(videoId, title, thumbUrl, false);
        this.typeId = 0;
        this.channelId = channelId;
        this.description = description;
        this.publishDate = publishDate;
        this.grabDate = grabDate;
        this.comments = comments;
        this.likes = likes;
        this.dislikes = dislikes;
        this.views = views;
        this.httpCode = httpCode;
    }

    public String getDescription() { return description; }
    public String getChannelId() { return channelId; }
    public YouTubeChannel getChannel() { return CommentSuite.db().getChannel(channelId); }
    public long getPublishedDate() { return publishDate; }
    public long getLastGrabDate() { return grabDate; }
    public long getCommentCount() { return comments; }
    public long getLikes() { return likes; }
    public long getDislikes() { return dislikes; }
    public long getViews() { return views; }
    public int getHttpCode() { return httpCode; }

    public void setGrabDate(long time) { this.grabDate = time; }

    public String toString() { return getTitle(); }
}
