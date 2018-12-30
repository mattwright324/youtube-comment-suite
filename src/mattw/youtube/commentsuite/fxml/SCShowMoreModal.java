package mattw.youtube.commentsuite.fxml;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.*;
import mattw.youtube.commentsuite.db.YouTubeChannel;
import mattw.youtube.commentsuite.db.YouTubeComment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static javafx.application.Platform.runLater;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class SCShowMoreModal extends VBox implements Cleanable, ImageCache {

    private static Logger logger = LogManager.getLogger(SCShowMoreModal.class.getSimpleName());

    private @FXML TextArea commentText, replyText;
    private @FXML TextField author;
    private @FXML ImageView authorThumb, accountThumb;
    private @FXML VBox replyPane;
    private @FXML ComboBox<YouTubeAccount> comboAccountSelect;

    private @FXML Button btnClose;
    private @FXML Button btnSubmit, btnReply;

    private SimpleBooleanProperty replyMode = new SimpleBooleanProperty(false);

    private ConfigFile<ConfigData> config;
    private ConfigData configData;

    public SCShowMoreModal() {
        logger.debug("Initialize SCShowMoreModal");

        config = FXMLSuite.getConfig();
        configData = config.getDataObject();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCShowMoreModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            cleanUp();

            accountThumb.setImage(ImageCache.toLetterAvatar('a'));

            btnSubmit.setOnAction(ae -> runLater(() -> enableReplyMode(!replyMode.getValue())));

            replyMode.addListener((o, ov, nv) -> {
                btnSubmit.setText(replyMode.getValue() ? "Cancel Reply" : "Make Reply");

                if(replyText.getText().trim().isEmpty()) {
                    if(configData.getPrefixReplies()) {
                        replyText.setText(String.format("+%s ", author.getText()));
                    }
                }
            });

            replyPane.managedProperty().bind(replyMode);
            replyPane.visibleProperty().bind(replyMode);
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    /**
     * Loads comment into modal.
     *
     * @param comment comment to display
     * @param replyMode show modal with reply elements enabled
     */
    public void loadComment(YouTubeComment comment, boolean replyMode) {
        YouTubeChannel channel = comment.getChannel();

        Image thumb = ImageCache.findOrGetImage(channel);

        runLater(() -> {
            cleanUp();

            author.setText(channel.getTitle());
            authorThumb.setImage(thumb);
            commentText.setText(comment.getCleanText());

            enableReplyMode(replyMode);
        });
    }

    public void enableReplyMode(boolean enable) {
        replyMode.setValue(enable);
    }

    public BooleanProperty replyModeProperty() {
        return replyMode;
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    @Override
    public void cleanUp() {
        enableReplyMode(false);

        author.setText("mattwright324");
        authorThumb.setImage(ImageCache.toLetterAvatar('m'));
        commentText.setText("Show more modal comment text.");

        replyText.setText("");
    }
}
