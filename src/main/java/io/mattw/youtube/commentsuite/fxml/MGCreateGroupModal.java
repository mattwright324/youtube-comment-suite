package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
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

import static javafx.application.Platform.runLater;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * This modal allows the user to create a new and empty Group with the name of their choosing. The name must be unique.
 *
 * @see ManageGroups
 */
public class MGCreateGroupModal extends VBox implements Cleanable {

    private static final Logger logger = LogManager.getLogger();

    @FXML private Label errorMsg;
    @FXML private TextField nameField;
    @FXML private Button btnSubmit, btnClose;

    private CommentDatabase database;

    public MGCreateGroupModal() {
        logger.debug("Initialize MGCreateGroupModal");

        database = CommentSuite.getDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGCreateGroupModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            errorMsg.managedProperty().bind(errorMsg.visibleProperty());

            nameField.textProperty().addListener((ov, prevText, currText) ->
                    runLater(() -> btnSubmit.setDisable(isBlank(nameField.getText()))));

            btnSubmit.setOnAction(ae -> tryCreate());
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void tryCreate() {
        final String input = trimToEmpty(nameField.getText());

        runLater(() -> nameField.setText(input));

        if (isBlank(input)) {
            runLater(() -> {
                errorMsg.setVisible(true);
                errorMsg.setText("Name cannot be blank");
            });
            return;
        }

        try {
            final Group newGroup = database.groups().create(input);

            logger.debug("Created new group [id={},name={}]", newGroup.getGroupId(), newGroup.getName());

            runLater(() -> {
                errorMsg.setText("");
                errorMsg.setVisible(false);
                btnClose.fire();
            });
        } catch (SQLException e) {
            logger.error(e);

            runLater(() -> {
                errorMsg.setVisible(true);
                errorMsg.setText("Name already exists, try another!");
            });
        }
    }

    public TextField getNameField() {
        return nameField;
    }

    @Override
    public void cleanUp() {
        errorMsg.setVisible(false);
        nameField.setText("");
        btnSubmit.setDisable(true);
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }
}
