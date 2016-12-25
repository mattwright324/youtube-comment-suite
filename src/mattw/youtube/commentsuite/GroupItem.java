package mattw.youtube.commentsuite;

import javax.swing.ImageIcon;

public class GroupItem {
	public int gitem_id;
	public int type_id;
	public String youtube_id;
	public String type;
	public String title;
	public String channel_title;
	public String published;
	public ImageIcon thumbnail;
	public String last_checked;
	
	public GroupItem(int type_id, String type, String youtube_id, String title, String channel_title, String published, ImageIcon thumbnail, String last_checked) {
		this.type_id = type_id;
		this.type = type;
		this.youtube_id = youtube_id;
		this.title = title;
		this.channel_title = channel_title;
		this.published = published;
		this.thumbnail = thumbnail;
		this.last_checked = last_checked;
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
