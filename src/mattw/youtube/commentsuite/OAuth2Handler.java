package mattw.youtube.commentsuite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.CommentsList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLEncoder;

public class OAuth2Handler {

    private Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL).create();
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    private OAuth2Tokens tokens;

    public OAuth2Handler(String clientId, String clientSecret, String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public OAuth2Tokens getTokens() { return tokens; }
    public void setTokens(OAuth2Tokens tokens) { this.tokens = tokens; }

    public String getAuthURL() throws UnsupportedEncodingException {
        return String.format("https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&&response_type=code&&scope=%s",
                clientId,
                URLEncoder.encode(redirectUri, "UTF-8"),
                URLEncoder.encode("https://www.googleapis.com/auth/youtube.force-ssl", "UTF-8"));
    }

    public void getAccessTokens(String code) throws IOException {
        Document doc = Jsoup.connect("https://accounts.google.com/o/oauth2/token")
                .ignoreContentType(true)
                .data("code", code)
                .data("client_id", clientId)
                .data("client_secret", clientSecret)
                .data("redirect_uri", redirectUri)
                .data("grant_type", "authorization_code")
                .post();
        setTokens(gson.fromJson(doc.text(), OAuth2Tokens.class));
    }

    public void refreshTokens() throws IOException {
        Document doc = Jsoup.connect("https://accounts.google.com/o/oauth2/token")
                .ignoreContentType(true)
                .data("client_id", clientId)
                .data("client_secret", clientSecret)
                .data("refresh_token", tokens.refresh_token)
                .data("grant_type", "refresh_token")
                .post();
        OAuth2Tokens newTokens = gson.fromJson(doc.text(), OAuth2Tokens.class);
        newTokens.setRefreshToken(tokens.refresh_token);
        setTokens(newTokens);
    }

    class MakeReply {
        public MakeReply(String parentId, String textOriginal) {
            snippet = new Snippet();
            snippet.parentId = parentId;
            snippet.textOriginal = textOriginal;
        }
        public final Snippet snippet;
        public class Snippet {
            public String parentId;
            public String textOriginal;
        }
    }

    public CommentsList.Item postReply(String parentId, String textOriginal) throws IOException, YouTubeErrorException {
        String payload = gson.toJson(new MakeReply(parentId, textOriginal));
        boolean tryAgain = false;
        do {
            HttpsURLConnection conn = (HttpsURLConnection) new URL("https://www.googleapis.com/youtube/v3/comments?part=snippet&access_token="+tokens.access_token).openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.connect();
            OutputStream os = conn.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            String response = new String(toByteArray(conn.getInputStream()), "UTF-8");
            if(conn.getResponseCode() < HttpsURLConnection.HTTP_BAD_REQUEST) {
                return gson.fromJson(response, CommentsList.Item.class);
            } else if(conn.getResponseCode() == 401) {
                tryAgain = true;
                System.out.println("Refreshing tokens and trying again.");
            } else {
                throw gson.fromJson(response, YouTubeErrorException.class);
            }
            os.close();
            conn.disconnect();
        } while(tryAgain);
        return null;
    }

    public byte[] toByteArray(InputStream is) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] b = new byte[4096];
            int n;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        }
    }
}
