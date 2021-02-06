package io.mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * This modal allows the user to delete the entire Group of its ManageGroupsManager. This deletes only the Group
 * object from the database and does not clean up its GroupItems, Videos, Comments, or Channels unless the
 * vacuum option is selected before deletion. Alternatively, the user could do a manual vacuum from {@link Settings}
 * if they did not select vacuum when deleting.
 *
 * @see ManageGroupsManager
 */
public class MGMVDeleteGroupModal extends VBox {

    private static final Logger logger = LogManager.getLogger();

    @FXML private Button btnClose;
    @FXML private Button btnDelete;

    public MGMVDeleteGroupModal() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVDeleteGroupModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnDelete() {
        return btnDelete;
    }

}
