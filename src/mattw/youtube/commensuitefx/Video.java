package mattw.youtube.commensuitefx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Date;

import javax.imageio.ImageIO;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Video {
	
	public StringProperty videoTitle;
	public LongProperty videoChecked;
	
	public String video_id;
	public Channel channel;
	public Date grab_date;
	public Date publish_date;
	public String video_title, video_desc;
	public long total_comments, total_likes, total_dislikes, total_views;
	public String thumb_url;
	public int http_code;
	
	public boolean needImage = false;
	public BufferedImage buffered_thumb;
	public long comment_count = 0;
	private File thumbs = new File("Thumbs/");
	
	public Video(String video_id, String video_title) {
		this.video_id = video_id;
		this.video_title = video_title;
	}
	
	public Video(String video_id, Channel channel, Date grab_date, Date publish_date, String video_title, String video_desc, long total_comments, long total_likes, long total_dislikes, long total_views, String thumb_url, int http_code, boolean needImage) {
		this.video_id = video_id;
		this.channel = channel;
		this.grab_date = grab_date;
		this.publish_date = publish_date;
		this.video_title = video_title;
		this.video_desc = video_desc;
		this.total_comments = total_comments;
		this.total_likes = total_likes;
		this.total_dislikes = total_dislikes;
		this.total_views = total_views;
		this.http_code = http_code;
		this.needImage = needImage;
		
		this.videoTitle = new SimpleStringProperty(video_title);
		this.videoChecked = new SimpleLongProperty(grab_date != null ? grab_date.getTime() : 0);
		
		if(!needImage) return;
		thumbs.mkdirs();
		File thumbFile = new File(thumbs, video_id+".jpg");
		try {
			if(thumbFile.exists()) {
				buffered_thumb = ImageIO.read(thumbFile);
			} else {
				if(thumb_url != null && !thumb_url.equals("")) {
					System.out.println("Thumbnail not found ["+video_id+"]\n    Attempting to download from url.");
					buffered_thumb = ImageIO.read(new URL(thumb_url));
					ImageIO.write(buffered_thumb, "jpg", thumbFile);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public String getVideoTitle() {
		return videoTitle.get();
	}
	
	public Long getVideoChecked() {
		return videoChecked.get();
	}
	
	public String getYoutubeLink() {
		return "https://youtu.be/"+video_id;
	}
	
	public void setCommentCount(long comment_count) {
		this.comment_count = comment_count;
	}
	
	public boolean equals(Object o) {
		if(o instanceof Video) {
			Video video = (Video) o;
			return video.video_id.equals(video_id);
		}
		return false;
	}
	
	public String toString() {
		return video_title;
	}
}
