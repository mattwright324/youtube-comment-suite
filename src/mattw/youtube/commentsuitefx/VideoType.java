package mattw.youtube.commentsuitefx;

import java.util.Date;

public class VideoType extends YoutubeObject {
	
	private final String channelId;
	private final String title;
	private final String description;
	private final long publishDate, grabDate;
	private final long comments, likes, dislikes, views;
	private final int httpCode;
	
	public VideoType(String youtubeId, String channelId, String title, String thumbUrl, boolean fetchThumb, String description, long comments, long likes, long dislikes, long views, Date publishDate, Date grabDate, int httpCode) {
		super(youtubeId, title, thumbUrl, fetchThumb);
		typeId = 0;
		this.title = title;
		this.description = description;
		this.publishDate = publishDate.getTime();
		this.grabDate = grabDate.getTime();
		this.channelId = channelId;
		this.comments = comments;
		this.likes = likes;
		this.dislikes = dislikes;
		this.views = views;
		this.httpCode = httpCode;
	}
	
	public String getChannelId() {
		return channelId;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public long getPublishDate() {
		return publishDate;
	}
	
	public long getGrabDate() {
		return grabDate;
	}
	
	public long getComments() {
		return comments;
	}
	
	public long getLikes() {
		return likes;
	}
	
	public long getDislikes() {
		return dislikes;
	}
	
	public long getViews() {
		return views;
	}

	public String toString() {
		return getTitle();
	}
	
	public int getHttpCode() {
		return httpCode;
	}
}
