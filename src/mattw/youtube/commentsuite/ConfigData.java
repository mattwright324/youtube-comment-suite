package mattw.youtube.commentsuite;

import java.util.ArrayList;
import java.util.List;

public class ConfigData  {

    public transient String defaultApiKey = "AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ";

    public boolean autoLoadStats = true;
    public boolean prefixReplies = true;
    public boolean archiveThumbs = false;
    public boolean customApiKey = false;
    public List<YouTubeAccount> accounts = new ArrayList<>();
    public String youtubeApiKey = defaultApiKey;

    public ConfigData() {}

    public String getDefaultApiKey() { return defaultApiKey; }

    public String getYoutubeApiKey() { return customApiKey ? youtubeApiKey : defaultApiKey; }
    public void setYoutubeApiKey(String apiKey) { this.youtubeApiKey = apiKey; }

    public List<YouTubeAccount> getAccounts() { return accounts; }

    public boolean usingCustomApiKey() { return customApiKey; }
    public void setCustomApiKey(boolean customApiKey) { this.customApiKey = customApiKey; }

    public boolean getArchiveThumbs() { return archiveThumbs; }
    public void setArchiveThumbs(boolean archiveThumbs) { this.archiveThumbs = archiveThumbs; }

    public boolean getPrefixReplies() { return prefixReplies; }
    public void setPrefixReplies(boolean prefixReplies) { this.prefixReplies = prefixReplies; }

    public boolean getAutoLoadStats() { return autoLoadStats; }
    public void setAutoLoadStats(boolean autoLoadStats) { this.autoLoadStats = autoLoadStats; }

}
