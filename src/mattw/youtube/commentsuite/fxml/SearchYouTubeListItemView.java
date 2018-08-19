package mattw.youtube.commentsuite.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.YouTubeSearchItem;
import mattw.youtube.commentsuite.db.YouTubeObject;
import mattw.youtube.datav3.resources.SearchList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Loads template FXML and displays info from SearchList.Item.
 *
 * Searching occurs in the SearchYouTubeController.
 *
 * @author mattwright324
 */
public class SearchYouTubeListItemView extends HBox {

    private static Logger logger = LogManager.getLogger(SearchYouTubeListItemView.class.getName());
    private static Image loading = new Image("/mattw/youtube/commentsuite/img/loading.png");

    @FXML Label type;
    @FXML Label title;
    @FXML Label author;
    @FXML Label description;
    @FXML Label number;
    @FXML ImageView thumbnail;

    private SearchList.Item data;
    private String objectId;
    private String youtubeURL;
    private String typeStr;

    public SearchYouTubeListItemView(SearchList.Item data, int num) throws IOException {
        this.data = data;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SearchYouTubeListItem.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        number.setText(String.format("# %s", num));
        thumbnail.setFitWidth(thumbnail.getFitHeight() * loading.getWidth() / loading.getHeight());
        thumbnail.setImage(loading);

        if(data.id.videoId != null) {
            objectId = data.id.videoId;
            youtubeURL = String.format("https://youtu.be/%s", objectId);
            typeStr = "Video";
        } else if(data.id.channelId != null) {
            objectId = data.id.channelId;
            youtubeURL = String.format("https://youtube.com/channel/%s", objectId);
            typeStr = "Channel";
        } else if(data.id.playlistId != null) {
            objectId = data.id.playlistId;
            youtubeURL = String.format("https://youtube.com/playlist?list=%s", objectId);
            typeStr = "Playlist";
        } else {
            logger.error("A SearchList.Item object has been passed with no id.");
            typeStr = "Error";
        }

        type.setText(typeStr);

        if(data.snippet != null) {
            title.setText(data.snippet.title);
            author.setText(data.snippet.channelTitle);
            description.setText(data.snippet.description);

            new Thread(() -> {
                YouTubeSearchItem obj = new YouTubeSearchItem(data);
                Image thumb = ImageCache.findOrGetImage(obj);
                thumbnail.setFitWidth(thumbnail.getFitHeight() * thumb.getWidth() / thumb.getHeight());
                thumbnail.setImage(thumb);
            }).start();
        } else {
            title.setText("SearchList.Item Error");
            author.setText(data.etag);
            description.setText("There was no snippet attached to this object.");
            Image thumb = new Image("/mattw/youtube/commentsuite/img/oops.png");
            Platform.runLater(() -> {
                thumbnail.setFitWidth(thumbnail.getFitHeight() * thumb.getWidth() / thumb.getHeight());
                thumbnail.setImage(thumb);
            });
        }
    }

    public String getYoutubeURL() { return this.youtubeURL; }

    public String getObjectId() { return this.objectId; }

    public SearchList.Item getData() { return this.data; }

}
