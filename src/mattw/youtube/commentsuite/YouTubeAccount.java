package mattw.youtube.commentsuite;

import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.ChannelsList;

import java.io.IOException;

/**
 * Combination of YouTubeChannel and OAuth2Tokens as sign-in.
 * Data stored in Config 'commentsuite.json'
 */
public class YouTubeAccount {

    public String username;
    public String channelId;
    public String thumbUrl;
    public OAuth2Tokens tokens;

    /**
     * Only used with "YouTube Account Sign-in," otherwise initialized by Gson & Config.
     */
    public YouTubeAccount(OAuth2Tokens tokens) {
        this.tokens = tokens;
        updateData();
    }

    public void updateData() {
        CommentSuite.youtube().setProfileAccessToken(tokens.access_token);
        try {
            ChannelsList cl = CommentSuite.youtube().channelsList().getMine(ChannelsList.PART_SNIPPET, "");
            ChannelsList.Item cli = cl.items[0];
            this.channelId = cli.getId();
            if(cli.hasSnippet()) {
                this.username = cli.snippet.title;
                this.thumbUrl = cli.snippet.thumbnails.medium.url.toString();
            }
        } catch (IOException | YouTubeErrorException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() { return username; }
    public String getChannelId() { return channelId; }
    public String getThumbUrl() { return thumbUrl; }

    public String toString() { return username; }

    public OAuth2Tokens getTokens() { return tokens; }
    public void setTokens(OAuth2Tokens tokens) { this.tokens = tokens; }

    public boolean equals(Object o) {
        return o != null && o instanceof YouTubeAccount && ((YouTubeAccount) o).getChannelId() != null && ((YouTubeAccount) o).getChannelId().equals(channelId);
    }
}
