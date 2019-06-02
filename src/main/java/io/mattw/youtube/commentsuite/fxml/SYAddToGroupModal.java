package io.mattw.youtube.commentsuite.fxml;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import io.mattw.youtube.commentsuite.Cleanable;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.db.GroupItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to add selected items (Videos, Channels, Playlists) from the Search YouTube section to an
 * already existing group or create an entirely new group to add the selection to.
 *
 * @see SearchYouTube
 * @since 2018-12-30
 * @author mattwright324
 */
public class SYAddToGroupModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(SYAddToGroupModal.class.getSimpleName());

    private @FXML RadioButton addToExisting, addToNew;
    private @FXML ComboBox<Group> groupsList;
    private @FXML TextField groupName;
    private @FXML Label lblAbout, lblWarn;

    private @FXML Button btnClose;
    private @FXML Button btnSubmit;

    private CommentDatabase database;

    private ListView<SearchYouTubeListItem> listView;

    public SYAddToGroupModal(ListView<SearchYouTubeListItem> listView) {
        this.listView = listView;

        logger.debug("Initialize SYAddToGroupModal");

        database = FXMLSuite.getDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SYAddToGroupModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            listView.getSelectionModel().getSelectedItems().addListener(
                    (ListChangeListener<SearchYouTubeListItem>)(lcl) ->
                runLater(() -> lblAbout.setText(String.format("%s item(s) selected", lcl.getList().size())))
            );

            groupsList.disableProperty().bind(addToNew.selectedProperty());
            groupsList.setItems(database.globalGroupList);
            groupsList.getItems().addListener((ListChangeListener<Group>)(c -> {
                SelectionModel<Group> selectionModel = groupsList.getSelectionModel();
                if(!groupsList.getItems().isEmpty() && selectionModel.getSelectedIndex() == -1) {
                    runLater(() -> selectionModel.select(0));
                }
            }));

            groupName.disableProperty().bind(addToExisting.selectedProperty());

            btnSubmit.setOnAction(ae -> new Thread(() -> {
                runLater(() -> btnSubmit.setDisable(true));

                List<SearchYouTubeListItem> items = listView.getSelectionModel().getSelectedItems();

                if(addToExisting.isSelected()) {
                    Group group = groupsList.getSelectionModel().getSelectedItem();
                    if(group != null) {
                        submitItemsToGroup(items, group);
                    } else {
                        logger.warn("Selected existing group was null.");
                        runLater(() -> setError("Selected group is null."));
                    }
                } else if(addToNew.isSelected()) {
                    try {
                        Group group = database.createGroup(groupName.getText());

                        if(group != null) {
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

    private void submitItemsToGroup(List<SearchYouTubeListItem> items, Group group) {
        List<GroupItem> list = items.stream()
                .map(SearchYouTubeListItem::getYoutubeURL)
                .map(link -> { try {
                    return new GroupItem(link);
                } catch (IOException e) {
                    logger.error("Failed to parse to GroupItem", e);

                    return null;
                }})
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        logger.debug("Group Items to add [list={}]", list.toString());

        if(!list.isEmpty()) {
            try {
                database.insertGroupItems(group, list);
                database.commit();

                logger.debug("GroupItems were successfully added to group");

                runLater(() -> {
                    group.reloadGroupItems();
                    btnClose.fire();
                });
            } catch (SQLException e) {
                logger.error("Failed to insert group items to group [id={}]", group.getId());

                runLater(() -> setError(e.getMessage()));
            }
        } else {
            String message = "Could not convert to GroupItems";

            logger.error(message);

            runLater(() -> setError(message));
        }

    }

    void setError(String error) {
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

        if(groupsList.getItems().isEmpty()) {
            addToNew.fire();
        } else {
            addToExisting.fire();
            groupsList.getSelectionModel().select(0);
        }
    }
}
