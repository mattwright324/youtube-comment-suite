package mattw.youtube.commentsuite;

public class OAuth2Tokens {
    public String access_token;
    public String token_type;
    public int expires_in;
    public String refresh_token;

    public void setRefreshToken(String token) {
        refresh_token = token;
    }
}
