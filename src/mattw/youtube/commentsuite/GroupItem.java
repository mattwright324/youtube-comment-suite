package mattw.youtube.commentsuite;

import mattw.youtube.datav3.resources.SearchList;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Database entry for searched YouTube entries (Video, Channel, Playlist).
 */
public class GroupItem extends YouTubeObject {

    private String gitemId;
    private String youtubeId;
    private String title;
    private String thumbUrl;
    private long published;
    private long lastChecked;
    private boolean fetchThumb = false;

    public GroupItem(SearchList.Item item) {
        super(item.id.getId(), item.snippet.title, item.snippet.thumbnails.medium.url.toString(), true);
        this.gitemId = generateId();
        this.published = item.snippet.publishedAt.getTime();
        this.lastChecked = 0;
    }

    public GroupItem(String gitemId, String youtubeId, String title, String thumbUrl, boolean fetchThumb, long published, long lastChecked) {
        super(youtubeId, title, thumbUrl, fetchThumb);
        this.gitemId = gitemId;
    }

    private String generateId() {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(StandardCharsets.UTF_8.encode(String.valueOf(System.nanoTime())));
            return String.format("%032x", new BigInteger(1, md5.digest()));
        } catch (Exception e) {
            e.printStackTrace();
            return String.valueOf(System.nanoTime());
        }
    }
}
