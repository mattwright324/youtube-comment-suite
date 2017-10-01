package mattw.youtube.commentsuite;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Similarities between GroupItem, YouTubeChannel, YouTubeComment, and YouTubeVideo.
 */
abstract class YouTubeObject {

    protected static File thumbFolder = new File("thumbs/");
    protected static Map<String,Image> thumbCache = new HashMap<>();

    protected int typeId = -1;
    private String youtubeId;
    private String title;
    private String thumbUrl;
    private boolean fetchThumb;

    public YouTubeObject(String youtubeId, String title, String thumbUrl, boolean fetchThumb) {
        this.youtubeId = youtubeId;
        this.title = title;
        this.thumbUrl = thumbUrl;
        this.fetchThumb = fetchThumb;
        if(CommentSuite.config().downloadThumbs() && fetchThumb) {
            getThumbnail();
        }
    }


    public boolean thumbCached() { return thumbCache.containsKey(youtubeId); }

    /**
     * Caches thumbs when grabbed.
     */
    public Image getThumbnail() {
        File thumbFile = new File(thumbFolder, youtubeId+".jpg");
        if(CommentSuite.config().downloadThumbs()) {
            try {
                if(!thumbFile.exists()) {
                    thumbFolder.mkdirs();
                    thumbFile.createNewFile();
                    ImageIO.write(ImageIO.read(new URL(thumbUrl)), "jpg", thumbFile);
                }
            } catch (Exception ignored) {}
        }
        if(!thumbCached()) {
            Image image = new Image(thumbFile.exists() ? "file:///"+thumbFile.getAbsolutePath() : thumbUrl);
            if(image.isError()) {
                if(typeId == 0)
                    return CommentSuite.IMG_VID_PLACEHOLDER;
                else
                    return CommentSuite.IMG_BLANK_PROFILE;

            }
            thumbCache.put(youtubeId, image);
        }
        return thumbCache.get(youtubeId);
    }

    public String getYouTubeLink() {
        switch(typeId){
            case 0:  return "https://youtu.be/"+youtubeId;
            case 1:  return "https://www.youtube.com/channel/"+youtubeId;
            case 2:  return "https://www.youtube.com/playlist?list="+youtubeId;
            case 3:  return "https://www.youtube.com/watch?v="+(this instanceof YouTubeComment ? ((YouTubeComment) this).getVideoId()+"&lc="+youtubeId : youtubeId);
            default: return "https://www.youtube.com/error/"+youtubeId;
        }
    }

    public String getTypeName() {
        switch(typeId) {
            case 0: return "Video";
            case 1: return "Channel";
            case 2: return "Playlist";
            case 3: return "Comment";
            default: return "Error";
        }
    }

    public static void clearThumbCache() { thumbCache.clear(); }

    public String getYouTubeId() { return youtubeId; }
    public String getTitle() { return title; }
    public String getThumbUrl() { return thumbUrl; }
    public boolean fetchThumb() { return fetchThumb; }

    public String toString() { return getYouTubeId(); }

    public boolean equals(Object o) {
        return o != null && o instanceof YouTubeObject && ((YouTubeObject) o).getYouTubeId() != null && ((YouTubeObject) o).getYouTubeId().equals(youtubeId);
    }
}
