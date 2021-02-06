package io.mattw.youtube.commentsuite.fxml;

import com.google.common.eventbus.Subscribe;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;

public class SCManageTagsModal extends VBox {

    private static final Logger logger = LogManager.getLogger();

    @FXML
    private Label lblAbout;
    @FXML
    private ListView<String> allTags;
    @FXML
    private TextField tags;
    @FXML
    private Button btnAdd, btnRemove, btnSelect, btnFinish;

    private final CommentDatabase database;

    private List<SearchCommentsListItem> selected;

    public SCManageTagsModal() {
        logger.debug("Initialize SCManageTagsModal");

        database = CommentSuite.getDatabase();
        CommentSuite.getEventBus().register(this);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCManageTagsModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            allTags.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            refreshAllTags();

            btnAdd.setOnAction(ae -> {
                try {
                    database.comments().associateTags(toComments(), toTags());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            btnRemove.setOnAction(ae -> {
                try {
                    database.comments().deassociateTags(toComments(), toTags());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            btnSelect.setOnAction(ae -> runLater(() -> tags.setText(String.join(", ", allTags.getSelectionModel().getSelectedItems()))));
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

    public void withComments(List<SearchCommentsListItem> selected) {
        this.selected = selected;

        runLater(() -> lblAbout.setText(String.format("%s comments(s) selected", selected.size())));
    }

    public List<YouTubeComment> toComments() {
        return selected.stream().map(SearchCommentsListItem::getComment).collect(Collectors.toList());
    }

    public List<String> toTags() {
        return Stream.of(tags.getText().split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public Button getBtnFinish() {
        return btnFinish;
    }
}
