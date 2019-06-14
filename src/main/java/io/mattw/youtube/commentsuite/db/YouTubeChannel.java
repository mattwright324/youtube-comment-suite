package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.Comment;
import org.apache.commons.text.StringEscapeUtils;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class YouTubeChannel extends YouTubeObject {

    public YouTubeChannel(Channel item) {
        this(item, false);
    }

    public YouTubeChannel(Channel item, boolean fetchThumb) {
        super(item.getId(), StringEscapeUtils.unescapeHtml4(item.getSnippet().getTitle()),
                item.getSnippet().getThumbnails().getDefault().getUrl(), fetchThumb);
        setTypeId(YType.CHANNEL);
    }

    public YouTubeChannel(String channelId, String name, String thumbUrl, boolean fetchThumb) {
        super(channelId, name, thumbUrl, fetchThumb);
        setTypeId(YType.CHANNEL);
    }

    public YouTubeChannel(Comment item) {
        this(item, false);
    }

    public YouTubeChannel(Comment item, boolean fetchThumb) {
        this(item.getSnippet().getAuthorChannelId().toString(),
                StringEscapeUtils.unescapeHtml4(item.getSnippet().getAuthorDisplayName()),
                item.getSnippet().getAuthorProfileImageUrl(), fetchThumb);
        setTypeId(YType.CHANNEL);
    }

    public String toString() { return getTitle(); }
}
