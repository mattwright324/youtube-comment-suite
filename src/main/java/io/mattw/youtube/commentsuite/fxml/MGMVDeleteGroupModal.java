package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.SQLException;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to delete the entire Group of its ManageGroupsManager. This deletes only the Group
 * object from the database and does not clean up its GroupItems, Videos, Comments, or Channels unless the
 * vacuum option is selected before deletion. Alternatively, the user could do a manual vacuum from {@link Settings}
 * if they did not select vacuum when deleting.
 *
 * @author mattwright324
 * @see ManageGroupsManager
 */
public class MGMVDeleteGroupModal extends VBox {

    private static final Logger logger = LogManager.getLogger();

    private CommentDatabase database;

    @FXML private CheckBox doVacuum;
    @FXML private Button btnClose;
    @FXML private Button btnDelete;

    private Group group;

    public MGMVDeleteGroupModal(Group group) {
        this.group = group;

        database = FXMLSuite.getDatabase();

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

    public CheckBox getDoVacuum() { return doVacuum; }
}
