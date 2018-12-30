package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.Cleanable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class MGCreateGroupModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(MGCreateGroupModal.class.getName());

    private @FXML Label errorMsg;
    private @FXML TextField nameField;
    private @FXML Button btnSubmit, btnClose;

    public MGCreateGroupModal() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGCreateGroupModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException e) { logger.error(e); }
    }

    public Label getErrorMsg() {
        return errorMsg;
    }

    public TextField getNameField() {
        return nameField;
    }

    @Override
    public void cleanUp() {
        getErrorMsg().setManaged(false);
        getNameField().setText("");
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }
}
