package mattw.youtube.commentsuite;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class Video {
	public String video_id;
	public Channel channel;
	public Date grab_date;
	public String publish_date;
	public String video_title, video_desc;
	public long total_comments, total_likes, total_dislikes, total_views;
	public String thumb_url;
	public ImageIcon thumbnail;
	public ImageIcon small_thumb;
	
	private File thumbs = new File("Thumbs/");
	
	public Video(String video_id, Channel channel, Date grab_date, String publish_date, String video_title, String video_desc, long total_comments, long total_likes, long total_dislikes, long total_views, String thumb_url) {
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
		
		thumbs.mkdirs();
		File thumbFile = new File(thumbs, video_id+".png");
		if(thumbFile.exists()) {
			try {
				thumbnail = new ImageIcon(ImageIO.read(thumbFile));
			} catch (IOException e) {
				
			}
		} else {
			System.out.println(video_id+".png doesn't exist");
			System.out.println("    "+thumbFile.getAbsolutePath());
			try {
				thumbs.mkdirs();
				System.out.println("    Downloading...");
				ImageIO.write(ImageIO.read(new URL(thumb_url)), "png", thumbFile);
				thumbnail = new ImageIcon(ImageIO.read(thumbFile));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		if(thumbnail == null) thumbnail = CommentSuite.window.imgThumbPlaceholder;
		small_thumb = new ImageIcon(thumbnail.getImage().getScaledInstance(160, 90, 0));
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
