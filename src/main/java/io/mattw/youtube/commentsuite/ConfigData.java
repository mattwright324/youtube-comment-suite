package io.mattw.youtube.commentsuite;

import io.mattw.youtube.commentsuite.refresh.RefreshOptions;
import io.mattw.youtube.commentsuite.util.StringMask;

import java.io.Serializable;

public class ConfigData implements Serializable {

    public static final transient String DEFAULT_API_KEY = "AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ";
    public static final transient String FAST_GROUP_ADD_THUMB_PLACEHOLDER = "~";

    private boolean archiveThumbs = false;
    private boolean autoLoadStats = true;
    private boolean customApiKey = false;
    private boolean fastGroupAdd = false;
    private boolean filterDuplicatesOnCopy = true;
    private RefreshOptions refreshOptions = new RefreshOptions();
    private String youtubeApiKey = DEFAULT_API_KEY;

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

    public String getApiKeyOrDefault() {
        return isCustomApiKey() ? getYoutubeApiKey() : DEFAULT_API_KEY;
    }

    @Override
    public String toString() {
        return "ConfigData{" +
                ", archiveThumbs=" + archiveThumbs +
                ", autoLoadStats=" + autoLoadStats +
                ", customApiKey=" + customApiKey +
                ", fastGroupAdd=" + fastGroupAdd +
                ", filterDuplicatesOnCopy=" + filterDuplicatesOnCopy +
                ", refreshOptions=" + refreshOptions +
                ", youtubeApiKey='" + StringMask.maskHalf(youtubeApiKey) + '\'' +
                '}';
    }
}
