package mattw.youtube.commentsuite;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Config {

    private static Logger logger = LogManager.getLogger(Config.class.getSimpleName());

    private File file;
    private Data data = new Data();
    private Gson gson = new Gson();

    public Config(String file) {
        logger.debug(String.format("Initialize Config [file=%s]", file));
        this.file = new File(file);
        if (this.file.exists()) {
            load();
        } else {
            save();
        }
    }

    public void save() {
        logger.debug(String.format("Saving Config File"));
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(gson.toJson(this.data));
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void load() {
        logger.debug(String.format("Loading Config File"));
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
            String line;
            StringBuilder text = new StringBuilder();
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            this.data = gson.fromJson(text.toString(), Data.class);
        } catch (Exception e) {
            logger.error(e);
            logger.error("Problem loading existing config, using default settings.");
            this.data = new Data();
        }
    }

    protected class Data {
        public Data() {}
        public boolean auto_load_stats = true;
        public boolean prefix_replies = true;
        public boolean download_thumbs = false;
        public List<YouTubeAccount> accounts = new ArrayList<>();
    }

    public boolean isSignedIn(String channelId) { return getAccounts().stream().anyMatch(acc -> acc.channelId.equals(channelId)); }
    public void setPrefixReplies(boolean prefix) { data.prefix_replies = prefix; }
    public boolean prefixReplies() { return data.prefix_replies; }

    public void setAutoLoadStats(boolean load) { data.auto_load_stats = load; }
    public boolean autoLoadStats() { return data.auto_load_stats; }

    public void setDownloadThumbs(boolean download) { data.download_thumbs = download; }
    public boolean downloadThumbs() { return data.download_thumbs; }

    public List<YouTubeAccount> getAccounts() { return data.accounts; }
}
