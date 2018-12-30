package mattw.youtube.commentsuite.db;

import mattw.youtube.datav3.entrypoints.VideosList;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class YouTubeVideo extends YouTubeObject {

    private String channelId;
    private String description;
    private long publishDate, grabDate;
    private long comments, likes, dislikes, views;
    private int httpCode;

    public YouTubeVideo(VideosList.Item item) {
        super(item.getId(), item.snippet.title, item.snippet.thumbnails.getMedium().getURL().toString(), true);
        setTypeId(YType.VIDEO);
        this.channelId = item.snippet.channelId;
        this.description = item.snippet.description;
        this.publishDate = item.snippet.publishedAt.getTime();
        this.views = item.statistics.viewCount;
        this.likes = item.statistics.likeCount;
        this.dislikes = item.statistics.dislikeCount;
        this.comments = item.statistics.commentCount;
        this.grabDate = System.currentTimeMillis();
    }

    @Deprecated
    public YouTubeVideo(VideosList.Item itemSnip, VideosList.Item itemStat) {
        super(itemSnip.getId(), itemSnip.snippet.title, itemSnip.snippet.thumbnails.getMedium().getURL().toString(), true);
        setTypeId(YType.VIDEO);
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
        setTypeId(YType.VIDEO);
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
    // public YouTubeChannel getChannel() { return CommentSuite.db().getChannel(channelId); }
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
