package mattw.youtube.commensuitefx;

import java.util.Date;

public class Comment {
	public String comment_id;
	public Channel channel;
	public String video_id;
	public Date comment_date;
	public String comment_text;
	public long comment_likes, reply_count;
	public boolean is_reply;
	public String parent_id;
	
	public Comment(String comment_id, Channel channel, String video_id, Date comment_date, String comment_text, long comment_likes, long reply_count, boolean is_reply, String parent_id) {
		this.comment_id = comment_id;
		this.channel = channel;
		this.video_id = video_id;
		this.comment_date = comment_date;
		this.comment_text = comment_text;
		this.comment_likes = comment_likes;
		this.reply_count = reply_count;
		this.is_reply = is_reply;
		this.parent_id = parent_id;
	}
	
	public boolean equals(Object o) {
		if(o instanceof Comment) {
			Comment c = (Comment) o;
			return c.comment_id.equals(comment_id);
		}
		return false;
	}
	
	public String toString() {
		return comment_text;
	}
}
