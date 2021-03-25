package io.mattw.youtube.commentsuite.fxml;

import com.google.common.eventbus.Subscribe;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.*;
import io.mattw.youtube.commentsuite.events.GroupAddEvent;
import io.mattw.youtube.commentsuite.events.GroupDeleteEvent;
import io.mattw.youtube.commentsuite.events.GroupRenameEvent;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to add selected items (Videos, Channels, Playlists) from the Search YouTube section to an
 * already existing group or create an entirely new group to add the selection to.
 *
 * @see SearchYouTube
 */
public class SYAddToGroupModal extends VBox implements Cleanable {

    private static final Logger logger = LogManager.getLogger();

    @FXML private RadioButton addToExisting, addToNew;
    @FXML private ComboBox<Group> comboGroupSelect;
    @FXML private TextField groupName;
    @FXML private Label lblAbout, lblWarn;

    @FXML private Button btnClose;
    @FXML private Button btnSubmit;

    private final CommentDatabase database;

    private final ListView<SearchYouTubeListItem> listView;

    public SYAddToGroupModal(final ListView<SearchYouTubeListItem> listView) {
        this.listView = listView;

        logger.debug("Initialize SYAddToGroupModal");

        CommentSuite.getEventBus().register(this);

        database = CommentSuite.getDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SYAddToGroupModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            listView.getSelectionModel().getSelectedItems().addListener(
                    (ListChangeListener<SearchYouTubeListItem>) (lcl) ->
                            runLater(() -> lblAbout.setText(String.format("%s item(s) selected", lcl.getList().size())))
            );

            comboGroupSelect.disableProperty().bind(addToNew.selectedProperty());
            runLater(this::rebuildGroupSelect);

            groupName.disableProperty().bind(addToExisting.selectedProperty());

            btnSubmit.setOnAction(ae -> new Thread(() -> {
                runLater(() -> btnSubmit.setDisable(true));

                List<SearchYouTubeListItem> items = listView.getSelectionModel().getSelectedItems();

                if (addToExisting.isSelected()) {
                    Group group = comboGroupSelect.getSelectionModel().getSelectedItem();
                    if (group != null) {
                        submitItemsToGroup(items, group);
                    } else {
                        logger.warn("Selected existing group was null.");
                        runLater(() -> setError("Selected group is null."));
                    }
                } else if (addToNew.isSelected()) {
                    try {
                        Group group = database.groups().create(groupName.getText());

                        if (group != null) {
                            submitItemsToGroup(items, group);
                        } else {
                            logger.warn("Created group was null.");
                            runLater(() -> setError("Created group is null."));
                        }
                    } catch (SQLException e) {
                        logger.error("Failed to create new group [name={}]", groupName.getText());
                        runLater(() -> setError(e.getMessage()));
                    }
                }
            }).start());
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    private void submitItemsToGroup(final List<SearchYouTubeListItem> items, final Group group) {
        final GroupItemResolver resolver = new GroupItemResolver();
        final List<GroupItem> list = items.stream()
                .map(SearchYouTubeListItem::getData)
                .map(resolver::from)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        logger.debug("Group Items to add [list={}]", list.toString());

        if (!list.isEmpty()) {
            try {
                database.groupItems().insertAll(group, list);
                database.commit();

                logger.debug("GroupItems were successfully added to group");

                runLater(() -> btnClose.fire());
            } catch (SQLException e) {
                logger.error("Failed to insert group items to group [id={}]", group.getGroupId());

                runLater(() -> setError(e.getMessage()));
            }
        } else {
            String message = "Could not convert to GroupItems";

            logger.error(message);

            runLater(() -> setError(message));
        }

    }

    void setError(final String error) {
        lblWarn.setText(error);
        lblWarn.setVisible(true);
        lblWarn.setManaged(true);
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    @Override
    public void cleanUp() {
        groupName.setText("");

        lblWarn.setManaged(false);
        lblWarn.setVisible(false);

        setDisable(false);

        btnSubmit.setDisable(false);

        if (comboGroupSelect.getItems().isEmpty()) {
            addToNew.fire();
        } else {
            addToExisting.fire();
            comboGroupSelect.getSelectionModel().select(0);
        }
    }

    @Subscribe
    public void groupDeleteEvent(final GroupDeleteEvent deleteEvent) {
        logger.debug("Group Delete Event");
        runLater(this::rebuildGroupSelect);
    }

    @Subscribe
    public void groupAddEvent(final GroupAddEvent addEvent) {
        logger.debug("Group Add Event");
        runLater(this::rebuildGroupSelect);
    }

    @Subscribe
    public void groupRenameEvent(final GroupRenameEvent renameEvent) {
        logger.debug("Group Rename Event");
        runLater(this::rebuildGroupSelect);
    }

    private void rebuildGroupSelect() {
        final Group selectedGroup = comboGroupSelect.getValue();
        final ObservableList<Group> groups = FXCollections.observableArrayList(database.groups().getAllGroups());
        comboGroupSelect.setItems(FXCollections.emptyObservableList());
        comboGroupSelect.setItems(groups);

        if (selectedGroup == null || comboGroupSelect.getValue() == null) {
            comboGroupSelect.getSelectionModel().select(0);
        } else if (groups.contains(selectedGroup)) {
            comboGroupSelect.setValue(selectedGroup);
        }
    }

}
