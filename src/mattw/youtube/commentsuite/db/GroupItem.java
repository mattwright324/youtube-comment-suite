package mattw.youtube.commentsuite.db;


import mattw.youtube.datav3.entrypoints.ChannelsList;
import mattw.youtube.datav3.entrypoints.PlaylistsList;
import mattw.youtube.datav3.entrypoints.SearchList;
import mattw.youtube.datav3.entrypoints.VideosList;

/**
 * Database entry for searched YouTube entries (Video, Channel, Playlist).
 * getYouTubeId() from YouTubeObject represents the GroupItem ID.
 *
 * @author mattwright324
 */
public class GroupItem extends YouTubeObject {

    public static String NO_ITEMS = "GI000";
    public static String ALL_ITEMS = "GI001";

    private String channelTitle;
    private long published;
    private long lastChecked;

    /**
     * Used for converting selected search items for inserting into database.
     */
    public GroupItem(SearchList.Item item) {
        super(item.id.getId(), item.snippet.title, item.snippet.thumbnails.getMedium().getURL().toString(), true);
        this.published = item.snippet.publishedAt.getTime();
        this.channelTitle = item.snippet.channelTitle;
        this.lastChecked = 0;
        if(item.id.videoId != null) setTypeId(YType.VIDEO);
        if(item.id.channelId != null) setTypeId(YType.CHANNEL);
        if(item.id.playlistId != null) setTypeId(YType.PLAYLIST);
    }

    /**
     * Group Management
     * @param item
     */
    public GroupItem(VideosList.Item item) {
        this(item.getId(), YType.VIDEO, item.snippet.title, item.snippet.channelTitle,
                item.snippet.thumbnails.getMedium().getURL().toString(), item.snippet.publishedAt.getTime(), 0);
    }

    /**
     * Group Management
     * @param item
     */
    public GroupItem(ChannelsList.Item item) {
        this(item.getId(), YType.CHANNEL, item.snippet.title, item.snippet.title,
                item.snippet.thumbnails.getMedium().getURL().toString(), item.snippet.publishedAt.getTime(), 0);
    }

    /**
     * Group Management
     * @param item
     */
    public GroupItem(PlaylistsList.Item item) {
        this(item.getId(), YType.PLAYLIST, item.snippet.title, item.snippet.channelTitle,
                item.snippet.thumbnails.getMedium().getURL().toString(), item.snippet.publishedAt.getTime(), 0);
    }

    /**
     * Used for "All Items (#)" and "No items" display in the "Comment Search" and "Group Manager" ComboBoxes.
     */
    public GroupItem(String gitemId, String title) {
        super(gitemId, title, null, false);
    }

    /**
     * Used for database init.
     */
    public GroupItem(String gitemId, YType typeId, String title, String channelTitle, String thumbUrl, long published, long lastChecked) {
        super(gitemId, title, thumbUrl, true);
        setTypeId(typeId);
        this.channelTitle = channelTitle;
        this.published = published;
        this.lastChecked = lastChecked;
    }

    public String getChannelTitle() { return channelTitle; }
    public long getPublished() { return published; }
    public long getLastChecked() { return lastChecked; }
    public void setLastChecked(long timestamp) { this.lastChecked = timestamp; }

    public String toString() { return getTitle(); }
}
