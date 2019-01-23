package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import mattw.youtube.commentsuite.*;
import mattw.youtube.commentsuite.db.YouTubeChannel;
import mattw.youtube.commentsuite.db.YouTubeComment;
import mattw.youtube.commentsuite.io.BrowserUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static javafx.application.Platform.runLater;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class SearchCommentsListItem extends HBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(SearchCommentsListItem.class.getName());

    private @FXML ImageView thumbnail;
    private @FXML Hyperlink author;
    private @FXML Label commentText;
    private @FXML Label date;
    private @FXML Label type;
    private @FXML Label likes;
    private @FXML Hyperlink showMore, viewTree, reply;

    private YouTubeComment comment;
    private YouTubeChannel channel;

    private BrowserUtil browserUtil = new BrowserUtil();
    private ConfigData configData;

    SearchCommentsListItem(YouTubeComment comment) throws IOException {
        this.comment = comment;

        configData = FXMLSuite.getConfig().getDataObject();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SearchCommentsListItem.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        channel = comment.getChannel();

        thumbnail.setImage(channel.getDefaultThumb());
        checkProfileThumb();

        author.setText(channel.getTitle());
        author.setOnAction(ae -> browserUtil.open(channel.buildYouTubeLink()));
        author.setBorder(Border.EMPTY);

        commentText.setText(comment.getCleanText());
        commentText.setTextOverrun(OverrunStyle.ELLIPSIS);

        date.setText(comment.getDate().toString());

        if(comment.getReplyCount() > 0 || comment.isReply()) {
            viewTree.setManaged(true);
            viewTree.setVisible(true);
            if(!comment.isReply()) {
                viewTree.setText(String.format("View Thread (%,d)", comment.getReplyCount()));
            }
        }

        if(comment.isReply()) {
            type.setText("Reply");
            this.getStyleClass().add("reply");
        }

        if(comment.getLikes() > 0) {
            likes.setText(String.format("+%,d", comment.getLikes()));
        } else {
            likes.setVisible(false);
            likes.setManaged(false);
        }

        reply.setManaged(!configData.getAccounts().isEmpty());
        reply.setVisible(!configData.getAccounts().isEmpty());

        configData.accountListChangedProperty().addListener((o, ov, nv) -> {
            reply.setManaged(!configData.getAccounts().isEmpty());
            reply.setVisible(!configData.getAccounts().isEmpty());
        });
    }

    YouTubeComment getComment() {
        return comment;
    }

    Hyperlink getShowMore() {
        return showMore;
    }

    Hyperlink getReply() {
        return reply;
    }

    Hyperlink getViewTree() {
        return viewTree;
    }

    void treeMode() {
        viewTree.setVisible(false);
        viewTree.setManaged(false);
    }

    /**
     * Loads profile thumbnail.
     */
    void loadProfileThumb() {
        runLater(() -> thumbnail.setImage(ImageLoader.LOADING.getImage()));
        Image thumbImage = ImageCache.findOrGetImage(channel);
        runLater(() -> thumbnail.setImage(thumbImage));
    }

    /**
     * Checks if profile thumb loaded and loads if present.
     */
    void checkProfileThumb() {
        if(ImageCache.hasImageCached(channel)) {
            loadProfileThumb();
        }
    }

    @Override
    public void cleanUp() {
        showMore.setOnAction(null);
        viewTree.setOnAction(null);
        reply.setOnAction(null);
    }
}
