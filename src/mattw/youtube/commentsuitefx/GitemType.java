package mattw.youtube.commentsuitefx;

import java.util.Date;

import mattw.youtube.datav3.resources.SearchList;

public class GitemType extends YoutubeObject {

	private final long published, lastChecked;
	private final String channelTitle;
	private int gitemId;
	
	private static String findId(SearchList.Item item) {
		if(item.id.videoId != null) {
			return item.id.videoId;
		} else if(item.id.channelId != null) {
			return item.id.channelId;
		} else if(item.id.playlistId != null) {
			return item.id.playlistId;
		}
		return "";
	}
	
	public GitemType(SearchList.Item item) {
		super(findId(item), item.snippet.title, item.snippet.thumbnails.medium.url.toString(), true);
		int type;
		if(item.id.videoId != null) {
			type = 0;
		} else if(item.id.channelId != null) {
			type = 1;
		} else if(item.id.playlistId != null) {
			type = 2;
		} else {
			type = -1;
		}
		typeId = type;
		this.gitemId = -1;
		this.channelTitle = item.snippet.channelTitle;
		this.published = item.snippet.publishedAt.getTime();
		this.lastChecked = 0;
	}
	
	public GitemType(int gitemId, String title) {
		super(null, title, null, false);
		typeId = -1;
		this.gitemId = gitemId;
		this.published = 0;
		this.lastChecked = 0;
		this.channelTitle = null;
	}
	
	public GitemType(int type, int gitemId, String youtubeId, String title, String channelTitle, String thumbUrl, boolean fetchThumb, Date published, Date lastChecked) {
		super(youtubeId, title, thumbUrl, fetchThumb);
		typeId = type;
		this.gitemId = gitemId;
		this.channelTitle = channelTitle;
		this.published = published.getTime();
		this.lastChecked = lastChecked.getTime();
	}
	
	public void setGitemId(int gitemId) {
		this.gitemId = gitemId;
	}
	
	public int getGitemId() {
		return gitemId;
	}
	
	public String getChannelTitle() {
		return channelTitle;
	}
	
	public Long getPublished() {
		return published;
	}
	
	public Long getLastChecked() {
		return lastChecked;
	}
	
	public String toString() {
		return getTitle();
	}

}
