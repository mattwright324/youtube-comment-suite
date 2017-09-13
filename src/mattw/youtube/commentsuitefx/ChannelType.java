package mattw.youtube.commentsuitefx;

import org.apache.commons.lang3.StringEscapeUtils;

import mattw.youtube.datav3.resources.ChannelsList;
import mattw.youtube.datav3.resources.CommentThreadsList;
import mattw.youtube.datav3.resources.CommentsList;

public class ChannelType extends YoutubeObject {
	
	ChannelType(String channeId, String name, String thumbUrl, boolean fetchThumb) {
		super(channeId, name, thumbUrl, fetchThumb);
		typeId = 1;
	}

	ChannelType(CommentsList.Item item, boolean fetchThumb) {
		this(item.snippet.authorChannelId.value, StringEscapeUtils.unescapeHtml4(item.snippet.authorDisplayName), item.snippet.authorProfileImageUrl, fetchThumb);
	}

	protected ChannelType(CommentThreadsList.Item item, boolean fetchThumb) {
		this(item.snippet.topLevelComment, fetchThumb);
	}

	ChannelType(ChannelsList.Item item, boolean fetchThumb) {
		this(item.getId(), StringEscapeUtils.unescapeHtml4(item.snippet.title), item.snippet.thumbnails.default_thumb.url.toString(), fetchThumb);
	}
	
	public String toString() {
		return getTitle();
	}
}
