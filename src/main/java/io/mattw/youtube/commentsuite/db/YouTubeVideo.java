package io.mattw.youtube.commentsuite.db;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isAnyBlank;

public class YouTubeVideo implements Linkable, HasImage {

    private static final Logger logger = LogManager.getLogger();

    private String id;
    private String title;
    private String thumbUrl;
    private transient YouTubeType type = YouTubeType.VIDEO;
    private String channelId;
    private String description;
    private transient long published;
    private String publishDate;
    private transient long refreshedOn;
    private String refreshedOnDate;
    private long viewCount;
    private long comments;
    private long likes;
    private int responseCode;

    @Override
    public String getId() {
        return id;
    }

    public YouTubeVideo setId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public YouTubeVideo setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public String getThumbUrl() {
        return thumbUrl;
    }

    public YouTubeVideo setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
        return this;
    }

    public YouTubeType getType() {
        return type;
    }

    public YouTubeVideo setType(YouTubeType type) {
        this.type = type;
        return this;
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

    public int getResponseCode() {
        return responseCode;
    }

    public YouTubeVideo setResponseCode(int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    @Override
    public String toString() {
        return Stream.of(title, id)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(super.toString());
    }

    @Override
    public String toYouTubeLink() {
        return String.format(GROUP_ITEM_FORMATS.get(this.type), this.id);
    }

    public static Optional<YouTubeVideo> from(final Video video) {
        final String id = Optional.ofNullable(video)
                .map(Video::getId)
                .orElse(null);
        final Optional<VideoSnippet> snippet = Optional.ofNullable(video).map(Video::getSnippet);
        final String channelId = snippet.map(VideoSnippet::getChannelId).orElse(null);
        final String title = snippet.map(VideoSnippet::getTitle).orElse(null);
        final String thumb = snippet
                .map(VideoSnippet::getThumbnails)
                .map(ThumbnailDetails::getDefault)
                .map(Thumbnail::getUrl)
                .orElse(null);
        final String description = snippet.map(VideoSnippet::getDescription).orElse(null);
        final long published = snippet
                .map(VideoSnippet::getPublishedAt)
                .map(DateTime::getValue)
                .orElse(0L);

        final Optional<VideoStatistics> statistics = Optional.ofNullable(video).map(Video::getStatistics);
        final long viewCount = statistics.map(VideoStatistics::getViewCount)
                    .orElse(BigInteger.ZERO)
                    .longValue();
        final long likes = statistics.map(VideoStatistics::getLikeCount)
                .orElse(BigInteger.ZERO)
                .longValue();
        final long commentCount = statistics.map(VideoStatistics::getCommentCount)
                .orElse(BigInteger.ZERO)
                .longValue();

        if (isAnyBlank(id, channelId)) {
            logger.debug("Invalid {}", video);
            return Optional.empty();
        }

        return Optional.of(new YouTubeVideo()
                .setId(id)
                .setChannelId(channelId)
                .setTitle(title)
                .setThumbUrl(thumb)
                .setType(YouTubeType.VIDEO)
                .setDescription(description)
                .setPublished(published)
                .setViewCount(viewCount)
                .setLikes(likes)
                .setComments(commentCount)
                .setRefreshedOn(System.currentTimeMillis())
        );
    }

}
