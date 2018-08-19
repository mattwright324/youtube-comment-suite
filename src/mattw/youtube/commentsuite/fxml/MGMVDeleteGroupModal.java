package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.db.Group;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class MGMVDeleteGroupModal extends VBox {

    private static Logger logger = LogManager.getLogger(MGMVDeleteGroupModal.class.getName());

    @FXML Button btnClose;
    @FXML Button btnCreate;

    private Group group;

    public MGMVDeleteGroupModal(Group group) {
        this.group = group;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVDeleteGroupModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException e) { logger.error(e); }
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnCreate() {
        return btnCreate;
    }
}
