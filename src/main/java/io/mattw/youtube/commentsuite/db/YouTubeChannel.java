package io.mattw.youtube.commentsuite.db;

import io.mattw.youtube.datav3.entrypoints.ChannelsList;
import io.mattw.youtube.datav3.entrypoints.CommentsList;
import org.apache.commons.text.StringEscapeUtils;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class YouTubeChannel extends YouTubeObject {

    public YouTubeChannel(ChannelsList.Item item) {
        this(item, false);
    }

    public YouTubeChannel(ChannelsList.Item item, boolean fetchThumb) {
        super(item.getId(), StringEscapeUtils.unescapeHtml4(item.getSnippet().getTitle()),
                item.getSnippet().getThumbnails().getDefault().getURL().toString(), fetchThumb);
        setTypeId(YType.CHANNEL);
    }

    public YouTubeChannel(String channelId, String name, String thumbUrl, boolean fetchThumb) {
        super(channelId, name, thumbUrl, fetchThumb);
        setTypeId(YType.CHANNEL);
    }

    public YouTubeChannel(CommentsList.Item item) {
        this(item, false);
    }

    public YouTubeChannel(CommentsList.Item item, boolean fetchThumb) {
        this(item.getSnippet().getAuthorChannelId().getValue(),
                StringEscapeUtils.unescapeHtml4(item.getSnippet().getAuthorDisplayName()),
                item.getSnippet().getAuthorProfileImageUrl(), fetchThumb);
        setTypeId(YType.CHANNEL);
    }

    public String toString() { return getTitle(); }
}
