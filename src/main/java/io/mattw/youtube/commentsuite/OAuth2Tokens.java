package io.mattw.youtube.commentsuite;

import java.io.Serializable;

/**
 * @author mattwright324
 */
public class OAuth2Tokens implements Serializable {

    private String access_token, token_type, refresh_token;
    private int expires_in;

    public String getAccessToken() {
        return access_token;
    }

    public String getTokenType() {
        return token_type;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void setRefreshToken(String token) {
        refresh_token = token;
    }

    public int getExpiresIn() {
        return expires_in;
    }
}
