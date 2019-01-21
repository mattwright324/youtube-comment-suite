package mattw.youtube.commentsuite.fxml;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionModel;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.Group;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

import static javafx.application.Platform.runLater;

/**
 * Manages group selection, creation, and content switching.
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public class ManageGroups implements Initializable {

    private static Logger logger = LogManager.getLogger(ManageGroups.class.getSimpleName());
    Cache<String, ManageGroupsManager> managerCache = CacheBuilder.newBuilder().build();

    private CommentDatabase database;

    private @FXML OverlayModal<MGCreateGroupModal> overlayModal;

    private @FXML ImageView plusIcon;
    private @FXML ComboBox<Group> comboGroupSelect;
    private @FXML Button btnCreateGroup;
    private @FXML Pane content;

    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialize ManageGroups");

        database = FXMLSuite.getDatabase();

        /*
         * Logic for main pane.
         */

        plusIcon.setImage(ImageLoader.PLUS.getImage());

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
            ManageGroupsManager manager = managerCache.getIfPresent(nv.getId());
            if (manager != null) {
                runLater(() -> {
                    content.getChildren().clear();
                    content.getChildren().addAll(manager);
                });
            } else {
                try {
                    ManageGroupsManager m = new ManageGroupsManager(selectionModel.getSelectedItem());
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

        /*
         * Logic for Create Group popup.
         */

        MGCreateGroupModal modal = new MGCreateGroupModal();
        overlayModal.setContent(modal);
        btnCreateGroup.setOnAction(ae -> runLater(() -> {
            modal.cleanUp();
            overlayModal.setVisible(true);
        }));
        modal.getBtnClose().setOnAction(ae -> runLater(() ->
                overlayModal.setVisible(false))
        );
        modal.getBtnSubmit().setOnAction(ae -> new Thread(() -> {
            logger.debug("Attempting to create group");
            runLater(() -> overlayModal.setDisable(true));
            String name = modal.getNameField().getText();
            if(!name.isEmpty()) {
                try {
                    Group g = database.createGroup(name);
                    logger.debug(String.format("Created new group [id=%s,name=%s]", g.getId(), g.getName()));
                    runLater(() -> {
                        comboGroupSelect.getSelectionModel().select(g);
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
