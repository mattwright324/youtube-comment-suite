package mattw.youtube.commentsuite.fxml;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import static javafx.application.Platform.runLater;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.Group;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Manages group selection, creation, and content switching.
 *
 * @author mattwright324
 */
public class ManageGroupsController implements Initializable {

    private static Logger logger = LogManager.getLogger(ManageGroupsController.class.getName());
    private static Image plus = new Image("/mattw/youtube/commentsuite/img/plus.png");

    Cache<String,ManageGroupsManagerView> managerCache = CacheBuilder.newBuilder().build();

    private CommentDatabase database;

    private @FXML OverlayModal<MGCreateGroupModal> overlayModal;

    private @FXML ImageView plusIcon;
    private @FXML ComboBox<Group> comboGroupSelect;
    private @FXML Button btnCreateGroup;
    private @FXML Pane content;

    public void initialize(URL location, ResourceBundle resources) {
        database = FXMLSuite.getDatabase();

        /**
         * Logic for main pane.
         */

        plusIcon.setImage(plus);

        SelectionModel<Group> selectionModel = comboGroupSelect.getSelectionModel();
        comboGroupSelect.setItems(database.globalGroupList);
        new Thread(() -> {
            try {
                database.refreshGroups();
            } catch (SQLException e) {
                logger.error(e);
            }
        }).start();
        comboGroupSelect.getItems().addListener((ListChangeListener<Group>)(c -> {
            if(!comboGroupSelect.getItems().isEmpty() && selectionModel.getSelectedIndex() == -1) {
                selectionModel.select(0);
            }
        }));
        selectionModel.selectedItemProperty().addListener((o, ov, nv) -> {
            ManageGroupsManagerView manager = managerCache.getIfPresent(nv.getId());
            if (manager != null) {
                runLater(() -> {
                    content.getChildren().clear();
                    content.getChildren().addAll(manager);
                });
            } else {
                try {
                    ManageGroupsManagerView m = new ManageGroupsManagerView(selectionModel.getSelectedItem());
                    managerCache.put(nv.getId(), m);
                    runLater(() -> {
                        content.getChildren().clear();
                        content.getChildren().addAll(m);
                    });
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        });

        /**
         * Logic for Create Group popup.
         */

        MGCreateGroupModal modal = new MGCreateGroupModal();
        overlayModal.setContent(modal);

        btnCreateGroup.setOnAction(ae -> runLater(() -> {
            modal.getErrorMsg().setManaged(false);
            modal.getNameField().setText("");
            overlayModal.setVisible(true);
        }));

        modal.getBtnClose().setOnAction(ae -> runLater(() -> {
            overlayModal.setVisible(false);
        }));

        modal.getBtnCreate().setOnAction(ae -> new Thread(() -> {
            logger.debug("Attempting to create group");
            runLater(() -> overlayModal.setDisable(true));
            String name = modal.getNameField().getText();
            if(!name.equals("")) {
                try {
                    Group g = database.createGroup(name);
                    logger.debug(String.format("Created new group [id=%s,name=%s]", g.getId(), g.getName()));
                    runLater(() -> {
                        overlayModal.setDisable(false);
                        modal.getErrorMsg().setManaged(false);
                        overlayModal.setVisible(false);
                    });
                } catch (SQLException e) {
                    logger.error(e);
                    runLater(() -> {
                        overlayModal.setDisable(false);
                        modal.getErrorMsg().setManaged(true);
                        modal.getErrorMsg().setText("Name already exists, try another!");
                    });
                }
            } else {
                runLater(() -> {
                    overlayModal.setDisable(true);
                    modal.getErrorMsg().setManaged(true);
                    modal.getErrorMsg().setText("Name must not be empty.");
                });
            }
        }).start());

    }
}
