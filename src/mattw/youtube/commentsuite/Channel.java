package mattw.youtube.commentsuite;

import javax.swing.ImageIcon;

public class Channel {
	public String channel_id;
	public String channel_name;
	public String channel_profile_url;
	public ImageIcon channel_profile;
	
	public Channel(String channel_id, String channel_name, String channel_profile_url, ImageIcon channel_profile) {
		this.channel_id = channel_id;
		this.channel_name = channel_name;
		this.channel_profile_url = channel_profile_url;
		this.channel_profile = channel_profile;
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
