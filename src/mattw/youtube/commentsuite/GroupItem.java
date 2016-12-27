package mattw.youtube.commentsuite;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class GroupItem {
	public int gitem_id;
	public int type_id;
	public String youtube_id;
	public String type;
	public String title;
	public String channel_title;
	public Date published;
	public Date last_checked;
	public String thumb_url;
	
	public ImageIcon thumbnail;
	private File thumbs = new File("Thumbs/");
	
	public GroupItem(int type_id, String type, String youtube_id, String title, String channel_title, Date published, Date last_checked, String thumb_url) {
		this.type_id = type_id;
		this.type = type;
		this.youtube_id = youtube_id;
		this.title = title;
		this.channel_title = channel_title;
		this.published = published;
		this.last_checked = last_checked;
		
		thumbs.mkdirs();
		File thumbFile = new File(thumbs, youtube_id+".png");
		try {
			if(thumbFile.exists()) {
				thumbnail = new ImageIcon(ImageIO.read(thumbFile));
			} else {
				if(thumb_url != null && !thumb_url.equals("")) {
					System.out.println("Thumbnail not found ["+youtube_id+"]\n    Attempting to download from url.");
					BufferedImage bi = ImageIO.read(new URL(thumb_url));
					ImageIO.write(bi, "png", thumbFile);
					thumbnail = new ImageIcon(bi);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if(thumbnail == null) thumbnail = CommentSuite.window.imgThumbPlaceholder;
		thumbnail = new ImageIcon(thumbnail.getImage().getScaledInstance((int) (45.0 * thumbnail.getIconWidth() / thumbnail.getIconHeight())+1, 45, 0));
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
