package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class MGCreateGroupModal extends VBox {

    private static Logger logger = LogManager.getLogger(MGCreateGroupModal.class.getName());

    private @FXML Label errorMsg;
    private @FXML TextField nameField;
    private @FXML Button btnClose;
    private @FXML Button btnCreate;

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

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnCreate() {
        return btnCreate;
    }
}
