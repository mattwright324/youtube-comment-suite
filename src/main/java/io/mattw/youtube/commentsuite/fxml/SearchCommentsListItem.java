package io.mattw.youtube.commentsuite.fxml;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ImageCache;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
import io.mattw.youtube.commentsuite.events.*;
import io.mattw.youtube.commentsuite.util.BrowserUtil;
import io.mattw.youtube.commentsuite.util.DateUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.LocalDateTime;

import static javafx.application.Platform.runLater;

public class SearchCommentsListItem extends HBox implements Cleanable {

    private static final Logger logger = LogManager.getLogger();
    private static final LocalDateTime DAYS_AGO_60 = LocalDateTime.now().minusDays(60);

    @FXML private ImageView thumbnail;
    @FXML private Hyperlink author;
    @FXML private Label commentText;
    @FXML private Label date;
    @FXML private Label type;
    @FXML private Label likes;
    @FXML private Hyperlink showMore, viewTree;
    @FXML private HBox systemTagsPane, userTagsPane;

    private final BrowserUtil browserUtil = new BrowserUtil();
    private final ConfigData configData;
    private final EventBus eventBus;

    private final YouTubeComment comment;
    private final YouTubeChannel channel;
    private boolean showReplyBtn = true;

    public SearchCommentsListItem(final YouTubeComment comment) throws IOException {
        this.comment = comment;
        this.channel = comment.getChannel();

        configData = CommentSuite.getConfig().getDataObject();
        eventBus = CommentSuite.getEventBus();
        eventBus.register(this);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SearchCommentsListItem.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        checkProfileThumb();

        author.setText(channel.getTitle());
        author.setOnAction(ae -> browserUtil.open(channel.toYouTubeLink()));
        author.setBorder(Border.EMPTY);

        commentText.setText(comment.getCleanText(false).replace("\r\n", " "));
        commentText.setTextOverrun(OverrunStyle.ELLIPSIS);

        date.setText(DateUtils.epochMillisToDateTime(comment.getPublished()).toString());

        if (comment.getReplyCount() > 0 || comment.isReply()) {
            viewTree.setManaged(true);
            viewTree.setVisible(true);
            if (!comment.isReply()) {
                viewTree.setText(String.format("View Thread (%,d)", comment.getReplyCount()));
            }
        }

        if (comment.isReply()) {
            this.getStyleClass().add("reply");
            type.setText("Reply");
        }

        if (comment.getLikes() > 0) {
            likes.setText(String.format("+%,d", comment.getLikes()));
        } else {
            likes.setVisible(false);
            likes.setManaged(false);
        }

        reloadUserTags();

        showMore.setOnAction(ae -> eventBus.post(new ShowMoreEvent(this)));
        viewTree.setOnAction(ae -> eventBus.post(new ViewTreeEvent(this)));
    }

    public YouTubeComment getComment() {
        return comment;
    }

    public void addTag(Pane pane, String text) {
        final Label tag = new Label(text);
        tag.getStyleClass().addAll("textMuted", "tag");
        runLater(() -> pane.getChildren().add(tag));
    }

    @Subscribe
    public void tagsChangeEvent(final TagsChangeEvent tagsChangeEvent) {
        if (tagsChangeEvent.wasChanged(comment)) {
            tagsChangeEvent.updateTags(comment);

            reloadUserTags();
        }
    }

    public void reloadUserTags() {
        runLater(() -> userTagsPane.getChildren().clear());

        if (comment.getTags() != null) {
            comment.getTags().stream().sorted().forEach(tag -> runLater(() -> addTag(userTagsPane, tag)));
        }
    }

    public void treeMode() {
        viewTree.setVisible(false);
        viewTree.setManaged(false);
    }

    /**
     * Loads profile thumbnail.
     */
    public void loadProfileThumb() {
        runLater(() -> thumbnail.setImage(ImageLoader.LOADING.getImage()));
        Image thumbImage = channel.findOrGetThumb();
        runLater(() -> thumbnail.setImage(thumbImage));
    }

    /**
     * Checks if profile thumb loaded and loads if present.
     */
    public void checkProfileThumb() {
        if (channel.isThumbLoaded()) {
            loadProfileThumb();
        } else {
            runLater(() -> thumbnail.setImage(ImageCache.toLetterAvatar(channel.getTitle())));
        }
    }

    @Override
    public void cleanUp() {
        showMore.setOnAction(null);
        viewTree.setOnAction(null);
    }
}
