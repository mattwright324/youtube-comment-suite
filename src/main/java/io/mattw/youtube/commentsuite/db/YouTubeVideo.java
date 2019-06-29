package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Video;

/**
 * @author mattwright324
 */
public class YouTubeVideo extends YouTubeObject {

    private String channelId;
    private String description;
    private long publishDate;
    private long refreshedOnDate;
    private long viewCount;
    private long comments;
    private long likes;
    private long dislikes;
    private int httpCode;

    // Field(s) used just for export to make things pretty.
    private YouTubeChannel author;

    public YouTubeVideo(Video item) {
        super(item.getId(), item.getSnippet().getTitle(), item.getSnippet().getThumbnails().getMedium().getUrl());
        setTypeId(YType.VIDEO);

        this.channelId = item.getSnippet().getChannelId();
        this.description = item.getSnippet().getDescription();
        this.publishDate = item.getSnippet().getPublishedAt().getValue();
        this.viewCount = item.getStatistics().getViewCount().longValue();
        this.likes = item.getStatistics().getLikeCount().longValue();
        this.dislikes = item.getStatistics().getDislikeCount().longValue();
        this.comments = item.getStatistics().getCommentCount().longValue();
        this.refreshedOnDate = System.currentTimeMillis();
    }

    /**
     * Used for database init.
     */
    public YouTubeVideo(String videoId, String channelId, String title, String description, String thumbUrl, long publishDate, long grabDate, long comments, long likes, long dislikes, long viewCount, int httpCode) {
        super(videoId, title, thumbUrl);
        setTypeId(YType.VIDEO);

        this.channelId = channelId;
        this.description = description;
        this.publishDate = publishDate;
        this.refreshedOnDate = grabDate;
        this.comments = comments;
        this.likes = likes;
        this.dislikes = dislikes;
        this.viewCount = viewCount;
        this.httpCode = httpCode;
    }

    /**
     * Overwrite channelId as null when set because it will be on the channel object for export.
     */
    public YouTubeVideo setAuthor(YouTubeChannel author) {
        this.channelId = null;
        this.author = author;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public String getChannelId() {
        return channelId;
    }

    public long getPublishedDate() {
        return publishDate;
    }

    public long getRefreshedOnDate() {
        return refreshedOnDate;
    }

    public void setRefreshedOnDate(long epochMillis) {
        this.refreshedOnDate = epochMillis;
    }

    public long getCommentCount() {
        return comments;
    }

    public long getLikes() {
        return likes;
    }

    public long getDislikes() {
        return dislikes;
    }

    public long getViewCount() {
        return viewCount;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String toString() {
        return getTitle();
    }
}
