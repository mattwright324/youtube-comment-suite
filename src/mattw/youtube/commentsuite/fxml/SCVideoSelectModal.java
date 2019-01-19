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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * This modal allows the user to select a specific video to list comments from that are within the currently
 * selected Group and GroupItem prior to opening the modal.
 *
 * @see SearchComments
 * @since 2018-12-30
 * @author mattwright324
 */
public class SCVideoSelectModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(SCVideoSelectModal.class.getSimpleName());

    private CommentDatabase database;

    private @FXML Label lblSelection;
    private @FXML ListView videoList;
    private @FXML ImageView btnReset;
    private @FXML Button btnClose, btnSubmit;

    private Group group;
    private GroupItem groupItem;

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



            // TODO: Add video selection funcationality.
        } catch (IOException e) { logger.error("An error occurred loading the SCVideoSelectModal", e); }
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    public void loadWith(Group group, GroupItem groupItem) {

    }

    @Override
    public void cleanUp() {

    }
}
