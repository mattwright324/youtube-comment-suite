package io.mattw.youtube.commentsuite.db;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Resolve a number of different data types from the YouTube API and links to GroupItem(s)
 */
public class GroupItemResolver {

    private static final Logger logger = LogManager.getLogger();

    public Optional<GroupItem> to(
            final String id, final GroupItemType type, final String displayName,
            final String thumbUrl, final String channelTitle, final long publishedAt
    ) {
        if (isBlank(id) || type == null) {
            return Optional.empty();
        }

        return Optional.of(new GroupItem()
                .setId(id)
                .setType(type)
                .setDisplayName(displayName)
                .setThumbUrl(thumbUrl)
                .setChannelTitle(channelTitle)
                .setPublished(publishedAt));
    }

    public Optional<GroupItem> from(final SearchResult searchResult) {
        final String id = Optional.ofNullable(searchResult)
                .map(SearchResult::getId)
                .map(this::getIdFromResource)
                .orElse(null);
        final GroupItemType type = Optional.ofNullable(searchResult)
                .map(SearchResult::getId)
                .map(this::getTypeFromResource)
                .orElse(null);
        final String displayName = Optional.ofNullable(searchResult)
                .map(SearchResult::getSnippet)
                .map(SearchResultSnippet::getTitle)
                .orElse(null);
        final String thumbUrl = Optional.ofNullable(searchResult)
                .map(SearchResult::getSnippet)
                .map(SearchResultSnippet::getThumbnails)
                .map(ThumbnailDetails::getMedium)
                .map(Thumbnail::getUrl)
                .orElse(null);
        final String channelTitle = Optional.ofNullable(searchResult)
                .map(SearchResult::getSnippet)
                .map(SearchResultSnippet::getChannelTitle)
                .orElse(null);
        final long published = Optional.ofNullable(searchResult)
                .map(SearchResult::getSnippet)
                .map(SearchResultSnippet::getPublishedAt)
                .map(DateTime::getValue)
                .orElse(0L);

        return to(id, type, displayName, thumbUrl, channelTitle, published);
    }

    public Optional<GroupItem> from(final Video video) {
        final String id = Optional.ofNullable(video)
                .map(Video::getId)
                .orElse(null);
        final String displayName = Optional.ofNullable(video)
                .map(Video::getSnippet)
                .map(VideoSnippet::getTitle)
                .orElse(null);
        final String thumbUrl = Optional.ofNullable(video)
                .map(Video::getSnippet)
                .map(VideoSnippet::getThumbnails)
                .map(ThumbnailDetails::getMedium)
                .map(Thumbnail::getUrl)
                .orElse(null);
        final String channelTitle = Optional.ofNullable(video)
                .map(Video::getSnippet)
                .map(VideoSnippet::getChannelTitle)
                .orElse(null);
        final long published = Optional.ofNullable(video)
                .map(Video::getSnippet)
                .map(VideoSnippet::getPublishedAt)
                .map(DateTime::getValue)
                .orElse(0L);

        return to(id, GroupItemType.VIDEO, displayName, thumbUrl, channelTitle, published);
    }

    public Optional<GroupItem> from(final Channel channel) {
        final String id = Optional.ofNullable(channel)
                .map(Channel::getId)
                .orElse(null);
        final String displayName = Optional.ofNullable(channel)
                .map(Channel::getSnippet)
                .map(ChannelSnippet::getTitle)
                .orElse(null);
        final String thumbUrl = Optional.ofNullable(channel)
                .map(Channel::getSnippet)
                .map(ChannelSnippet::getThumbnails)
                .map(ThumbnailDetails::getMedium)
                .map(Thumbnail::getUrl)
                .orElse(null);
        final long published = Optional.ofNullable(channel)
                .map(Channel::getSnippet)
                .map(ChannelSnippet::getPublishedAt)
                .map(DateTime::getValue)
                .orElse(0L);

        return to(id, GroupItemType.CHANNEL, displayName, thumbUrl, displayName, published);
    }

    public Optional<GroupItem> from(final Playlist playlist) {
        final String id = Optional.ofNullable(playlist)
                .map(Playlist::getId)
                .orElse(null);
        final String displayName = Optional.ofNullable(playlist)
                .map(Playlist::getSnippet)
                .map(PlaylistSnippet::getTitle)
                .orElse(null);
        final String thumbUrl = Optional.ofNullable(playlist)
                .map(Playlist::getSnippet)
                .map(PlaylistSnippet::getThumbnails)
                .map(ThumbnailDetails::getMedium)
                .map(Thumbnail::getUrl)
                .orElse(null);
        final String channelTitle = Optional.ofNullable(playlist)
                .map(Playlist::getSnippet)
                .map(PlaylistSnippet::getChannelTitle)
                .orElse(null);
        final long published = Optional.ofNullable(playlist)
                .map(Playlist::getSnippet)
                .map(PlaylistSnippet::getPublishedAt)
                .map(DateTime::getValue)
                .orElse(0L);

        return to(id, GroupItemType.PLAYLIST, displayName, thumbUrl, channelTitle, published);
    }

    /**
     * @return id of video, channel, or playlist
     */
    private String getIdFromResource(final ResourceId resourceId) {
        return Optional.ofNullable(resourceId)
                .map(resource -> Stream.of(resource.getVideoId(), resource.getChannelId(), resource.getPlaylistId()))
                .orElseGet(Stream::empty)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return id of video, channel, or playlist
     */
    private GroupItemType getTypeFromResource(final ResourceId resourceId) {
        return Optional.ofNullable(resourceId)
                .map(resource -> {
                    if (resource.getVideoId() != null) {
                        return GroupItemType.VIDEO;
                    } else if (resource.getPlaylistId() != null) {
                        return GroupItemType.PLAYLIST;
                    } else if (resource.getChannelId() != null) {
                        return GroupItemType.CHANNEL;
                    }
                    return null;
                })
                .orElse(null);
    }

}
