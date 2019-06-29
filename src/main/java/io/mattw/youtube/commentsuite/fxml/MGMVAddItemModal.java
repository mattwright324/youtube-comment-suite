package io.mattw.youtube.commentsuite.fxml;

import com.google.api.services.youtube.YouTube;
import io.mattw.youtube.commentsuite.Cleanable;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.db.GroupItem;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to add a GroupItem to the Group of the ManageGroupsManager with a YouTube link.
 * The YouTube link can be any of a video, channel, or playlist and should match the example formats to be accepted.
 *
 * @see GroupItem#GroupItem(String)
 * @see ManageGroupsManager
 * @author mattwright324
 */
public class MGMVAddItemModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(MGMVAddItemModal.class.getName());

    private CommentDatabase database;
    private YouTube youtube;

    private @FXML Label alertError;

    private @FXML TabPane tabPane;
    private @FXML Tab tabSingular, tabBulk;

    private @FXML TextField link;
    private @FXML Label link1, link2, link3, link4, link5;
    private @FXML TextArea multiLink;

    private @FXML Button btnClose, btnSubmit;

    private Group group;

    private IntegerProperty itemAdded = new SimpleIntegerProperty(0);

    public MGMVAddItemModal(Group group) {
        this.group = group;

        database = FXMLSuite.getDatabase();
        youtube = FXMLSuite.getYouTube();

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

                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

                if(selectedTab.equals(tabSingular)) {
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
                        String message = e.getMessage();

                        runLater(() -> setError(message));

                        logger.error("Failed to submit link", e);
                    }
                } else if(selectedTab.equals(tabBulk)) {
                    List<String> givenLinks = Stream.of(multiLink.getText().split("\n"))
                            .map(String::trim)
                            .filter(item -> StringUtils.isNotEmpty(item) && // not empty
                                    item.toLowerCase().startsWith("http") && // is a URL
                                    item.toLowerCase().contains("youtu")) // most likely a youtu.be / youtube.com link
                            .distinct() // remove duplicates
                            .collect(Collectors.toList());

                    if(!givenLinks.isEmpty()) {
                        List<GroupItem> list = new ArrayList<>();

                        int failures = 0;

                        for(String givenLink : givenLinks) {
                            try {
                                list.add(new GroupItem(givenLink));
                            } catch (IOException e) {
                                failures++;
                            }
                        }

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
                                failures++;
                            }
                        }

                        logger.warn("Failed to parse or insert {} links for bulk add", failures);
                    } else {
                        String message = "No valid links to submit (bulk).";

                        runLater(() -> setError(message));

                        logger.info(message);
                    }
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
        multiLink.setText("");
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }
}
