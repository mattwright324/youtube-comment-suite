package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.YouTubeSearchItem;
import mattw.youtube.datav3.entrypoints.SearchList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Loads template FXML and displays info from SearchList.Item.
 *
 * Searching occurs in the SearchYouTube.
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public class SearchYouTubeListItem extends HBox {

    private static Logger logger = LogManager.getLogger(SearchYouTubeListItem.class.getName());
    private static Image loading = ImageLoader.LOADING.getImage();

    private @FXML Label type;
    private @FXML Label title;
    private @FXML Label author;
    private @FXML Label description;
    private @FXML Label number;
    private @FXML ImageView thumbnail;

    private SearchList.Item data;
    private String objectId;
    private String youtubeURL;
    private String typeStr;

    public SearchYouTubeListItem(SearchList.Item data, int num) throws IOException {
        this.data = data;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SearchYouTubeListItem.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        number.setText(String.format("# %s", num));
        thumbnail.setFitWidth(thumbnail.getFitHeight() * loading.getWidth() / loading.getHeight());
        thumbnail.setImage(loading);

        if((objectId = data.getId().getVideoId()) != null) {
            youtubeURL = String.format("https://youtu.be/%s", objectId);
            typeStr = "Video";
        } else if((objectId = data.getId().getChannelId()) != null) {
            youtubeURL = String.format("https://youtube.com/channel/%s", objectId);
            typeStr = "Channel";
        } else if((objectId = data.getId().getPlaylistId()) != null) {
            youtubeURL = String.format("https://youtube.com/playlist?list=%s", objectId);
            typeStr = "Playlist";
        } else {
            logger.error("A SearchList.Item object has been passed with no id.");
            typeStr = "Error";
        }

        type.setText(typeStr);

        if(data.hasSnippet()) {
            title.setText(data.getSnippet().getTitle());
            author.setText(data.getSnippet().getChannelTitle());
            description.setText(data.getSnippet().getDescription());

            new Thread(() -> {
                YouTubeSearchItem obj = new YouTubeSearchItem(data);
                Image thumb = ImageCache.findOrGetImage(obj);
                thumbnail.setFitWidth(thumbnail.getFitHeight() * thumb.getWidth() / thumb.getHeight());
                thumbnail.setImage(thumb);
            }).start();
        } else {
            title.setText("SearchList.Item Error");
            author.setText(data.getEtag());
            description.setText("There was no snippet attached to this object.");
            Image thumb = ImageLoader.OOPS.getImage();
            runLater(() -> {
                thumbnail.setFitWidth(thumbnail.getFitHeight() * thumb.getWidth() / thumb.getHeight());
                thumbnail.setImage(thumb);
            });
        }
    }

    public String getYoutubeURL() { return this.youtubeURL; }

    public String getObjectId() { return this.objectId; }

    public SearchList.Item getData() { return this.data; }

}
