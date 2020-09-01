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

            btnDelete.setOnAction(ae -> new Thread(() -> {
                runLater(() -> {
                    btnDelete.setDisable(true);
                    btnClose.setDisable(true);
                });

                try {
                    logger.warn("Deleting Group[id={},name={}]", group.getId(), group.getName());
                    database.deleteGroup(this.group);

                    logger.warn("Cleaning up after group delete [id={},name={}]", group.getId(), group.getName());
                    database.cleanUp();
                    database.commit();
                    if (doVacuum.isSelected()) {
                        database.vacuum();
                    }
                    database.refreshGroups();
                } catch (SQLException e) {
                    logger.error("Failed to delete group.", e);
                }
                runLater(() -> {
                    btnDelete.setDisable(false);
                    btnClose.setDisable(false);
                });
            }).start());
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
