package mattw.youtube.commentsuitefx;

import java.util.Date;

import com.google.gson.Gson;

import mattw.youtube.datav3.resources.CommentThreadsList;
import mattw.youtube.datav3.resources.CommentsList;

public class CommentType extends YoutubeObject {
	
	private final String text;
	private final Date date;
	private final String videoId, channelId;
	private final int likes, replies;
	private final boolean isReply;
	private final String parentId;
	
	public CommentType(String commentId, String videoId, String channelId, String text, Date date, int likes, int replies, boolean isReply, String parentId) {
		super(commentId, null);
		typeId = 3;
		this.text = text;
		this.date = date;
		this.videoId = videoId;
		this.channelId = channelId;
		this.likes = likes;
		this.replies = replies;
		this.isReply = isReply;
		this.parentId = parentId;
	}
	
	public CommentType(CommentsList.Item item, String videoId) {
		super(item.getId(), null);
		typeId = 3;
		this.text = item.snippet.textDisplay;
		this.date = item.snippet.publishedAt;
		this.videoId = videoId;
		if(item.snippet.authorChannelId != null && item.snippet.authorChannelId.value != null) {
			this.channelId = item.snippet.authorChannelId.value;
		} else {
			System.out.println("Null Channel: "+new Gson().toJson(item));
			this.channelId = "";
		}
		this.likes = item.snippet.likeCount;
		this.replies = -1;
		this.isReply = true;
		this.parentId = item.snippet.parentId;
	}
	
	public CommentType(CommentThreadsList.Item item) {
		super(item.snippet.topLevelComment.getId(), null);
		typeId = 3;
		this.text = item.snippet.topLevelComment.snippet.textDisplay;
		this.date = item.snippet.topLevelComment.snippet.publishedAt;
		this.videoId = item.snippet.videoId;
		if(item.snippet.topLevelComment.snippet.authorChannelId != null && item.snippet.topLevelComment.snippet.authorChannelId.value != null) {
			this.channelId = item.snippet.topLevelComment.snippet.authorChannelId.value;
		} else {
			System.out.println("Null Channel: "+new Gson().toJson(item));
			this.channelId = "";
		}
		this.likes = item.snippet.topLevelComment.snippet.likeCount;
		this.replies = item.snippet.totalReplyCount;
		this.isReply = false;
		this.parentId = null;
	}
	
	public String getText() {
		return text;
	}
	
	public Date getDate() {
		return date;
	}
	
	public String getVideoId() {
		return videoId;
	}
	
	public String getChannelId() {
		return channelId;
	}
	
	public int getLikes() {
		return likes;
	}
	
	public int getReplies() {
		return replies;
	}
	
	public boolean isReply() {
		return isReply;
	}
	
	public String getParentId() {
		return parentId;
	}

	public String toString() {
		return getId();
	}
}
