package mattw.youtube.commensuitefx;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

abstract class YoutubeObject {

	private static final Map<String,Image> THUMBCACHE = new HashMap<>();

	public int typeId;
	final private String youtubeId;
	final private String title;
	final private String thumbUrl;

	final private boolean willFetch;
	final private Image image;

	YoutubeObject(String youtubeId, String title) {
		this(youtubeId, title, null, false);
	}

	YoutubeObject(String youtubeId, String title, String thumbUrl, boolean fetchThumb) {
		this.youtubeId = youtubeId != null ? youtubeId : "";
		this.title = title;
		this.thumbUrl = thumbUrl;
		this.willFetch = fetchThumb;
		if(this.willFetch) {
		    if(THUMBCACHE.containsKey(youtubeId)) {
		        image = THUMBCACHE.get(youtubeId);
            } else {
                THUMBCACHE.put(youtubeId, image = new Image(thumbUrl));
            }
		} else {
			image = CommentResult.BLANK_PROFILE;
		}
	}

	public abstract String toString();

	public boolean willFetchThumb() {
		return willFetch;
	}

	public boolean hasThumb() {
		return image != null;
	}

	public String getId() {
		return youtubeId;
	}

	public String getTitle() {
		return title;
	}

	public String getThumbUrl() {
		return thumbUrl;
	}

	public Image fetchThumb() {
		return image;
	}

	public String getYoutubeLink() {
		switch(typeId){
		case 0:  return "https://youtu.be/"+youtubeId;
		case 1:  return "https://www.youtube.com/channel/"+youtubeId;
		case 2:  return "https://www.youtube.com/playlist?list="+youtubeId;
		default: return "https://www.youtube.com/error/"+youtubeId;
		}
	}

	public boolean equals(Object o) {
		return o instanceof YoutubeObject && ((YoutubeObject) o).getId().equals(getId());
	}
}