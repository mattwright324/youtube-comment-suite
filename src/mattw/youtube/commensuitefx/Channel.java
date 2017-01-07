package mattw.youtube.commensuitefx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;

public class Channel {
	public String channel_id;
	public String channel_name;
	public String channel_profile_url;
	
	public BufferedImage buffered_profile;
	public boolean download_profile = false;
	private File thumbs = new File("Thumbs/");
	
	public Channel(String channel_id, String channel_name, String channel_profile_url, boolean download_profile) {
		this.channel_id = channel_id;
		this.channel_name = channel_name;
		this.channel_profile_url = channel_profile_url;
		this.download_profile = download_profile;
		
		loadProfile();
	}
	
	public void loadProfile() {
		thumbs.mkdirs();
		File thumbFile = new File(thumbs, channel_id+".jpg");
		try {
			if(thumbFile.exists()) {
				buffered_profile = ImageIO.read(thumbFile);
			} else {
				if(download_profile && channel_profile_url != null && !channel_profile_url.equals("")) {
					buffered_profile = ImageIO.read(new URL(channel_profile_url));
					ImageIO.write(buffered_profile, "jpg", thumbFile);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public String getYoutubeLink() {
		return "https://www.youtube.com/channel/"+channel_id;
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
