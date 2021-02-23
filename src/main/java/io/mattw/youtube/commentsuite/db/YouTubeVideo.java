package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;
import io.mattw.youtube.commentsuite.util.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Optional;

public class YouTubeVideo extends YouTubeObject implements Exportable {

    private static final Logger logger = LogManager.getLogger();

    private String channelId;
    private String description;
    private transient long published;
    private String publishDate;
    private transient long refreshedOn;
    private String refreshedOnDate;
    private long viewCount;
    private long comments;
    private long likes;
    private long dislikes;
    private int responseCode;

    // Field(s) used just for export to make things pretty.
    private YouTubeChannel author;

    public YouTubeVideo(String id, String title, String thumbUrl) {
        super(id, title, thumbUrl);
        setTypeId(GroupItemType.VIDEO);
    }

    /**
     * Used for refreshing
     *
     * @param item YouTube video object with snippet and statistics parts present
     */
    public YouTubeVideo(Video item) {
        super(item.getId(), item.getSnippet().getTitle(), item.getSnippet().getThumbnails().getMedium().getUrl());
        setTypeId(GroupItemType.VIDEO);

        if (item.getSnippet() != null) {
            VideoSnippet snippet = item.getSnippet();

            this.channelId = snippet.getChannelId();
            this.description = snippet.getDescription();
            this.published = snippet.getPublishedAt().getValue();
        } else {
            logger.warn("Video snippet is null");
        }

        if (item.getStatistics() != null) {
            VideoStatistics stats = item.getStatistics();

            this.viewCount = Optional.ofNullable(stats.getViewCount())
                    .orElse(BigInteger.ZERO)
                    .longValue();

            // Likes and dislikes may be disabled on the video
            this.likes = Optional.ofNullable(stats.getLikeCount())
                    .orElse(BigInteger.ZERO)
                    .longValue();
            this.dislikes = Optional.ofNullable(stats.getDislikeCount())
                    .orElse(BigInteger.ZERO)
                    .longValue();

            // When comments are disabled, this value will be null on the video.
            // responseCode should also be 403 when when comment threads are grabbed during refresh.
            this.comments = Optional.ofNullable(stats.getCommentCount())
                    .orElse(BigInteger.ZERO)
                    .longValue();
        } else {
            logger.warn("Video statistics is null");
        }

        this.refreshedOn = System.currentTimeMillis();
    }

    @Override
    public void prepForExport() {
        channelId = null;
        publishDate = DateUtils.epochMillisToDateTime(published).toString();
        refreshedOnDate = DateUtils.epochMillisToDateTime(refreshedOn).toString();
    }

    public String getChannelId() {
        return channelId;
    }

    public YouTubeVideo setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public YouTubeVideo setDescription(String description) {
        this.description = description;
        return this;
    }

    public long getPublished() {
        return published;
    }

    public YouTubeVideo setPublished(long published) {
        this.published = published;
        return this;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public YouTubeVideo setPublishDate(String publishDate) {
        this.publishDate = publishDate;
        return this;
    }

    public long getRefreshedOn() {
        return refreshedOn;
    }

    public YouTubeVideo setRefreshedOn(long refreshedOn) {
        this.refreshedOn = refreshedOn;
        return this;
    }

    public String getRefreshedOnDate() {
        return refreshedOnDate;
    }

    public YouTubeVideo setRefreshedOnDate(String refreshedOnDate) {
        this.refreshedOnDate = refreshedOnDate;
        return this;
    }

    public long getViewCount() {
        return viewCount;
    }

    public YouTubeVideo setViewCount(long viewCount) {
        this.viewCount = viewCount;
        return this;
    }

    public long getComments() {
        return comments;
    }

    public YouTubeVideo setComments(long comments) {
        this.comments = comments;
        return this;
    }

    public long getLikes() {
        return likes;
    }

    public YouTubeVideo setLikes(long likes) {
        this.likes = likes;
        return this;
    }

    public long getDislikes() {
        return dislikes;
    }

    public YouTubeVideo setDislikes(long dislikes) {
        this.dislikes = dislikes;
        return this;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public YouTubeVideo setResponseCode(int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    public YouTubeChannel getAuthor() {
        return author;
    }

    public YouTubeVideo setAuthor(YouTubeChannel author) {
        this.author = author;
        return this;
    }

    /**
     * @return title of the video
     */
    public String toString() {
        return getTitle();
    }
}
