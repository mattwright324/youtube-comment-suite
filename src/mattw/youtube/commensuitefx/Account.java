package mattw.youtube.commensuitefx;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mattw.youtube.datav3.list.ChannelsList;

import java.io.IOException;

public class Account {

	protected StringProperty nameProperty = new SimpleStringProperty("");
	public String username = "";
	public String channelId;
	public String profile;
	public OA2Tokens tokens;

	public String toString() {
		return getUsername();
	}

	public Account(OA2Tokens tokens, boolean first) {
		this.tokens = tokens;
		if(first) {
			try {
				getData();
			} catch (IOException ignored) {
			}
		}
		setUsername(username);
	}

	public void refreshTokens() {
		OA2Tokens new_tokens;
		try {
			new_tokens = OA2Handler.refreshAccessTokens(tokens);
			new_tokens.setRefreshToken(tokens.refresh_token);
			setTokens(new_tokens);
			getData();
			try {
				CommentSuiteFX.getApp().getConfig().save();
			} catch (IOException ignored) {}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getData() throws IOException {
		CommentSuiteFX.getYoutube().setAccessToken(tokens.access_token);
		ChannelsList cl = CommentSuiteFX.getYoutube().getChannelsByMine(ChannelsList.PART_SNIPPET);
		String title = cl.items[0].snippet.title;
		setUsername(title);
		setChannelId(cl.items[0].id);
		setProfile(cl.items[0].snippet.thumbnails.default_thumb.url.toString());
		try {
			CommentSuiteFX.getApp().getConfig().save();
		} catch (IOException ignored) {
			ignored.printStackTrace();
		}
	}

	public void signOut() {
		CommentSuiteFX.getApp().getConfig().accounts.remove(this);
		try {
			CommentSuiteFX.getApp().getConfig().save();
		} catch (IOException ignored) {
		}
	}

	public OA2Tokens getTokens() {
		return this.tokens;
	}

	public void setTokens(OA2Tokens tokens) {
		this.tokens = tokens;
	}

	public String getUsername() {
		nameProperty = new SimpleStringProperty(username);
		return nameProperty.getValue();
	}

	public void setUsername(String user) {
		username = user;
		nameProperty = new SimpleStringProperty(username);
	}

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String id) {
		channelId = id;
	}
}
