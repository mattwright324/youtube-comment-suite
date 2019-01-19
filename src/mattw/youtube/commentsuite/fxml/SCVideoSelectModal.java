package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.Cleanable;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.commentsuite.db.GroupItem;
import mattw.youtube.commentsuite.db.YouTubeVideo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This modal allows the user to select a specific video for comment searching that are within the currently
 * selected Group and GroupItem prior to opening the modal.
 *
 * @see SearchComments
 * @since 2018-12-30
 * @author mattwright324
 */
public class SCVideoSelectModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(SCVideoSelectModal.class.getSimpleName());

    private CommentDatabase database;

    private @FXML Label lblSelection, errorMsg;
    private @FXML ListView<MGMVYouTubeObjectItem> videoList;
    private @FXML ImageView btnReset;
    private @FXML Button btnClose, btnSubmit;

    private Group group;
    private GroupItem groupItem;
    private YouTubeVideo selectedVideo;

    public SCVideoSelectModal() {
        logger.debug("Initialize SCVideoSelectModal");

        database = FXMLSuite.getDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCVideoSelectModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            btnReset.setImage(ImageLoader.CLOSE.getImage());
            btnReset.setDisable(true);

            videoList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                //selectedVideo = nv;
            });

            // TODO: Add video selection funcationality.
        } catch (IOException e) { logger.error("An error occurred loading the SCVideoSelectModal", e); }
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    YouTubeVideo getSelectedVideo() {
        return selectedVideo;
    }

    void loadWith(Group group, GroupItem groupItem) {
        if(this.group != group || this.groupItem != groupItem) {
            this.group = group;
            this.groupItem = groupItem;
            this.selectedVideo = null;
            videoList.getSelectionModel().clearSelection();
        }

        updateSelectionLabel();
    }

    void updateSelectionLabel() {
        btnReset.setDisable(selectedVideo == null);
        lblSelection.setText(String.format("%s > %s > %s",
                group.getName(),
                groupItem.getTitle(),
                selectedVideo != null ? selectedVideo.getTitle() : "All Videos"));
    }

    void updateVideoList() throws SQLException {
        List<YouTubeVideo> videos;
        List<MGMVYouTubeObjectItem> itemList = new ArrayList<>();

        if(groupItem.getYoutubeId().equals(GroupItem.ALL_ITEMS)) {
            videos = database.getVideos(groupItem, "", "", 50);
        }
    }

    @Override
    public void cleanUp() {
        updateSelectionLabel();
    }
}
