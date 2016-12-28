package mattw.youtube.commentsuite;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class Channel {
	public String channel_id;
	public String channel_name;
	public String channel_profile_url;
	
	public ImageIcon channel_profile;
	public boolean download_profile = false;
	private File thumbs = new File("Thumbs/");
	
	public Channel(String channel_id, String channel_name, String channel_profile_url, boolean download_profile) {
		this.channel_id = channel_id;
		this.channel_name = channel_name;
		this.channel_profile_url = channel_profile_url;
		this.download_profile = download_profile;
		
		thumbs.mkdirs();
		File thumbFile = new File(thumbs, channel_id+".jpg");
		try {
			if(thumbFile.exists()) {
				channel_profile = new ImageIcon(ImageIO.read(thumbFile));
			} else {
				if(download_profile && channel_profile_url != null && !channel_profile_url.equals("")) {
					BufferedImage bi = ImageIO.read(new URL(channel_profile_url));
					ImageIO.write(bi, "jpg", thumbFile);
					channel_profile = new ImageIcon(bi);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if(channel_profile == null) {
			channel_profile = CommentSuite.window.imgBlankProfile;
		} else {
			channel_profile = new ImageIcon(channel_profile.getImage().getScaledInstance((int) (24.0 * channel_profile.getIconWidth() / channel_profile.getIconHeight()), 24, 0));
		}
	}
	
	public boolean equals(Object o) {
		if(o instanceof Channel) {
			Channel c = (Channel) o;
			return c.channel_id.equals(channel_id);
		}
		return false;
	}
	
	public String toString() {
		return channel_name;
	}
}
