package mattw.youtube.commentsuite;

import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Config {

    private File file;
    private Data data = new Data();
    private Gson gson = new Gson();

    public Config(String file) {
        this.file = new File(file);
        if (this.file.exists()) {
            load();
        } else {
            save();
        }
    }

    public void save() {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(gson.toJson(this.data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
            String line;
            StringBuilder text = new StringBuilder();
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            this.data = gson.fromJson(text.toString(), Data.class);
        } catch (Exception e) {
            e.printStackTrace();
            this.data = new Data();
        }
    }

    protected class Data {
        public Data() {}
        public boolean prefix_replies = true;
        public List<YouTubeAccount> accounts = new ArrayList<>();
    }

    public boolean isSignedIn(String channelId) { return getAccounts().stream().anyMatch(acc -> acc.channelId.equals(channelId)); }
    public void setPrefixReplies(boolean prefix) { data.prefix_replies = prefix; }
    public boolean prefixReplies() { return data.prefix_replies; }
    public List<YouTubeAccount> getAccounts() { return data.accounts; }
}
