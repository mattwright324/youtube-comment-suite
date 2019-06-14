package io.mattw.youtube.commentsuite;

import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mattw.youtube.commentsuite.util.UTF8UrlEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * @since 2019-01-21
 * @author mattwright324
 */
public class OAuth2Handler {

    private Logger logger = LogManager.getLogger(this);

    private Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL).create();
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authUrl;
    private OAuth2Tokens tokens;

    OAuth2Handler() {}

    OAuth2Handler(String clientId, String clientSecret, String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;

        this.authUrl = String.format("https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&&response_type=code&&scope=%s",
                clientId,
                UTF8UrlEncoder.encode(redirectUri),
                UTF8UrlEncoder.encode("https://www.googleapis.com/auth/youtube.force-ssl"));
    }

    public OAuth2Tokens getTokens() {
        return tokens;
    }

    public void setTokens(OAuth2Tokens tokens) {
        this.tokens = tokens;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public OAuth2Tokens getAccessTokens(String code) throws IOException {
        Document doc = Jsoup.connect("https://accounts.google.com/o/oauth2/token")
                .ignoreContentType(true)
                .data("code", code)
                .data("client_id", clientId)
                .data("client_secret", clientSecret)
                .data("redirect_uri", redirectUri)
                .data("grant_type", "authorization_code")
                .post();
        return gson.fromJson(doc.text(), OAuth2Tokens.class);
    }

    public void refreshTokens() throws IOException {
        Document doc = Jsoup.connect("https://accounts.google.com/o/oauth2/token")
                .ignoreContentType(true)
                .data("client_id", clientId)
                .data("client_secret", clientSecret)
                .data("refresh_token", tokens.getRefreshToken())
                .data("grant_type", "refresh_token")
                .post();
        OAuth2Tokens newTokens = gson.fromJson(doc.text(), OAuth2Tokens.class);
        newTokens.setRefreshToken(tokens.getRefreshToken());
        setTokens(newTokens);
    }

    /**
     * Attempts to send a reply to the parent comment id and text supplied. It will attempt to send to reply 10 times
     * before failure and throwing an error. On each failure, if it detects the tokens used by the account have
     * expired, it will attempt to refresh them and use and newly updated tokens.
     *
     * @param parentId id of comment or parentId of reply-comment to reply to
     * @param textOriginal text to reply to the comment with
     * @throws IOException failed to reply
     */
    public Comment postReply(String parentId, String textOriginal) throws IOException {
        CommentSnippet snippet = new CommentSnippet();
        snippet.setParentId(parentId);
        snippet.setTextOriginal(textOriginal);

        Comment comment = new Comment();
        comment.setSnippet(snippet);

        int attempt = 0;
        do {
            try {
                Comment result = FXMLSuite.getYouTube().comments()
                        .insert("snippet", comment)
                        .setOauthToken(tokens.getAccessToken())
                        .execute();

                return result;
            } catch (IOException e) {
                logger.warn("Failed on comment reply, {}", e.getLocalizedMessage());
                logger.debug("Refreshing tokens and trying again [attempt={}]", attempt);

                refreshTokens();
            }

            attempt++;
        } while(attempt < 10);

        throw new IOException("Could not reply and failed to refresh tokens.");
    }
}
