package io.mattw.youtube.commentsuite.db;

import com.google.api.client.util.ArrayMap;
import com.google.api.services.youtube.model.*;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class YouTubeChannel implements Linkable, HasImage {

    private String id;
    private String title;
    private String thumbUrl;
    private transient YouTubeType type = YouTubeType.CHANNEL;

    public String getId() {
        return id;
    }

    public YouTubeChannel setId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public YouTubeChannel setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public YouTubeChannel setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
        return this;
    }

    public YouTubeType getType() {
        return type;
    }

    public YouTubeChannel setType(YouTubeType type) {
        this.type = type;
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

    public static Optional<YouTubeChannel> to(final String id, final String title, final String thumbUrl, Object source) {
        if (isBlank(id)) {
            logger.debug("Invalid {}", source);
            return Optional.empty();
        }

        return Optional.of(new YouTubeChannel()
                .setId(id)
                .setType(YouTubeType.CHANNEL)
                .setTitle(title)
                .setThumbUrl(thumbUrl));
    }

    public static Optional<YouTubeChannel> from(final Channel channel) {
        final String id = Optional.ofNullable(channel)
                .map(Channel::getId)
                .orElse(null);
        final String title = Optional.ofNullable(channel)
                .map(Channel::getSnippet)
                .map(ChannelSnippet::getTitle)
                .orElse(null);
        final String thumb = Optional.ofNullable(channel)
                .map(Channel::getSnippet)
                .map(ChannelSnippet::getThumbnails)
                .map(ThumbnailDetails::getDefault)
                .map(Thumbnail::getUrl)
                .orElse(null);

        return to(id, title, thumb, channel);
    }

    public static Optional<YouTubeChannel> from(final CommentThread commentThread) {
        final Comment topLevelComment = Optional.ofNullable(commentThread)
                .map(CommentThread::getSnippet)
                .map(CommentThreadSnippet::getTopLevelComment)
                .orElse(null);

        return from(topLevelComment);
    }

    public static Optional<YouTubeChannel> from(final Comment comment) {
        final String id = Optional.ofNullable(comment)
                .map(Comment::getSnippet)
                .map(CommentSnippet::getAuthorChannelId)
                .map(YouTubeChannel::getChannelIdFromObject)
                .orElse(null);
        final String title = Optional.ofNullable(comment)
                .map(Comment::getSnippet)
                .map(CommentSnippet::getAuthorDisplayName)
                .orElse(null);
        final String thumb = Optional.ofNullable(comment)
                .map(Comment::getSnippet)
                .map(CommentSnippet::getAuthorProfileImageUrl)
                .orElse(null);

        return to(id, title, thumb, comment);
    }

    public static String getChannelIdFromObject(final Object authorChannelId) {
        if (authorChannelId instanceof ArrayMap) {
            return ((ArrayMap<String, String>) authorChannelId).get("value");
        }

        return Optional.ofNullable(authorChannelId)
                .map(Object::toString)
                .orElse(null);
    }

}
