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
	private final String youtube_data_key = "AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ";

	public List<Account> accounts = new ArrayList<>();
	public boolean downloadThumbs = false;
	public boolean prefixReplies = true;
	public boolean showWelcome = true;

	public YCSConfig() {}

	public boolean isSignedIn(String channelId) {
		return accounts.stream().anyMatch(acc -> acc.getChannelId().equals(channelId));
	}

	public String getWelcomeStatement() {
		if(accounts.isEmpty()) {
			return "Welcome, Guest";
		} else if(accounts.size() == 1) {
			return "Welcome, "+accounts.get(0).getUsername();
		} else
			return "Welcome, "+accounts.get(0).getUsername()+" and "+(accounts.size()-1)+" more";
	}

	public void submitTokens(OA2Tokens tokens) {
		accounts.add(new Account(tokens, true));
		try { save(); } catch (IOException ignored) {}
	}

	public String getYoutubeKey() {
		return youtube_data_key;
	}

	public boolean canDownloadThumbs() {
		return downloadThumbs;
	}

	public void setDownloadThumbs(boolean b) {
		downloadThumbs = b;
	}

	public boolean willPrefixReplies() {
		return prefixReplies;
	}

	public void setPrefixReplies(boolean b) {
		prefixReplies = b;
	}

	public boolean willShowWelcome() {
		return showWelcome;
	}

	public void setShowWelcome(boolean b) {
		showWelcome = b;
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

	private void loadAs(YCSConfig config) {
		accounts = config.accounts;
	}
}
