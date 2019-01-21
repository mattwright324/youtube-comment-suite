package mattw.youtube.commentsuite.fxml;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.*;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.YouTubeChannel;
import mattw.youtube.commentsuite.db.YouTubeComment;
import mattw.youtube.commentsuite.io.BrowserUtil;
import mattw.youtube.datav3.entrypoints.CommentsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to see a specific comment in scrollable TextArea so that they may read the comment
 * in its entirety. It also allows the user to reply to the comment with any of currently signed-into accounts
 * if they exist.
 *
 * @see SearchComments
 * @since 2018-12-30
 * @author mattwright324
 */
public class SCShowMoreModal extends VBox implements Cleanable, ImageCache {

    private static Logger logger = LogManager.getLogger(SCShowMoreModal.class.getSimpleName());

    private @FXML Label errorMsg;
    private @FXML TextArea commentText, replyText;
    private @FXML TextField author;
    private @FXML ImageView authorThumb, accountThumb;
    private @FXML VBox replyPane;
    private @FXML ComboBox<YouTubeAccount> comboAccountSelect;
    private @FXML CheckBox openReply;

    private @FXML Button btnClose;
    private @FXML Button btnSubmit, btnReply;

    private SimpleBooleanProperty replyMode = new SimpleBooleanProperty(false);

    private YouTubeComment loadedComment;

    private CommentDatabase database;
    private BrowserUtil browserUtil = new BrowserUtil();
    private OAuth2Handler oAuth2Handler;
    private ConfigFile<ConfigData> config;
    private ConfigData configData;

    public SCShowMoreModal() {
        logger.debug("Initialize SCShowMoreModal");

        database = FXMLSuite.getDatabase();
        oAuth2Handler = FXMLSuite.getOauth2();
        config = FXMLSuite.getConfig();
        configData = config.getDataObject();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCShowMoreModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            cleanUp();

            accountThumb.setImage(ImageCache.toLetterAvatar(' '));

            btnSubmit.setOnAction(ae -> runLater(() -> enableReplyMode(!replyMode.getValue())));

            // TODO: Account selection, sending replies
            replyMode.addListener((o, ov, nv) -> {
                btnSubmit.setText(replyMode.getValue() ? "Cancel Reply" : "Make Reply");

                if(replyText.getText().trim().isEmpty()) {
                    if(configData.getPrefixReplies()) {
                        replyText.setText(String.format("+%s ", author.getText()));
                    }
                }
            });

            comboAccountSelect.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if(nv != null) {
                    Image thumb = ImageCache.findOrGetImage(nv);

                    runLater(() -> accountThumb.setImage(thumb));
                } else {
                    runLater(() -> accountThumb.setImage(ImageCache.toLetterAvatar('a')));
                }
            });

            configData.accountListChangedProperty().addListener((o, ov, nv) -> runLater(() -> {
                comboAccountSelect.getItems().clear();
                comboAccountSelect.getItems().addAll(configData.getAccounts());
                comboAccountSelect.getSelectionModel().select(0);
            }));

            btnReply.disableProperty().bind(replyText.textProperty().length().greaterThan(0)
                    .and(comboAccountSelect.getSelectionModel().selectedIndexProperty().greaterThan(-1))
                    .and(replyPane.visibleProperty().not())
                    .or(replyText.disabledProperty()));
            btnReply.setOnAction(ae -> new Thread(() -> {
                runLater(() -> {
                    replyText.setDisable(true);
                    comboAccountSelect.setDisable(true);
                });

                try {
                    oAuth2Handler.setTokens(comboAccountSelect.getValue().getTokens());

                    String parentId = loadedComment.isReply() ?
                            loadedComment.getParentId() : loadedComment.getYoutubeId();

                    CommentsList.Item yourReply = oAuth2Handler.postReply(parentId, replyText.getText());

                    YouTubeComment comment = new YouTubeComment(yourReply, loadedComment.getVideoId());

                    database.insertComments(Collections.singletonList(comment));
                    database.commit();

                    if(openReply.isSelected()) {
                        browserUtil.open(comment.buildYouTubeLink());
                    }

                    runLater(() -> btnClose.fire());
                } catch (IOException e) {
                    logger.error("Failed to reply to comment", e);

                    runLater(() -> setError("Failed to reply to comment."));
                } catch (SQLException e) {
                    logger.error("Failed to insert your reply to the database.", e);

                    runLater(() -> setError("Successfully replied. Failed to insert your reply to the database."));
                }

                runLater(() -> {
                    replyText.setDisable(false);
                    comboAccountSelect.setDisable(false);
                });
            }).start());

            replyPane.managedProperty().bind(replyMode);
            replyPane.visibleProperty().bind(replyMode);
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    void setError(String error) {
        errorMsg.setText(error);
        errorMsg.setVisible(true);
        errorMsg.setManaged(true);
    }

    /**
     * Loads comment into modal.
     *
     * @param comment comment to display
     * @param replyMode show modal with reply elements enabled
     */
    public void loadComment(YouTubeComment comment, boolean replyMode) {
        this.loadedComment = comment;

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

        errorMsg.setVisible(false);
        errorMsg.setManaged(false);
    }
}
