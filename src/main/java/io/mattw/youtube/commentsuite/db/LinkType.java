package io.mattw.youtube.commentsuite.db;

public enum LinkType {
    VIDEO(GroupItemType.VIDEO),
    PLAYLIST(GroupItemType.PLAYLIST),
    CHANNEL_USER(GroupItemType.CHANNEL),
    CHANNEL_ID(GroupItemType.CHANNEL),
    CHANNEL_CUSTOM(GroupItemType.CHANNEL);

    private GroupItemType itemType;

    LinkType(GroupItemType itemType) {
        this.itemType = itemType;
    }

    public GroupItemType getItemType() {
        return itemType;
    }
}
