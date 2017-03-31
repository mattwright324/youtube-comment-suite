package mattw.youtube.commensuitefx;

import org.apache.commons.lang3.StringEscapeUtils;

import mattw.youtube.datav3.list.ChannelsList;
import mattw.youtube.datav3.list.CommentThreadsList;
import mattw.youtube.datav3.list.CommentsList;

public class ChannelType extends YoutubeObject {
	
	protected ChannelType(String channeId, String name, String thumbUrl, boolean fetchThumb) {
		super(channeId, name, thumbUrl, fetchThumb);
		typeId = 1;
	}
	
	/**
	 * This item must contain the snippet part.
	 * @param item
	 * @param fetchThumb
	 */
	protected ChannelType(CommentsList.Item item, boolean fetchThumb) {
		this(item.snippet.authorChannelId.value, StringEscapeUtils.unescapeHtml4(item.snippet.authorDisplayName), item.snippet.authorProfileImageUrl, fetchThumb);
	}
	
	/**
	 * This item must contain the snippet part.
	 * @param item
	 * @param fetchThumb
	 */
	protected ChannelType(CommentThreadsList.Item item, boolean fetchThumb) {
		this(item.snippet.topLevelComment, fetchThumb);
	}
	
	/**
	 * This item must contain the snippet part.
	 * @param item
	 * @param fetchThumb
	 */
	protected ChannelType(ChannelsList.Item item, boolean fetchThumb) {
		this(item.id, StringEscapeUtils.unescapeHtml4(item.snippet.title), item.snippet.thumbnails.default_thumb.url.toString(), fetchThumb);
	}
	
	public String toString() {
		return getTitle();
	}
}
