package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.math.BigInteger;
import java.util.Optional;

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

        logger.debug(ReflectionToStringBuilder.toString(item));

        if(item.getSnippet() != null) {
            VideoSnippet snippet = item.getSnippet();

            this.channelId = snippet.getChannelId();
            this.description = snippet.getDescription();
            this.publishDate = snippet.getPublishedAt().getValue();
        }

        if(item.getStatistics() != null) {
            VideoStatistics stats = item.getStatistics();

            this.viewCount = stats.getViewCount().longValue();

            // Likes and dislikes may be disabled on the video
            this.likes = Optional.ofNullable(stats.getLikeCount())
                    .orElse(BigInteger.ZERO)
                    .longValue();
            this.dislikes = Optional.ofNullable(stats.getDislikeCount())
                    .orElse(BigInteger.ZERO)
                    .longValue();

            // When comments are disabled, this value will be null on the video.
            // httpCode should also be 403 when when comment threads are grabbed during refresh.
            this.comments = Optional.ofNullable(stats.getCommentCount())
                                    .orElse(BigInteger.ZERO)
                                    .longValue();
        }

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
