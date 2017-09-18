package mattw.youtube.commentsuite;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Similarities between GroupItem, YouTubeChannel, YouTubeComment, and YouTubeVideo.
 */
abstract class YouTubeObject {

    private static Map<String,Image> thumbCache = new HashMap<>();

    private String youtubeId;
    private String title;
    private String thumbUrl;
    private boolean fetchThumb;

    public YouTubeObject(String youtubeId, String title, String thumbUrl, boolean fetchThumb) {
        this.youtubeId = youtubeId;
        this.title = title;
        this.thumbUrl = thumbUrl;
        this.fetchThumb = fetchThumb;
    }

    /**
     * Caches thumbs when grabbed.
     */
    public Image getThumbnail() {
        if(!thumbCache.containsKey(youtubeId)) {
            thumbCache.put(youtubeId, new Image(thumbUrl));
        }
        return thumbCache.get(youtubeId);
    }

    public static void clearThumbCache() { thumbCache.clear(); }

    public String getYouTubeId() { return youtubeId; }
    public String getTitle() { return title; }
    public String getThumbUrl() { return thumbUrl; }
    public boolean fetchThumb() { return fetchThumb; }

    public String getString() { return getYouTubeId(); }
}
