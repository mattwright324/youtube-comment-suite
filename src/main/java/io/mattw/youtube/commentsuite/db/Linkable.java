package io.mattw.youtube.commentsuite.db;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public interface Linkable {

    Map<GroupItemType, String> GROUP_ITEM_FORMATS = ImmutableMap.of(
            GroupItemType.VIDEO, "https://youtu.be/%s",
            GroupItemType.CHANNEL, "https://www.youtube.com/channel/%s",
            GroupItemType.PLAYLIST, "https://www.youtube.com/playlist?list=%s"
    );

    String toYouTubeLink();

}
