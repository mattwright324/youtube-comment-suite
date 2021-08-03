package io.mattw.youtube.commentsuite.fxml;

import com.google.api.services.youtube.model.SearchResult;
import io.mattw.youtube.commentsuite.ImageLoader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static javafx.application.Platform.runLater;

/**
 * Loads template FXML and displays info from SearchList.Item.
 * <p>
 * Searching occurs in the SearchYouTube.
 *
 */
public class SearchYouTubeListItem extends HBox {

    private static final Logger logger = LogManager.getLogger();

    private static Image loading = ImageLoader.LOADING.getImage();

    @FXML private Label type;
    @FXML private Label title;
    @FXML private Label author;
    @FXML private Label description;
    @FXML private Label number;
    @FXML private ImageView thumbnail;

    private final SearchResult data;
    private String objectId;
    private String youtubeURL;
    private String typeStr;

    public SearchYouTubeListItem(final SearchResult data, final int num) throws IOException {
        this.data = data;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SearchYouTubeListItem.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        runLater(() -> {
            number.setText(String.format("# %s", num));
            thumbnail.setFitWidth(thumbnail.getFitHeight() * loading.getWidth() / loading.getHeight());
            thumbnail.setImage(loading);

            if ((objectId = data.getId().getVideoId()) != null) {
                youtubeURL = String.format("https://youtu.be/%s", objectId);
                typeStr = "Video";
            } else if ((objectId = data.getId().getChannelId()) != null) {
                youtubeURL = String.format("https://www.youtube.com/channel/%s", objectId);
                typeStr = "Channel";
            } else if ((objectId = data.getId().getPlaylistId()) != null) {
                youtubeURL = String.format("https://www.youtube.com/playlist?list=%s", objectId);
                typeStr = "Playlist";
            } else {
                logger.error("A SearchList.Item object has been passed with no id.");
                typeStr = "Error";
            }

            type.setText(typeStr);
        });

        if (data.getSnippet() != null) {
            runLater(() -> {
                title.setText(data.getSnippet().getTitle());
                author.setText(data.getSnippet().getChannelTitle());
                description.setText(data.getSnippet().getDescription());
            });

            new Thread(() -> {
                final YouTubeSearchItem searchItem = new YouTubeSearchItem(data);
                final Image thumb = searchItem.findOrGetThumb();
                runLater(() -> {
                    thumbnail.setFitWidth(thumbnail.getFitHeight() * thumb.getWidth() / thumb.getHeight());
                    thumbnail.setImage(thumb);
                });
            }).start();
        } else {
            final Image thumb = ImageLoader.OOPS.getImage();
            runLater(() -> {
                title.setText("SearchList.Item Error");
                author.setText(data.getEtag());
                description.setText("There was no snippet attached to this object.");
                thumbnail.setFitWidth(thumbnail.getFitHeight() * thumb.getWidth() / thumb.getHeight());
                thumbnail.setImage(thumb);
            });
        }
    }

    public String getYoutubeURL() {
        return this.youtubeURL;
    }

    public String getObjectId() {
        return this.objectId;
    }

    public SearchResult getData() {
        return this.data;
    }

}
