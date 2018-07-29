package mattw.youtube.commentsuite.fxml;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionModel;
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

    @FXML ImageView plusIcon;
    @FXML ComboBox<Group> comboGroupSelect;
    @FXML Button btnCreateGroup;
    @FXML Pane content;

    public void initialize(URL location, ResourceBundle resources) {
        database = FXMLSuite.getDatabase();

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
                ManageGroupsManagerView m = manager;
                Platform.runLater(() -> {
                    content.getChildren().clear();
                    content.getChildren().addAll(m);
                });
            } else {
                try {
                    ManageGroupsManagerView m = new ManageGroupsManagerView(selectionModel.getSelectedItem());
                    managerCache.put(nv.getId(), m);
                    Platform.runLater(() -> {
                        content.getChildren().clear();
                        content.getChildren().addAll(m);
                    });
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        });
    }
}
