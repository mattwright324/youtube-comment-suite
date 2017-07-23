package mattw.youtube.commensuitefx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class YCSConfig {
	
	private final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL, Modifier.PROTECTED).create();
	private final File CONFIG_FILE = new File("config.ycs");
	
	private String youtube_data_key = "AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ";
	public List<Account> accounts = new ArrayList<>();

	public boolean isSignedIn(String channelId) {
		return accounts.stream().anyMatch(cid -> cid.getChannelId().equals(channelId));
	}

	public String getWelcomeStatement() {
		if(accounts.isEmpty()) {
			return "Welcome, Guest";
		}
		if(accounts.size() == 1) {
			return "Welcome, "+accounts.get(0).getUsername();
		}
		return "Welcome, "+accounts.get(0).getUsername()+" and "+(accounts.size()-1)+" more";
	}

	public void submitTokens(OA2Tokens tokens) {
		accounts.add(new Account(tokens, true));
		try { save(); } catch (IOException ignored) {}
	}

	private void loadAs(YCSConfig config) {
		accounts = config.accounts;
		setYoutubeKey(config.youtube_data_key);
	}

	public String getYoutubeKey() {
		return youtube_data_key;
	}
	private void setYoutubeKey(String key) {
		youtube_data_key = key;
	}
	
	public boolean isSetup() {
		return !youtube_data_key.equals("");
	}
	
	public void save() throws IOException {
		if(!CONFIG_FILE.exists()) {
			if(!CONFIG_FILE.createNewFile()) { System.err.println("Failed to create config file."); }
		}
		FileWriter fr = new FileWriter(CONFIG_FILE);
		fr.write(gson.toJson(this));
		fr.close();
	}
	
	public void load() throws IOException {
		if(!CONFIG_FILE.exists()) {
			save();
		}
		System.out.println("Loading config");
		BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE));
		StringBuilder json = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			json.append(line);
		}
		br.close();
		loadAs(gson.fromJson(json.toString(), YCSConfig.class));
	}
}
