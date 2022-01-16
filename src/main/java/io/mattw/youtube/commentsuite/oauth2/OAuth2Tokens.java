package io.mattw.youtube.commentsuite.oauth2;

import io.mattw.youtube.commentsuite.util.StringMask;

import java.io.Serializable;

public class OAuth2Tokens implements Serializable {

    private String access_token;
    private String token_type;
    private String refresh_token;
    private int expires_in;

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }

    public String getTokenType() {
        return token_type;
    }

    public void setTokenType(String token_type) {
        this.token_type = token_type;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void setRefreshToken(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public int getExpiresIn() {
        return expires_in;
    }

    public void setExpiresIn(int expires_in) {
        this.expires_in = expires_in;
    }

    @Override
    public String toString() {
        return "OAuth2Tokens{" +
                "access_token='" + StringMask.maskHalf(access_token) + '\'' +
                ", token_type='" + token_type + '\'' +
                ", refresh_token='" + StringMask.maskHalf(refresh_token) + '\'' +
                ", expires_in=" + expires_in +
                '}';
    }
}
