package io.mattw.youtube.commentsuite.fxml;

import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.common.eventbus.Subscribe;
import io.mattw.youtube.commentsuite.*;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
import io.mattw.youtube.commentsuite.events.AccountAddEvent;
import io.mattw.youtube.commentsuite.events.AccountDeleteEvent;
import io.mattw.youtube.commentsuite.oauth2.OAuth2Manager;
import io.mattw.youtube.commentsuite.oauth2.YouTubeAccount;
import io.mattw.youtube.commentsuite.util.BrowserUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
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
 */
public class SCShowMoreModal extends VBox implements Cleanable, ImageCache {

    private static final Logger logger = LogManager.getLogger();

    @FXML private Label errorMsg;
    @FXML private TextArea commentText, replyText;
    @FXML private TextField author;
    @FXML private ImageView authorThumb, accountThumb;
    @FXML private VBox replyPane;
    @FXML private ComboBox<YouTubeAccount> comboAccountSelect;
    @FXML private CheckBox openReply;

    @FXML private Button btnClose;
    @FXML private Button btnSubmit, btnReply;

    private SimpleBooleanProperty replyMode = new SimpleBooleanProperty(false);

    private YouTubeComment loadedComment;

    private final CommentDatabase database;
    private final BrowserUtil browserUtil = new BrowserUtil();
    private final OAuth2Manager oAuth2Manager;
    private final ConfigFile<ConfigData> config;
    private final ConfigData configData;

    public SCShowMoreModal() {
        logger.debug("Initialize SCShowMoreModal");

        database = CommentSuite.getDatabase();
        oAuth2Manager = CommentSuite.getOauth2Manager();
        config = CommentSuite.getConfig();
        configData = config.getDataObject();

        CommentSuite.getEventBus().register(this);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCShowMoreModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            cleanUp();

            accountThumb.setImage(ImageCache.toLetterAvatar(' '));

            btnSubmit.setOnAction(ae -> runLater(() -> enableReplyMode(!replyMode.getValue())));

            replyMode.addListener((o, ov, nv) -> {
                btnSubmit.setText(replyMode.getValue() ? "Cancel Reply" : "Make Reply");

                if (replyText.getText().trim().isEmpty()) {
                    if (configData.isPrefixReplies()) {
                        replyText.setText(String.format("@%s ", author.getText()));
                    }
                }
            });

            comboAccountSelect.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (nv != null) {
                    Image thumb = ImageCache.findOrGetImage(nv);

                    runLater(() -> accountThumb.setImage(thumb));
                } else {
                    runLater(() -> accountThumb.setImage(ImageCache.toLetterAvatar('a')));
                }
            });

            reloadAccountList();

            btnReply.visibleProperty().bind(btnReply.managedProperty());
            btnReply.managedProperty().bind(replyPane.visibleProperty());
            btnReply.disableProperty().bind(
                    replyText.textProperty().isEmpty()
                            .or(comboAccountSelect.getSelectionModel()
                                    .selectedIndexProperty().isEqualTo(-1))
            );

            btnReply.setOnAction(ae -> new Thread(() -> {
                runLater(() -> {
                    replyText.setDisable(true);
                    comboAccountSelect.setDisable(true);
                });

                try {
                    final YouTubeAccount selectedAccount = comboAccountSelect.getValue();
                    final String parentId = loadedComment.isReply() ?
                            loadedComment.getParentId() : loadedComment.getId();
                    final Comment yourReply = postReply(selectedAccount, parentId, replyText.getText());
                    final YouTubeComment comment = new YouTubeComment(yourReply, loadedComment.getVideoId());

                    database.insertComments(Collections.singletonList(comment));
                    database.commit();

                    if (openReply.isSelected()) {
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

    private void reloadAccountList() {
        comboAccountSelect.getItems().clear();
        comboAccountSelect.getItems().addAll(configData.getAccounts());
        comboAccountSelect.getSelectionModel().select(0);
    }

    @Subscribe
    public void accountAddEvent(final AccountAddEvent accountAddEvent) {
        reloadAccountList();
    }

    @Subscribe
    public void accountDeleteEvent(final AccountDeleteEvent accountDeleteEvent) {
        reloadAccountList();
    }

    void setError(String error) {
        errorMsg.setText(error);
        errorMsg.setVisible(true);
        errorMsg.setManaged(true);
    }

    /**
     * Loads comment into modal.
     *
     * @param comment   comment to display
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
            commentText.setText(comment.getCleanText(true));

            enableReplyMode(replyMode);
        });
    }

    /**
     * Attempts to send a reply to the parent comment id and text supplied. It will attempt to send to reply 5 times
     * after failure and throwing an error. On each failure, if it detects the tokens used by the account have
     * expired, it will attempt to refresh them and use and newly updated tokens.
     *
     * @param parentId     id of comment or parentId of reply-comment to reply to
     * @param textOriginal text to reply to the comment with
     * @throws IOException failed to reply
     */
    public Comment postReply(final YouTubeAccount account, final String parentId, final String textOriginal) throws IOException {
        final CommentSnippet snippet = new CommentSnippet();
        snippet.setParentId(parentId);
        snippet.setTextOriginal(textOriginal);

        final Comment comment = new Comment();
        comment.setSnippet(snippet);

        int attempt = 0;
        do {
            try {
                final Comment result = CommentSuite.getYouTube().comments()
                        .insert("snippet", comment)
                        .setOauthToken(account.getTokens().getAccessToken())
                        .execute();

                logger.debug("Successfully replied [id={}]", result.getId());

                return result;
            } catch (IOException e) {
                logger.warn("Failed on comment reply, {}", e.getLocalizedMessage());
                logger.debug("Refreshing tokens and trying again [attempt={}]", attempt);

                oAuth2Manager.getNewAccessToken(account);
            }

            attempt++;
        } while (attempt < 5);

        throw new IOException("Could not reply and failed to refresh tokens.");
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
