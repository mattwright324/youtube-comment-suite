package io.mattw.youtube.commentsuite;

import io.mattw.youtube.commentsuite.events.AccountAddEvent;
import io.mattw.youtube.commentsuite.events.AccountDeleteEvent;
import io.mattw.youtube.commentsuite.oauth2.YouTubeAccount;
import io.mattw.youtube.commentsuite.refresh.RefreshOptions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConfigData implements Serializable {

    public static final transient String DEFAULT_API_KEY = "AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ";
    public static final transient String FAST_GROUP_ADD_THUMB_PLACEHOLDER = "~";

    private List<YouTubeAccount> accounts = new ArrayList<>();
    private boolean archiveThumbs = false;
    private boolean autoLoadStats = true;
    private boolean customApiKey = false;
    private boolean fastGroupAdd = false;
    private boolean filterDuplicatesOnCopy = true;
    private boolean grabHeldForReview = false;
    private boolean prefixReplies = true;
    private RefreshOptions refreshOptions = new RefreshOptions();
    private String youtubeApiKey = DEFAULT_API_KEY;

    public List<YouTubeAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<YouTubeAccount> accounts) {
        this.accounts = accounts;
    }

    public boolean isArchiveThumbs() {
        return archiveThumbs;
    }

    public void setArchiveThumbs(boolean archiveThumbs) {
        this.archiveThumbs = archiveThumbs;
    }

    public boolean isAutoLoadStats() {
        return autoLoadStats;
    }

    public void setAutoLoadStats(boolean autoLoadStats) {
        this.autoLoadStats = autoLoadStats;
    }

    public boolean isCustomApiKey() {
        return customApiKey;
    }

    public void setCustomApiKey(boolean customApiKey) {
        this.customApiKey = customApiKey;
    }

    public boolean isFastGroupAdd() {
        return fastGroupAdd;
    }

    public void setFastGroupAdd(boolean fastGroupAdd) {
        this.fastGroupAdd = fastGroupAdd;
    }

    public boolean isFilterDuplicatesOnCopy() {
        return filterDuplicatesOnCopy;
    }

    public void setFilterDuplicatesOnCopy(boolean filterDuplicatesOnCopy) {
        this.filterDuplicatesOnCopy = filterDuplicatesOnCopy;
    }

    public boolean isGrabHeldForReview() {
        return grabHeldForReview;
    }

    public void setGrabHeldForReview(boolean grabHeldForReview) {
        this.grabHeldForReview = grabHeldForReview;
    }

    public boolean isPrefixReplies() {
        return prefixReplies;
    }

    public void setPrefixReplies(boolean prefixReplies) {
        this.prefixReplies = prefixReplies;
    }

    public RefreshOptions getRefreshOptions() {
        return refreshOptions;
    }

    public void setRefreshOptions(RefreshOptions refreshOptions) {
        this.refreshOptions = refreshOptions;
    }

    public String getYoutubeApiKey() {
        return youtubeApiKey;
    }

    public void setYoutubeApiKey(String youtubeApiKey) {
        this.youtubeApiKey = youtubeApiKey;
    }

    public void addAccount(final YouTubeAccount account) {
        if (accounts.stream().noneMatch(ac -> ac.getChannelId().equals(account.getChannelId()))) {
            accounts.add(account);
            CommentSuite.getEventBus().post(new AccountAddEvent(account));
        }
    }

    public void removeAccount(final YouTubeAccount account) {
        if (accounts.removeIf(acc -> acc.getChannelId() != null && acc.getChannelId().equals(account.getChannelId()))) {
            CommentSuite.getEventBus().post(new AccountDeleteEvent(account));
        }
    }

    public boolean isSignedIn(final String channelId) {
        return accounts.stream().anyMatch(acc -> channelId.equals(acc.getChannelId()));
    }

    public YouTubeAccount getAccount(final String channelId) {
        return accounts.stream()
                .filter(acc -> channelId.equals(acc.getChannelId()))
                .findFirst()
                .orElse(null);
    }

}
