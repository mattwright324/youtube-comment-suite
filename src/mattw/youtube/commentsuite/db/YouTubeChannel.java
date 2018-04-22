package mattw.youtube.commentsuite.db;

import mattw.youtube.datav3.resources.ChannelsList;
import mattw.youtube.datav3.resources.CommentsList;
import org.apache.commons.text.StringEscapeUtils;

public class YouTubeChannel extends YouTubeObject {

    public YouTubeChannel(String channelId, String name, String thumbUrl, boolean fetchThumb) {
        super(channelId, name, thumbUrl, fetchThumb);
        this.typeId = 1;
    }

    public YouTubeChannel(ChannelsList.Item item, boolean fetchThumb) {
        super(item.getId(), StringEscapeUtils.unescapeHtml4(item.snippet.title), item.snippet.thumbnails.default_thumb.url.toString(), fetchThumb);
        this.typeId = 1;
    }

    public YouTubeChannel(CommentsList.Item item, boolean fetchThumb) {
        this(item.snippet.authorChannelId.value, StringEscapeUtils.unescapeHtml4(item.snippet.authorDisplayName), item.snippet.authorProfileImageUrl, fetchThumb);
        this.typeId = 1;
    }

    public String toString() { return getTitle(); }
}
