package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Video;

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

    public YouTubeVideo(Video item) {
        super(item.getId(), item.getSnippet().getTitle(), item.getSnippet().getThumbnails().getMedium().getUrl().toString(), true);
        setTypeId(YType.VIDEO);
        this.channelId = item.getSnippet().getChannelId();
        this.description = item.getSnippet().getDescription();
        this.publishDate = item.getSnippet().getPublishedAt().getValue();
        this.views = item.getStatistics().getViewCount().longValue();
        this.likes = item.getStatistics().getLikeCount().longValue();
        this.dislikes = item.getStatistics().getDislikeCount().longValue();
        this.comments = item.getStatistics().getCommentCount().longValue();
        this.grabDate = System.currentTimeMillis();
    }

    @Deprecated
    public YouTubeVideo(Video itemSnip, Video itemStat) {
        super(itemSnip.getId(), itemSnip.getSnippet().getTitle(), itemSnip.getSnippet().getThumbnails().getMedium().getUrl().toString(), true);
        setTypeId(YType.VIDEO);
        this.channelId = itemSnip.getSnippet().getChannelId();
        this.description = itemSnip.getSnippet().getDescription();
        this.publishDate = itemSnip.getSnippet().getPublishedAt().getValue();
        this.views = itemStat.getStatistics().getViewCount().longValue();
        this.likes = itemStat.getStatistics().getLikeCount().longValue();
        this.dislikes = itemStat.getStatistics().getDislikeCount().longValue();
        this.comments = itemStat.getStatistics().getCommentCount().longValue();
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
