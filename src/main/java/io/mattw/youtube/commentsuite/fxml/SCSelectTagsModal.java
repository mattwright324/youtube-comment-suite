package io.mattw.youtube.commentsuite.fxml;

import com.google.common.eventbus.Subscribe;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.events.TagsChangeEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static javafx.application.Platform.runLater;

public class SCSelectTagsModal extends VBox {

    private static final Logger logger = LogManager.getLogger();

    @FXML
    private ListView<String> allTags;
    @FXML
    private Button btnClose, btnSelect;

    private final CommentDatabase database;

    private List<SearchCommentsListItem> selected;

    public SCSelectTagsModal() {
        logger.debug("Initialize SCSelectTagsModal");

        database = CommentSuite.getDatabase();
        CommentSuite.getEventBus().register(this);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCSelectTagsModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            allTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            refreshAllTags();
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    @Subscribe
    public void tagsChangeEvent(TagsChangeEvent tagsChangeEvent) {
        refreshAllTags();
    }

    private void refreshAllTags() {
        try {
            final List<String> tags = database.comments().getAllTags();

            runLater(() -> {
                allTags.getItems().clear();
                allTags.getItems().addAll(tags);
            });
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    public String getSelectedString() {
        return String.join(", ", allTags.getSelectionModel().getSelectedItems());
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSelect() {
        return btnSelect;
    }

}
