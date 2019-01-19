package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.YouTubeSearchItem;
import mattw.youtube.commentsuite.db.YouTubeVideo;
import mattw.youtube.datav3.entrypoints.SearchList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static javafx.application.Platform.runLater;

/**
 * @since 2019-01-19
 * @author mattwright324
 */
public class SCVideoSelectModalListItem extends HBox {

    private static Logger logger = LogManager.getLogger(SCVideoSelectModalListItem.class.getName());
    private static Image loading = ImageLoader.LOADING.getImage();

    private @FXML Label type;
    private @FXML Label title;
    private @FXML Label author;
    private @FXML Label description;
    private @FXML Label number;
    private @FXML ImageView thumbnail;

    private YouTubeVideo video;

    public SCVideoSelectModalListItem(YouTubeVideo video) throws IOException {
        this.video = video;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCVideoSelectModalListItem.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();


    }

    public YouTubeVideo getVideo() {
        return video;
    }
}
