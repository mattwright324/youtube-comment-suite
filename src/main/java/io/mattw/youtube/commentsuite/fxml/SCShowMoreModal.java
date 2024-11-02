package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.*;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
import io.mattw.youtube.commentsuite.util.BrowserUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to see a specific comment in scrollable TextArea so that they may read the comment
 * in its entirety. It also allows the user to reply to the comment with any of currently signed-into accounts
 * if they exist.
 *
 * @see SearchComments
 */
public class SCShowMoreModal extends VBox implements Cleanable, ImageCache {

    private static final Logger logger = LogManager.getLogger();

    @FXML private Label errorMsg;
    @FXML private TextArea commentText;
    @FXML private TextField author;
    @FXML private ImageView authorThumb;

    @FXML private Button btnClose;
    @FXML private Button btnSubmit;

    private YouTubeComment loadedComment;

    private final CommentDatabase database;
    private final BrowserUtil browserUtil = new BrowserUtil();
    private final ConfigFile<ConfigData> config;
    private final ConfigData configData;

    public SCShowMoreModal() {
        logger.debug("Initialize SCShowMoreModal");

        database = CommentSuite.getDatabase();
        config = CommentSuite.getConfig();
        configData = config.getDataObject();

        CommentSuite.getEventBus().register(this);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCShowMoreModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            cleanUp();
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    /**
     * Loads comment into modal.
     *
     * @param comment   comment to display
     */
    public void loadComment(YouTubeComment comment) {
        this.loadedComment = comment;

        YouTubeChannel channel = comment.getChannel();
        Image thumb = channel.findOrGetThumb();
        runLater(() -> {
            cleanUp();

            author.setText(channel.getTitle());
            authorThumb.setImage(thumb);
            commentText.setText(comment.getCleanText(true));
        });
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    @Override
    public void cleanUp() {
        author.setText("mattwright324");
        authorThumb.setImage(ImageCache.toLetterAvatar('m'));
        commentText.setText("Show more modal comment text.");

        errorMsg.setVisible(false);
        errorMsg.setManaged(false);
    }
}
