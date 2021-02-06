package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.db.GroupItem;
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
 * @see ManageGroupsManager
 */
public class MGMVRemoveAllModal extends VBox implements Cleanable {

    private static final Logger logger = LogManager.getLogger();

    private CommentDatabase database;

    @FXML private Label alertError;
    @FXML private TextField link;
    @FXML private Button btnClose;
    @FXML private Button btnSubmit;

    private final Group group;
    private final ObservableList<MGMVGroupItemView> groupItems;

    public MGMVRemoveAllModal(final Group group, final ObservableList<MGMVGroupItemView> groupItems) {
        logger.debug("Initialize for Group [id={},name={}]", group.getGroupId(), group.getName());

        this.group = group;
        this.groupItems = groupItems;

        database = CommentSuite.getDatabase();

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

                        database.groupItems().deleteAssociations(group, items);
                        database.cleanUp();
                        runLater(() -> btnClose.fire());
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
