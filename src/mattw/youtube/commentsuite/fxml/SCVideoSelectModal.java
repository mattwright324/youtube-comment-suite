package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.db.CommentDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @author mattwright324
 */
public class SCVideoSelectModal extends VBox {

    private static Logger logger = LogManager.getLogger(SCVideoSelectModal.class.getSimpleName());

    private CommentDatabase database;

    private @FXML Button btnClose;
    private @FXML Button btnSubmit;

    public SCVideoSelectModal() {
        logger.debug("Initialize SCVideoSelectModal");

        database = FXMLSuite.getDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVRemoveAllModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            // TODO: Add remove functionality.
        } catch (IOException e) { logger.error(e); }
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

}
