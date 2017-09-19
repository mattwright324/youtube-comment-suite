package mattw.youtube.commentsuite;

import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.ChannelsList;

import java.io.IOException;

/**
 *
 */
public class YouTubeAccount {

    public String username;
    public String channelId;
    public String thumbUrl;
    public OAuth2Tokens tokens;

    public YouTubeAccount(OAuth2Tokens tokens) {
        this.tokens = tokens;
        updateData();
    }

    public void updateData() {
        CommentSuite.youtube().setProfileAccessToken(tokens.access_token);
        try {
            ChannelsList cl = CommentSuite.youtube().channelsList().getMine(ChannelsList.PART_SNIPPET, "");
            ChannelsList.Item cli = cl.items[0];
            this.username = cli.snippet.title;
            this.channelId = cli.getId();
            this.thumbUrl = cli.snippet.thumbnails.medium.url.toString();
        } catch (IOException | YouTubeErrorException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() { return username; }
    public String getChannelId() { return channelId; }
    public String getThumbUrl() { return thumbUrl; }

    public OAuth2Tokens getTokens() { return tokens; }
    public void setTokens(OAuth2Tokens tokens) { this.tokens = tokens; }
}
