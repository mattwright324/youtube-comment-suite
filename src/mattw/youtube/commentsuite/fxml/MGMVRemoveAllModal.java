package mattw.youtube.commentsuite.fxml;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.Cleanable;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.commentsuite.db.GroupItem;
import mattw.youtube.datav3.YouTubeData3;
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
 * @since 2018-12-30
 * @author mattwright324
 */
public class MGMVRemoveAllModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(MGMVRemoveAllModal.class.getSimpleName());

    private CommentDatabase database;
    private YouTubeData3 youtube;

    private @FXML Label alertError;
    private @FXML TextField link;
    private @FXML Button btnClose;
    private @FXML Button btnSubmit;

    private Group group;
    private ObservableList<MGMVGroupItemView> groupItems;

    private IntegerProperty itemsRemoved = new SimpleIntegerProperty(0);

    public MGMVRemoveAllModal(Group group, ObservableList<MGMVGroupItemView> groupItems) {
        logger.debug(String.format("Initialize for Group [id=%s,name=%s]", group.getId(), group.getName()));

        this.group = group;
        this.groupItems = groupItems;

        database = FXMLSuite.getDatabase();
        youtube = FXMLSuite.getYoutubeApi();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVRemoveAllModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            btnSubmit.setOnAction(ae -> new Thread(() -> {
                runLater(() -> btnSubmit.setDisable(true));

                if(groupItems.isEmpty()) {
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
                            btnClose.fire();
                        });
                    } catch (SQLException e) {
                        runLater(() -> setError(e.getLocalizedMessage()));
                        logger.error(e);
                    }
                }

            }).start());
        } catch (IOException e) { logger.error(e); }
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
