package mattw.youtube.commentsuite;

import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.YouTubeChannel;
import mattw.youtube.datav3.Parts;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.entrypoints.ChannelsList;
import mattw.youtube.datav3.entrypoints.YouTubeErrorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;

/**
 * Combination of YouTubeChannel and OAuth2Tokens as sign-in.
 * Data stored in Config 'commentsuite.json'
 *
 * @author mattwright324
 */
public class YouTubeAccount implements Serializable {

    private Logger logger = LogManager.getLogger(this);

    private String username, channelId, thumbUrl;
    private OAuth2Tokens tokens;

    /**
     * Only used with "YouTube Account Sign-in," otherwise initialized by Gson & Config.
     */
    public YouTubeAccount(OAuth2Tokens tokens) {
        this.tokens = tokens;

        updateData();
    }

    /**
     * Using the OAuth2Tokens passed in, query the YouTube API to get the "mine"
     * channel for those tokens.
     *
     * Pushes the channel to the database.
     */
    void updateData() {
        CommentDatabase database = FXMLSuite.getDatabase();
        YouTubeData3 youtube = FXMLSuite.getYoutubeApi();

        logger.debug("Getting account data for [accessToken={}]", this.tokens.getAccessToken().substring(0,10)+"...");

        String oldAccessToken = youtube.getProfileAccessToken();
        String newAccessToken = getTokens().getAccessToken();

        youtube.setProfileAccessToken(newAccessToken);

        try {
            ChannelsList cl = ((ChannelsList) youtube.channelsList().part(Parts.SNIPPET)).getMine("");
            ChannelsList.Item cli = cl.items[0];

            this.channelId = cli.getId();

            if(cli.hasSnippet()) {
                this.username = cli.snippet.title;
                this.thumbUrl = cli.snippet.thumbnails.getMedium().getURL().toString();

                try {
                    YouTubeChannel channel = new YouTubeChannel(cli);
                    database.insertChannels(Collections.singletonList(channel));
                    database.commit();
                } catch (SQLException e) {
                    logger.error("Unable to insert account channel into database.", e);
                }
            }
        } catch (YouTubeErrorException | IOException e) {
            logger.error("Failed to query for account channel info.", e);
        } finally {
            if(oldAccessToken == null || oldAccessToken.trim().isEmpty()) {
                youtube.setProfileAccessToken(newAccessToken);
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public String toString() {
        return username;
    }

    public OAuth2Tokens getTokens() {
        return tokens;
    }

    public void setTokens(OAuth2Tokens tokens) {
        this.tokens = tokens;
    }

    public boolean equals(Object o) {
        return o instanceof YouTubeAccount && ((YouTubeAccount) o).getChannelId() != null && ((YouTubeAccount) o).getChannelId().equals(channelId);
    }
}
