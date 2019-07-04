package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;
import io.mattw.youtube.commentsuite.util.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Optional;

/**
 * @author mattwright324
 */
public class YouTubeVideo extends YouTubeObject {

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

    public YouTubeVideo(Video item) {
        super(item.getId(), item.getSnippet().getTitle(), item.getSnippet().getThumbnails().getMedium().getUrl());
        setTypeId(YType.VIDEO);

        if (item.getSnippet() != null) {
            VideoSnippet snippet = item.getSnippet();

            this.channelId = snippet.getChannelId();
            this.description = snippet.getDescription();
            this.published = snippet.getPublishedAt().getValue();
        }

        if (item.getStatistics() != null) {
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
            // responseCode should also be 403 when when comment threads are grabbed during refresh.
            this.comments = Optional.ofNullable(stats.getCommentCount())
                    .orElse(BigInteger.ZERO)
                    .longValue();
        }

        this.refreshedOn = System.currentTimeMillis();
    }

    /**
     * Constructor used for initialization from the database.
     */
    public YouTubeVideo(String videoId, String channelId, String title, String description, String thumbUrl, long published, long grabDate, long comments, long likes, long dislikes, long viewCount, int responseCode) {
        super(videoId, title, thumbUrl);
        setTypeId(YType.VIDEO);

        this.channelId = channelId;
        this.description = description;
        this.published = published;
        this.refreshedOn = grabDate;
        this.comments = comments;
        this.likes = likes;
        this.dislikes = dislikes;
        this.viewCount = viewCount;
        this.responseCode = responseCode;
    }

    public void prepForExport() {
        publishDate = DateUtils.epochMillisToDateTime(published).toString();
        refreshedOnDate = DateUtils.epochMillisToDateTime(refreshedOn).toString();
    }

    /**
     * Overwrite channelId as null when set because it will be on the channel object for export.
     */
    public YouTubeVideo setAuthor(YouTubeChannel author) {
        this.channelId = null;
        this.author = author;
        return this;
    }

    /**
     * @return description that is on the video as of the refresh date
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return id of channel that published the video
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     * @return date video was published in epoch millis
     */
    public long getPublishedDate() {
        return published;
    }

    /**
     * @return date refreshed on in epoch millis
     */
    public long getRefreshedOn() {
        return refreshedOn;
    }

    public void setRefreshedOn(long epochMillis) {
        this.refreshedOn = epochMillis;
    }

    /**
     * The total number of comments as reported by YouTube API, not what's in the database.
     *
     * @return total comments as of refresh date
     */
    public long getCommentCount() {
        return comments;
    }

    /**
     * As reported by the YouTube API
     *
     * @return total likes as of refresh date
     */
    public long getLikes() {
        return likes;
    }

    /**
     * As reported by the YouTube API
     *
     * @return total dislikes as of refresh date
     */
    public long getDislikes() {
        return dislikes;
    }


    /**
     * As reported by the YouTube API
     *
     * @return total views as of refresh date
     */
    public long getViewCount() {
        return viewCount;
    }

    /**
     * Used to determine if comments are disabled (403) when querying the database.
     *
     * TODO: Maybe use error reason with new api? e.g. "commentsDisabled" Would require updates to DB columns, break compatibility
     *
     * @return response code when refreshing
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * @return title of the video
     */
    public String toString() {
        return getTitle();
    }
}
