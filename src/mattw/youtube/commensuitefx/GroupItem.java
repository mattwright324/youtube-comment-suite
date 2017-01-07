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

public class GroupItem {
	
	public StringProperty groupType;
	public StringProperty groupTitle;
	public LongProperty groupChecked;
	
	public int gitem_id;
	public int type_id;
	public String youtube_id;
	public String type;
	public String title;
	public String channel_title;
	public Date published;
	public Date last_checked;
	public String thumb_url;
	
	public boolean needImage = false;
	public BufferedImage thumbnail;
	private File thumbs = new File("Thumbs/");
	
	public GroupItem(int gitem_id, String title) {
		this.gitem_id = gitem_id;
		this.title = title;
	}
	
	public GroupItem(int type_id, String type, String youtube_id, String title, String channel_title, Date published, Date last_checked, String thumb_url, boolean needImage) {
		this.type_id = type_id;
		this.type = type;
		this.youtube_id = youtube_id;
		this.title = title;
		this.channel_title = channel_title;
		this.published = published;
		this.last_checked = last_checked;
		this.needImage = needImage;
		
		groupType = new SimpleStringProperty(type);
		groupTitle = new SimpleStringProperty(title);
		groupChecked = new SimpleLongProperty(last_checked != null ? last_checked.getTime() : 0);
		
		if(!needImage) return;
		thumbs.mkdirs();
		File thumbFile = new File(thumbs, youtube_id+".jpg");
		try {
			if(thumbFile.exists()) {
				thumbnail = ImageIO.read(thumbFile);
			} else {
				if(thumb_url != null && !thumb_url.equals("")) {
					System.out.println("Thumbnail not found ["+youtube_id+"]\n    Attempting to download from url.");
					thumbnail = ImageIO.read(new URL(thumb_url));
					ImageIO.write(thumbnail, "jpg", thumbFile);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public String getGroupType() {
		return groupType.get();
	}
	
	public String getGroupTitle() {
		return groupTitle.get();
	}
	
	public Long getGroupChecked() {
		return groupChecked.get();
	}
	
	public String getYoutubeLink() {
		String link = "https://www.youtube.com/";
		if(type_id == 0) link = "https://youtu.be/"+youtube_id;
		if(type_id == 1) link = "https://www.youtube.com/channels/"+youtube_id;
		if(type_id == 2) link = "https://www.youtube.com/playlists?list="+youtube_id;
		return link;
	}
	
	public void setID(int id) {
		gitem_id = id;
	}
	
	public int getID() {
		return gitem_id;
	}
	
	public String toString() {
		return title;
	}
}
