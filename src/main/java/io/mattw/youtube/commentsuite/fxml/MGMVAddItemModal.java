package io.mattw.youtube.commentsuite.fxml;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import io.mattw.youtube.commentsuite.Cleanable;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.db.GroupItem;
import io.mattw.youtube.datav3.YouTubeData3;
import io.mattw.youtube.datav3.entrypoints.YouTubeErrorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to add a GroupItem to the Group of the ManageGroupsManager with a YouTube link.
 * The YouTube link can be any of a video, channel, or playlist and should match the example formats to be accepted.
 *
 * @see GroupItem#GroupItem(String)
 * @see ManageGroupsManager
 * @since 2018-12-30
 * @author mattwright324
 */
public class MGMVAddItemModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(MGMVAddItemModal.class.getName());

    private CommentDatabase database;
    private YouTubeData3 youtube;

    private @FXML Label alertError;
    private @FXML TextField link;
    private @FXML Button btnClose, btnSubmit;

    private @FXML Label link1, link2, link3, link4, link5;

    private Group group;

    private IntegerProperty itemAdded = new SimpleIntegerProperty(0);

    public MGMVAddItemModal(Group group) {
        this.group = group;

        database = FXMLSuite.getDatabase();
        youtube = FXMLSuite.getYoutubeApi();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVAddItemModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            Label[] links = {link1, link2, link3, link4, link5};
            for(Label l : links) {
                l.setOnMouseClicked(me -> {
                    Object src = me.getSource();
                    if(src instanceof Label) {
                        Label label = (Label) src;
                        runLater(() -> link.setText(label.getText()));
                    }
                });
            }

            btnSubmit.setOnAction(ae -> new Thread(() -> {
                runLater(() -> btnSubmit.setDisable(true));

                try {
                    List<GroupItem> list = new ArrayList<>();
                    list.add(new GroupItem(link.getText()));

                    if(!list.isEmpty()) {
                        try {
                            database.insertGroupItems(this.group, list);
                            database.commit();
                            runLater(() -> {
                                itemAdded.setValue(itemAdded.getValue() + 1);
                                group.reloadGroupItems();
                                btnClose.fire();
                            });
                        } catch (SQLException e1) {
                            runLater(() -> setError(e1.getClass().getSimpleName()));
                        }
                    }
                } catch (IOException e) {
                    String message;
                    if(e instanceof YouTubeErrorException) {
                        message = ((YouTubeErrorException) e).getError().getMessage();
                    } else {
                        message = e.getMessage();
                    }
                    runLater(() -> setError(message));

                    logger.error("Failed to submit link", e);
                }

                runLater(() -> btnSubmit.setDisable(false));
            }).start());
        } catch (IOException e) { logger.error(e); }
    }

    public void setError(String message) {
        alertError.setText(message);
        alertError.setVisible(true);
        alertError.setManaged(true);
    }

    public IntegerProperty itemAddedProperty() {
        return itemAdded;
    }

    @Override
    public void cleanUp() {
        alertError.setVisible(false);
        alertError.setManaged(false);
        link.setText("");
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }
}
