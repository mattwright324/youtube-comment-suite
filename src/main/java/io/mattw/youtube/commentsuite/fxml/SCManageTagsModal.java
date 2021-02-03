package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.SQLLoader;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;

public class SCManageTagsModal extends VBox implements Cleanable {

    private static final Logger logger = LogManager.getLogger();

    @FXML
    private Label lblAbout, details;
    @FXML
    private TextField tags;
    @FXML
    private Button btnAdd, btnRemove, btnFinish;

    private final CommentDatabase database;

    private List<SearchCommentsListItem> selected;

    public SCManageTagsModal() {
        logger.debug("Initialize SCShowMoreModal");

        database = CommentSuite.getDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCManageTagsModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            cleanUp();

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
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    public void withComments(List<SearchCommentsListItem> selected) {
        this.selected = selected;
        //this.allTags = database.comments().getAllTags();

        runLater(() -> {
            lblAbout.setText(String.format("%s comments(s) selected", selected.size()));
        });
    }

    public List<YouTubeComment> toComments() {
        return selected.stream().map(SearchCommentsListItem::getComment).collect(Collectors.toList());
    }

    public List<String> toTags() {
        return Stream.of(tags.getText().split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    public void cleanUp() {

    }

    public Button getBtnFinish() {
        return btnFinish;
    }
}
