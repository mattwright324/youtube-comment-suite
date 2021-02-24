package io.mattw.youtube.commentsuite.db;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Database entry for searched YouTube entries (Video, Channel, Playlist).
 * getYouTubeId() from YouTubeObject represents the GroupItem ID.
 */
public class GroupItem implements Linkable {

    public static final String ALL_ITEMS = "ALL_ITEMS";

    private String id;
    private String displayName;
    private String thumbUrl;
    private YouTubeType type;
    private String channelTitle;
    private long published;
    private long lastChecked;

    public String getId() {
        return id;
    }

    public GroupItem setId(String id) {
        this.id = id;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public GroupItem setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public GroupItem setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
        return this;
    }

    public YouTubeType getType() {
        return type;
    }

    public GroupItem setType(YouTubeType type) {
        this.type = type;
        return this;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public GroupItem setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
        return this;
    }

    public long getPublished() {
        return published;
    }

    public GroupItem setPublished(long published) {
        this.published = published;
        return this;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public GroupItem setLastChecked(long lastChecked) {
        this.lastChecked = lastChecked;
        return this;
    }

    @Override
    public String toString() {
        return Stream.of(displayName, id)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(super.toString());
    }

    @Override
    public String toYouTubeLink() {
        return String.format(GROUP_ITEM_FORMATS.get(this.type), this.id);
    }

}
