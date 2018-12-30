package mattw.youtube.commentsuite.fxml;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.Cleanable;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.Group;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static javafx.application.Platform.runLater;

/**
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
                runLater(() -> this.setDisable(true));

                if(addToExisting.isSelected()) {
                    Group group = groupsList.getSelectionModel().getSelectedItem();
                    if(group != null) {

                    } else {
                        logger.debug("Selected existing group was null.");
                        runLater(() -> setError("Selected group is null."));
                    }
                } else if(addToNew.isSelected()) {

                }
            }));
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
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

        if(groupsList.getItems().isEmpty()) {
            addToNew.fire();
        } else {
            addToExisting.fire();
            groupsList.getSelectionModel().select(0);
        }
    }
}
