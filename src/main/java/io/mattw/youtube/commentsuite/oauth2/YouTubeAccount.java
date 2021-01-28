package io.mattw.youtube.commentsuite.oauth2;

import java.io.Serializable;

public class YouTubeAccount implements Serializable {

    private String username;
    private String channelId;
    private String thumbUrl;
    private OAuth2Tokens tokens;

    public YouTubeAccount(OAuth2Tokens tokens) {
        this.tokens = tokens;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public OAuth2Tokens getTokens() {
        return tokens;
    }

    public void setTokens(OAuth2Tokens tokens) {
        this.tokens = tokens;
    }

    public String toString() {
        return username;
    }

    public boolean equals(Object o) {
        return o instanceof YouTubeAccount && ((YouTubeAccount) o).getChannelId() != null && ((YouTubeAccount) o).getChannelId().equals(channelId);
    }

}
