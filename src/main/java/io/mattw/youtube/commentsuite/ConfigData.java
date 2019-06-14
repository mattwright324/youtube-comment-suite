package io.mattw.youtube.commentsuite;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static javafx.application.Platform.runLater;

/**
 * @author mattwright324
 * @since 2018-12-30
 */
public class ConfigData implements Serializable {

    private transient String defaultApiKey = "AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ";
    private transient SimpleIntegerProperty accountListChanged = new SimpleIntegerProperty(0);

    private boolean autoLoadStats = true;
    private boolean prefixReplies = true;
    private boolean archiveThumbs = false;
    private boolean customApiKey = false;
    private List<YouTubeAccount> accounts = new ArrayList<>();
    private String youtubeApiKey = defaultApiKey;

    public ConfigData() {}

    public String getDefaultApiKey() {
        return defaultApiKey;
    }

    public String getYoutubeApiKey() {
        return customApiKey ? youtubeApiKey : defaultApiKey;
    }

    public void setYoutubeApiKey(String apiKey) {
        this.youtubeApiKey = apiKey;
    }

    public List<YouTubeAccount> getAccounts() {
        return accounts;
    }

    public void refreshAccounts() {
        accounts.forEach(YouTubeAccount::updateData);
    }

    public void addAccount(YouTubeAccount account) {
        if (accounts.stream().noneMatch(ac -> ac.getChannelId().equals(account.getChannelId()))) {
            accounts.add(account);
            triggerAccountListChanged();
        }
    }

    public void removeAccount(YouTubeAccount account) {
        if (accounts.removeIf(acc -> acc.getChannelId() != null && acc.getChannelId().equals(account.getChannelId()))) {
            triggerAccountListChanged();
        }
    }

    public ReadOnlyIntegerProperty accountListChangedProperty() {
        return accountListChanged;
    }

    public void triggerAccountListChanged() {
        runLater(() -> accountListChanged.setValue(accountListChanged.getValue() + 1));
    }

    public boolean usingCustomApiKey() {
        return customApiKey;
    }

    public void setCustomApiKey(boolean customApiKey) {
        this.customApiKey = customApiKey;
    }

    public boolean getArchiveThumbs() {
        return archiveThumbs;
    }

    public void setArchiveThumbs(boolean archiveThumbs) {
        this.archiveThumbs = archiveThumbs;
    }

    public boolean getPrefixReplies() {
        return prefixReplies;
    }

    public void setPrefixReplies(boolean prefixReplies) {
        this.prefixReplies = prefixReplies;
    }

    public boolean getAutoLoadStats() {
        return autoLoadStats;
    }

    public void setAutoLoadStats(boolean autoLoadStats) {
        this.autoLoadStats = autoLoadStats;
    }

}
