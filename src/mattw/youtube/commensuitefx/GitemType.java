package mattw.youtube.commensuitefx;

import java.util.Date;

public class GitemType extends YoutubeObject {
	
	private final String typeText;
	private final long published, lastChecked;
	private final String channelTitle;
	private int gitemId;
	
	protected GitemType(int gitemId, String title) {
		super(null, title, null, false);
		type_id = -1;
		typeText = "gitem";
		this.gitemId = gitemId;
		this.published = 0;
		this.lastChecked = 0;
		this.channelTitle = null;
	}
	
	protected GitemType(int type, int gitemId, String youtubeId, String title, String channelTitle, String thumbUrl, boolean fetchThumb, Date published, Date lastChecked) {
		super(youtubeId, title, thumbUrl, fetchThumb);
		type_id = type;
		typeText = type_id == 0 ? "comment" : type_id == 1 ? "channel" : type_id == 2 ? "playlist" : "???";
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
	
	public String getTypeText() {
		return typeText;
	}
	
	public String toString() {
		return getTitle();
	}

}
