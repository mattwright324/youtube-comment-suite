package io.mattw.youtube.commentsuite.fxml;

import com.google.api.services.youtube.YouTube;
import io.mattw.youtube.commentsuite.Cleanable;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.db.GroupItem;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to remove all GroupItems from the Group of its ManageGroupManager.
 *
 * @author mattwright324
 * @see ManageGroupsManager
 */
public class MGMVRemoveAllModal extends VBox implements Cleanable {

    private static final Logger logger = LogManager.getLogger();

    private CommentDatabase database;
    private YouTube youtube;

    @FXML private Label alertError;
    @FXML private TextField link;
    @FXML private Button btnClose;
    @FXML private Button btnSubmit;

    private Group group;
    private ObservableList<MGMVGroupItemView> groupItems;

    private IntegerProperty itemsRemoved = new SimpleIntegerProperty(0);

    public MGMVRemoveAllModal(Group group, ObservableList<MGMVGroupItemView> groupItems) {
        logger.debug("Initialize for Group [id={},name={}]", group.getId(), group.getName());

        this.group = group;
        this.groupItems = groupItems;

        database = FXMLSuite.getDatabase();
        youtube = FXMLSuite.getYouTube();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVRemoveAllModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            btnSubmit.setOnAction(ae -> new Thread(() -> {
                runLater(() -> btnSubmit.setDisable(true));

                if (groupItems.isEmpty()) {
                    runLater(() -> setError("There are no items in the list."));
                } else {
                    try {
                        List<GroupItem> items = groupItems.stream()
                                .map(MGMVGroupItemView::getGroupItem)
                                .collect(Collectors.toList());

                        database.deleteGroupItemLinks(group, items);
                        database.cleanUp();
                        runLater(() -> {
                            itemsRemoved.setValue(itemsRemoved.getValue() + 1);
                            group.reloadGroupItems();
                            btnClose.fire();
                        });
                    } catch (SQLException e) {
                        runLater(() -> setError(e.getLocalizedMessage()));
                        logger.error(e);
                    }
                }

            }).start());
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public void setError(String message) {
        alertError.setText(message);
        alertError.setVisible(true);
        alertError.setManaged(true);
    }

    public IntegerProperty itemsRemovedProperty() {
        return itemsRemoved;
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    @Override
    public void cleanUp() {
        btnSubmit.setDisable(false);
    }
}
