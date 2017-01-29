package mattw.youtube.commensuitefx;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public abstract class YoutubeObject {
	
	public int type_id;
	final private String youtubeId;
	final private String title;
	final private String thumbUrl;
	
	final private boolean willFetch;
	final private Image image;
	final private static File thumbs = new File("Thumbs/");
	final private File thumbFile;
	
	protected YoutubeObject(String youtubeId, String title) {
		this(youtubeId, title, null, false);
	}
	
	protected YoutubeObject(String youtubeId, String title, String thumbUrl, boolean fetchThumb) {
		this(youtubeId, title, thumbUrl, fetchThumb, true);
	}
	
	protected YoutubeObject(String youtubeId, String title, String thumbUrl, boolean fetchThumb, boolean saveThumb) {
		this.youtubeId = youtubeId != null ? youtubeId : "";
		this.title = title;
		this.thumbUrl = thumbUrl;
		this.willFetch = fetchThumb;
		if(fetchThumb) {
			thumbs.mkdir();
			thumbFile = new File(thumbs, youtubeId+".jpg");
			if(thumbFile.exists()) {
				image = new Image(thumbFile.toURI().toString());
			} else {
				System.out.println("Fetching thumb: From URL: "+thumbUrl);
				image = new Image(thumbUrl);
				System.out.println("Fetching thumb: Success");
				if(saveThumb) trySaveImage();
			}
		} else {
			thumbFile = null;
			image = null;
		}
	}
	
	public void trySaveImage() {
		try {
			System.out.println("Downloading thumb for id ("+youtubeId+")");
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "jpg", thumbFile);
		} catch (IOException e) {}
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
		switch(type_id){
		case 0:
			return "https://youtu.be/"+youtubeId;
		case 1:
			return "https://www.youtube.com/channel/"+youtubeId;
		case 2:
			return "https://www.youtube.com/playlist?list="+youtubeId;
		default:
			return "https://www.youtube.com/error/"+youtubeId;
		}
	}
	
	public boolean equals(Object o) {
		if(o instanceof YoutubeObject) {
			return ((YoutubeObject) o).getId().equals(getId());
		}
		return false;
	}
}