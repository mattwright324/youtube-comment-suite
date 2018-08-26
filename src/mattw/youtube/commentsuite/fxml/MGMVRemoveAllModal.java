package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.datav3.YouTubeData3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static javafx.application.Platform.runLater;

import java.io.IOException;

public class MGMVRemoveAllModal extends VBox {

    private static Logger logger = LogManager.getLogger(MGMVRemoveAllModal.class.getName());

    private CommentDatabase database;
    private YouTubeData3 youtube;

    private @FXML Label alertError;
    private @FXML TextField link;
    private @FXML Button btnClose;
    private @FXML Button btnSubmit;

    private Group group;

    public MGMVRemoveAllModal(Group group) {
        this.group = group;

        database = FXMLSuite.getDatabase();
        youtube = FXMLSuite.getYoutubeApi();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVRemoveAllModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            // TODO: Add remove functionality.
        } catch (IOException e) { logger.error(e); }
    }

    public void reset() {

    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }
}
