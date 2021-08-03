package io.mattw.youtube.commentsuite.fxml;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.Subscribe;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.events.GroupAddEvent;
import io.mattw.youtube.commentsuite.events.GroupDeleteEvent;
import io.mattw.youtube.commentsuite.events.GroupRenameEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionModel;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static javafx.application.Platform.runLater;

/**
 * Manages group selection, creation, and content switching.
 */
public class ManageGroups implements Initializable {

    private static final Logger logger = LogManager.getLogger();

    private static final Cache<String, ManageGroupsManager> managerCache = CacheBuilder.newBuilder().build();

    @FXML
    private OverlayModal<MGCreateGroupModal> overlayModal;

    @FXML
    private ImageView plusIcon;
    @FXML
    private ComboBox<Group> comboGroupSelect;
    @FXML
    private Button btnCreateGroup;
    @FXML
    private Pane content;

    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialize ManageGroups");

        CommentSuite.getEventBus().register(this);

        /*
         * Logic for main pane.
         */

        plusIcon.setImage(ImageLoader.PLUS.getImage());

        runLater(this::rebuildGroupSelect);

        SelectionModel<Group> selectionModel = comboGroupSelect.getSelectionModel();
        selectionModel.selectedItemProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                ManageGroupsManager manager = managerCache.getIfPresent(nv.getGroupId());
                if (manager != null) {
                    runLater(() -> {
                        content.getChildren().clear();
                        content.getChildren().addAll(manager);
                    });
                } else {
                    try {
                        ManageGroupsManager m = new ManageGroupsManager(selectionModel.getSelectedItem());
                        managerCache.put(nv.getGroupId(), m);
                        runLater(() -> {
                            content.getChildren().clear();
                            content.getChildren().addAll(m);
                        });
                    } catch (IOException e) {
                        logger.error(e);
                    }
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
        overlayModal.visibleProperty().addListener((cl) -> {
            modal.getBtnClose().setCancelButton(overlayModal.isVisible());
            modal.getBtnSubmit().setDefaultButton(overlayModal.isVisible());
        });
        modal.getBtnClose().setOnAction(ae -> runLater(() ->
                overlayModal.setVisible(false))
        );
    }

    public static Cache<String, ManageGroupsManager> getManagerCache() {
        return managerCache;
    }

    private ComboBox<Group> getComboGroupSelect() {
        return comboGroupSelect;
    }

    @Subscribe
    public void groupDeleteEvent(final GroupDeleteEvent deleteEvent) {
        logger.debug("Group Delete Event");
        for (Group group : deleteEvent.getGroups()) {
            managerCache.invalidate(group.getGroupId());
        }
        runLater(this::rebuildGroupSelect);
    }

    @Subscribe
    public void groupAddEvent(final GroupAddEvent addEvent) {
        logger.debug("Group Add Event");
        runLater(this::rebuildGroupSelect);
        addEvent.getGroups()
                .stream()
                .findFirst()
                .ifPresent(group -> runLater(() -> comboGroupSelect.setValue(group)));
    }

    @Subscribe
    public void groupRenameEvent(final GroupRenameEvent renameEvent) {
        logger.debug("Group Rename Event");
        runLater(this::rebuildGroupSelect);
    }

    private void rebuildGroupSelect() {
        final Group selectedGroup = comboGroupSelect.getValue();
        final ObservableList<Group> groups = FXCollections.observableArrayList(CommentSuite.getDatabase().groups().getAllGroups());
        comboGroupSelect.setItems(FXCollections.emptyObservableList());
        comboGroupSelect.setItems(groups);

        if (selectedGroup == null || comboGroupSelect.getValue() == null) {
            comboGroupSelect.getSelectionModel().select(0);
        } else if (groups.contains(selectedGroup)) {
            comboGroupSelect.setValue(selectedGroup);
        }
    }

}
