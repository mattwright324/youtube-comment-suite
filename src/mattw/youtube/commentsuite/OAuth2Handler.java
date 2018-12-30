package mattw.youtube.commentsuite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mattw.youtube.commentsuite.io.UTF8UrlEncoder;
import mattw.youtube.datav3.entrypoints.CommentsList;
import mattw.youtube.datav3.entrypoints.YouTubeErrorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class OAuth2Handler {

    private Logger logger = LogManager.getLogger(this);

    /**
     * Payload object converted to JSON when making replies.
     */
    private class MakeReply implements Serializable {
        private class Snippet implements Serializable {
            String parentId, textOriginal;
        }
        private Snippet snippet = new Snippet();
        private MakeReply(String parentId, String textOriginal) {
            snippet.parentId = parentId;
            snippet.textOriginal = textOriginal;
        }
    }

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
     * Posts a reply to a comment, parentId.
     */
    public CommentsList.Item postReply(String parentId, String textOriginal) throws IOException, YouTubeErrorException {
        String payload = gson.toJson(new MakeReply(parentId, textOriginal));
        String replyUrl = String.format("https://www.googleapis.com/youtube/v3/comments?part=snippet&access_token=%s",
                tokens.getAccessToken());

        int attempt = 0;
        do {
            HttpsURLConnection conn = (HttpsURLConnection) new URL(replyUrl).openConnection();
            try {
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.connect();

                try(OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                    if(conn.getResponseCode() == 200) {
                        String response = streamToString(conn.getInputStream());

                        return gson.fromJson(response, CommentsList.Item.class);
                    } else if(conn.getResponseCode() == 401) {
                        logger.debug("Refreshing tokens and trying again [attempt={}]", attempt);

                        refreshTokens();
                    } else {
                        String response = streamToString(conn.getErrorStream());

                        logger.warn("Issue when making reply, [code={}, response={}]",
                                conn.getResponseCode(),
                                response);

                        throw gson.fromJson(response, YouTubeErrorException.class);
                    }
                }
            } finally {
                conn.disconnect();
            }

            attempt++;
        } while(attempt < 10);

        throw new IOException("Could not reply and failed to refresh tokens.");
    }

    private String streamToString(InputStream is) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] b = new byte[65535];
            int n;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
