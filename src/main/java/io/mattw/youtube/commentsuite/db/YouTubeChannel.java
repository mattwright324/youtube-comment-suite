package io.mattw.youtube.commentsuite.db;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.Comment;
import org.apache.commons.text.StringEscapeUtils;

/**
 * @author mattwright324
 */
public class YouTubeChannel extends YouTubeObject {

    /**
     * Convert YouTube object to custom Channel object.
     */
    public YouTubeChannel(Channel item) {
        super(item.getId(), StringEscapeUtils.unescapeHtml4(item.getSnippet().getTitle()),
                item.getSnippet().getThumbnails().getDefault().getUrl());
        setTypeId(YType.CHANNEL);
    }

    /**
     * Comment objects usually have enough detail about the poster to create our object.
     */
    public YouTubeChannel(Comment item) {
        this(getChannelIdFromObject(item.getSnippet().getAuthorChannelId()),
                StringEscapeUtils.unescapeHtml4(item.getSnippet().getAuthorDisplayName()),
                item.getSnippet().getAuthorProfileImageUrl());
        setTypeId(YType.CHANNEL);
    }

    /**
     * Constructor used for initialization from the database.
     */
    public YouTubeChannel(String channelId, String name, String thumbUrl) {
        super(channelId, name, thumbUrl);
        setTypeId(YType.CHANNEL);
    }

    public String toString() {
        return getTitle();
    }
}
