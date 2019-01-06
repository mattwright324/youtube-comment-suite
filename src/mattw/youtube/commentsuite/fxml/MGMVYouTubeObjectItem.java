package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.db.YouTubeChannel;
import mattw.youtube.commentsuite.db.YouTubeObject;
import mattw.youtube.commentsuite.db.YouTubeVideo;
import mattw.youtube.commentsuite.io.BrowserUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static javafx.application.Platform.runLater;

public class MGMVYouTubeObjectItem extends HBox implements ImageCache {

    private static Logger logger = LogManager.getLogger();

    private @FXML ImageView thumbnail;
    private @FXML Label title, subtitle;

    private YouTubeObject object;
    private Long value;
    private String subtitleSuffix;
    private boolean commentsDisabled = false;
    private boolean isVideo = true;

    private BrowserUtil browserUtil = new BrowserUtil();

    public MGMVYouTubeObjectItem(YouTubeVideo video, Long value, String subtitleSuffix) {
        this(video, value, subtitleSuffix, false);
    }

    public MGMVYouTubeObjectItem(YouTubeVideo video, Long value, String subtitleSuffix, boolean commentsDisabled) {
        this.object = video;
        this.value = value;
        this.subtitleSuffix = subtitleSuffix;
        this.commentsDisabled = commentsDisabled;

        initialize();
    }

    public MGMVYouTubeObjectItem(YouTubeChannel channel, Long value, String subtitleSuffix) {
        this.object = channel;
        this.value = value;
        this.subtitleSuffix = subtitleSuffix;
        this.isVideo = false;

        initialize();
    }

    private void initialize() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVYouTubeObjectItem.fxml"));
            loader.setController(this);
            loader.setRoot(this);
            loader.load();

            thumbnail.setImage(ImageLoader.LOADING.getImage());
            thumbnail.setCursor(Cursor.HAND);
            thumbnail.setOnMouseClicked(me -> browserUtil.open(object.getYouTubeLink()));

            if(isVideo) {
                thumbnail.setFitWidth(89);
            } else {
                thumbnail.setFitWidth(50);
            }

            new Thread(() -> {
                Image image = ImageCache.findOrGetImage(object);

                runLater(() -> thumbnail.setImage(image));
            }).start();

            title.setText(object.getTitle());

            subtitle.setText(commentsDisabled ?
                    String.format("%s", subtitleSuffix) :
                    String.format("%,d %s", value, subtitleSuffix));

            if(commentsDisabled) {
                subtitle.setStyle("-fx-text-fill:orangered");
            }
        } catch (IOException e) {
            logger.error("Failed to initialize MGMVYouTubeObjectListItem", e);
        }

    }

}
