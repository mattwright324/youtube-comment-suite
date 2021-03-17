package io.mattw.youtube.commentsuite.db;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public interface Linkable {

    Map<YouTubeType, String> GROUP_ITEM_FORMATS = ImmutableMap.of(
            YouTubeType.VIDEO, "https://youtu.be/%s",
            YouTubeType.CHANNEL, "https://www.youtube.com/channel/%s",
            YouTubeType.PLAYLIST, "https://www.youtube.com/playlist?list=%s"
    );

    String toYouTubeLink();

}
