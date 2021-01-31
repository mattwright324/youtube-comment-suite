package io.mattw.youtube.commentsuite.oauth2;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.ConfigFile;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import io.mattw.youtube.commentsuite.util.UTF8UrlEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;

public class OAuth2Manager {

    private static final Logger logger = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL).create();

    private static final String OAUTH2_TOKENS_URL = "https://accounts.google.com/o/oauth2/token";
    private static final String OAUTH2_REVOKE_URL = "https://accounts.google.com/o/oauth2/revoke";
    private static final String CLIENT_ID = "972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "QuTdoA-KArupKMWwDrrxOcoS";
    private static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

    public static final String WEB_LOGIN_URL = String.format("https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&&response_type=code&&scope=%s",
            CLIENT_ID, UTF8UrlEncoder.encode(REDIRECT_URI), UTF8UrlEncoder.encode("https://www.googleapis.com/auth/youtube.force-ssl"));

    private final YouTube youTube;
    private final ConfigFile<ConfigData> configFile;
    private final ConfigData configData;
    private final CommentDatabase database;

    public OAuth2Manager() {
        this.youTube = CommentSuite.getYouTube();
        this.configFile = CommentSuite.getConfig();
        this.configData = configFile.getDataObject();
        this.database = CommentSuite.getDatabase();
    }

    /**
     * Create account tokens and details from login authorization code retrieved from {@link #WEB_LOGIN_URL}
     */
    public YouTubeAccount addAccount(final String authorizationCode) throws IOException {
        final OAuth2Tokens tokens = getOAuthTokens(authorizationCode);
        final YouTubeAccount account = new YouTubeAccount(tokens);

        getAndSetAccountDetails(account);

        configData.addAccount(account);
        configFile.save();

        return account;
    }

    /**
     * Get a new access token on the account because the current one has expired.
     */
    public YouTubeAccount getNewAccessToken(final YouTubeAccount account) throws IOException {
        final OAuth2Tokens newTokens = getNewAccessToken(account.getTokens());

        account.setTokens(newTokens);
        configData.getAccount(account.getChannelId()).setTokens(newTokens);
        configFile.save();

        return account;
    }

    /**
     * Revoke access to this account
     */
    public void revokeAccessTo(final YouTubeAccount account) throws IOException {
        logger.debug("Revoking access to account [name={}]", account.getUsername());

        final Document document = Jsoup.connect(OAUTH2_REVOKE_URL)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .data("token", account.getTokens().getAccessToken())
                .get();
        logger.debug(document.text());
    }

    private Connection getOauth2Connection() {
        return Jsoup.connect(OAUTH2_TOKENS_URL)
                .ignoreContentType(true)
                .data("client_id", CLIENT_ID)
                .data("client_secret", CLIENT_SECRET);
    }

    private OAuth2Tokens getOAuthTokens(final String authorizationCode) throws IOException {
        logger.debug("Grabbing OAuth2Tokens from authorization code");
        final Document result = getOauth2Connection()
                .data("code", authorizationCode)
                .data("redirect_uri", REDIRECT_URI)
                .data("grant_type", "authorization_code")
                .post();

        return gson.fromJson(result.text(), OAuth2Tokens.class);
    }

    private OAuth2Tokens getNewAccessToken(final OAuth2Tokens oldTokens) throws IOException {
        logger.debug("Refreshing OAuth2 Access Token that has expired {}", oldTokens);
        final Document result = getOauth2Connection()
                .data("refresh_token", oldTokens.getRefreshToken())
                .data("grant_type", "refresh_token")
                .post();

        final OAuth2Tokens newTokens = gson.fromJson(result.text(), OAuth2Tokens.class);
        newTokens.setRefreshToken(oldTokens.getRefreshToken());
        return newTokens;
    }

    private void getAndSetAccountDetails(final YouTubeAccount account) throws IOException {
        logger.debug("Grabbing account details");

        try {
            final ChannelListResponse response = youTube.channels()
                    .list("snippet")
                    .setOauthToken(account.getTokens().getAccessToken())
                    .setMine(true)
                    .execute();

            if (response.getItems() == null || response.getItems().isEmpty()) {
                throw new IOException("Response had no channel");
            }

            final Channel channelItem = response.getItems().get(0);
            final ChannelSnippet snippet = channelItem.getSnippet();

            if (snippet == null) {
                throw new IOException("Channel had no snippet");
            }

            account.setChannelId(channelItem.getId());
            account.setUsername(snippet.getTitle());
            account.setThumbUrl(Optional.ofNullable(snippet.getThumbnails())
                    .map(ThumbnailDetails::getMedium)
                    .map(Thumbnail::getUrl)
                    .orElse("https://placehold.it/128x128"));

            try {
                final YouTubeChannel channel = new YouTubeChannel(channelItem);
                database.channels().insert(channel);
                database.commit();
            } catch (SQLException e) {
                logger.error("Unable to insert account channel into database.", e);
            }
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 401) {
                logger.warn("Tokens have expired for account [username={}]", account.getUsername());
            } else {
                logger.error("An unexpected error occurred.");
                throw e;
            }
        } catch (IOException e) {
            logger.error("Failed to query for account channel info.");
            throw e;
        }
    }

}
