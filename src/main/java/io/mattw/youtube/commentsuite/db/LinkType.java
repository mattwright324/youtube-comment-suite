package io.mattw.youtube.commentsuite.db;

public enum LinkType {
    VIDEO(YouTubeType.VIDEO),
    PLAYLIST(YouTubeType.PLAYLIST),
    CHANNEL_USER(YouTubeType.CHANNEL),
    CHANNEL_ID(YouTubeType.CHANNEL),
    CHANNEL_CUSTOM(YouTubeType.CHANNEL);

    private YouTubeType itemType;

    LinkType(YouTubeType itemType) {
        this.itemType = itemType;
    }

    public YouTubeType getItemType() {
        return itemType;
    }
}
