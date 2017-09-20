package mattw.youtube.commentsuite;

import mattw.youtube.datav3.resources.SearchList;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Database entry for searched YouTube entries (Video, Channel, Playlist).
 */
public class GroupItem extends YouTubeObject {

    public static String NO_ITEMS = "GI000";
    public static String ALL_ITEMS = "GI001";

    private String gitemId;
    private String channelTitle;
    private long published;
    private long lastChecked;

    /**
     * Used for converting selected search items for inserting into database.
     */
    public GroupItem(SearchList.Item item) {
        super(item.id.getId(), item.snippet.title, item.snippet.thumbnails.medium.url.toString(), true);
        this.gitemId = generateId();
        this.published = item.snippet.publishedAt.getTime();
        this.lastChecked = 0;
        if(item.id.videoId != null) typeId = 1;
        if(item.id.channelId != null) typeId = 2;
        if(item.id.playlistId != null) typeId = 3;
    }

    /**
     * Used for "All Items (#)" and "No items" display in the "Comment Search" and "Group Manager" ComboBoxes.
     */
    public GroupItem(String gitemId, String title) {
        super(null, title, null, false);
        this.gitemId = gitemId;
    }

    /**
     * Used for database init.
     */
    public GroupItem(String gitemId, int typeId, String youtubeId, String title, String channelTitle, String thumbUrl, long published, long lastChecked) {
        super(youtubeId, title, thumbUrl, true);
        this.typeId = typeId;
        this.gitemId = gitemId;
        this.channelTitle = channelTitle;
        this.published = published;
        this.lastChecked = lastChecked;
    }

    public String getItemId() { return gitemId; }
    public String getChannelTitle() { return channelTitle; }
    public long getPublished() { return published; }
    public long getLastChecked() { return lastChecked; }

    private String generateId() {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(StandardCharsets.UTF_8.encode(String.valueOf(System.nanoTime())+this.channelTitle));
            return String.format("%032x", new BigInteger(1, md5.digest()));
        } catch (Exception e) {
            e.printStackTrace();
            return String.valueOf(System.nanoTime());
        }
    }

    public String toString() { return getTitle(); }
}
