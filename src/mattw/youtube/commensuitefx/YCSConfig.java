package mattw.youtube.commensuitefx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class YCSConfig {
	
	private final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL).create();
	private final File CONFIG_FILE = new File("config.ycs");
	
	private String youtube_data_key = "AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ";
	private String username = "Guest";
	private String channelId = "";
	private OA2Tokens access_tokens;
	
	private void loadAs(YCSConfig config) {
		setYoutubeKey(config.youtube_data_key);
		setUsername(config.username);
		setChannelId(config.channelId);
		setAccessTokens(config.access_tokens);
	}
	
	public void setAccessTokens(OA2Tokens tokens) {
		access_tokens = tokens;
	}
	
	public void setYoutubeKey(String key) {
		youtube_data_key = key;
	}
	
	public void setUsername(String user) {
		if(user.equals("")) {
			username = "Guest";
		} else {
			username = user;
		}
	}
	
	public void setChannelId(String id) {
		channelId = id;
	}
	
	public boolean isSetup() {
		return !youtube_data_key.equals("");
	}
	
	public OA2Tokens getAccessTokens() {
		return access_tokens;
	}
	
	public String getYoutubeKey() {
		return youtube_data_key;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getChannelId() {
		return channelId;
	}
	
	public void save() throws IOException {
		if(!CONFIG_FILE.exists()) {
			CONFIG_FILE.createNewFile();
		}
		FileWriter fr = new FileWriter(CONFIG_FILE);
		fr.write(gson.toJson(this));
		fr.close();
	}
	
	public void load() throws IOException {
		if(!CONFIG_FILE.exists()) {
			save();
		}
		BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE));
		String json = "";
		String line;
		while((line = br.readLine()) != null) {
			json += line;
		}
		br.close();
		loadAs(gson.fromJson(json, YCSConfig.class));
	}
	
}
