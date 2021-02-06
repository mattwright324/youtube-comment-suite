package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.ConfigFile;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.db.GroupItem;
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
 */
public class MGMVAddItemModal extends VBox implements Cleanable {

    private static final Logger logger = LogManager.getLogger();

    @FXML private Label alertError;
    @FXML private TabPane tabPane;
    @FXML private Tab tabSingular, tabBulk;
    @FXML private CheckBox fastGroupAdd;
    @FXML private VBox singularPane;
    @FXML private TextField link;
    @FXML private Label link1, link2, link3, link4, link5;
    @FXML private VBox bulkPane;
    @FXML private TextArea multiLink;
    @FXML private Button btnClose, btnSubmit;

    private final Group group;

    private final ConfigFile<ConfigData> configFile = CommentSuite.getConfig();
    private final ConfigData configData = configFile.getDataObject();
    private final CommentDatabase database;

    public MGMVAddItemModal(final Group group) {
        this.group = group;

        database = CommentSuite.getDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVAddItemModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            bulkPane.maxHeightProperty().bind(singularPane.heightProperty());
            multiLink.maxHeightProperty().bind(singularPane.heightProperty());

            fastGroupAdd.setSelected(configData.isFastGroupAdd());

            Label[] links = {link1, link2, link3, link4, link5};
            for (Label l : links) {
                l.setOnMouseClicked(me -> {
                    Object src = me.getSource();
                    if (src instanceof Label) {
                        Label label = (Label) src;
                        runLater(() -> link.setText(label.getText()));
                    }
                });
            }

            btnSubmit.setOnAction(ae -> new Thread(() -> {
                runLater(() -> btnSubmit.setDisable(true));

                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

                if (selectedTab.equals(tabSingular)) {
                    try {
                        GroupItem newItem = new GroupItem(link.getText());

                        List<GroupItem> list = new ArrayList<>();
                        list.add(newItem);

                        if (!list.isEmpty()) {
                            try {
                                database.groupItems().insertAll(this.group, list);
                                database.commit();
                                runLater(() -> btnClose.fire());
                            } catch (SQLException e1) {
                                runLater(() -> setError(e1.getClass().getSimpleName()));
                            }
                        }
                    } catch (IOException e) {
                        String message = e.getMessage();

                        runLater(() -> setError(message));

                        logger.error("Failed to submit link", e);
                    }
                } else if (selectedTab.equals(tabBulk)) {
                    List<String> givenLinks = Stream.of(multiLink.getText().split("\n"))
                            .map(String::trim)
                            .filter(item -> StringUtils.isNotEmpty(item) && // not empty
                                    item.toLowerCase().startsWith("http") && // is a URL
                                    item.toLowerCase().contains("youtu")) // most likely a youtu.be / youtube.com link
                            .distinct() // remove duplicates
                            .collect(Collectors.toList());

                    if (!givenLinks.isEmpty()) {
                        configData.setFastGroupAdd(fastGroupAdd.isSelected());
                        configFile.save();

                        List<GroupItem> list = new ArrayList<>();

                        int failures = 0;

                        for (String givenLink : givenLinks) {
                            try {
                                GroupItem newItem = new GroupItem(givenLink, configData.isFastGroupAdd());

                                list.add(newItem);
                            } catch (IOException e) {
                                failures++;
                            }
                        }

                        if (!list.isEmpty()) {
                            try {
                                database.groupItems().insertAll(this.group, list);
                                database.commit();
                                runLater(() -> btnClose.fire());
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
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public void setError(String message) {
        alertError.setText(message);
        alertError.setVisible(true);
        alertError.setManaged(true);
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
