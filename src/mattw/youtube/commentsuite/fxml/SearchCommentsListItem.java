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
import mattw.youtube.commentsuite.Cleanable;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.db.YouTubeChannel;
import mattw.youtube.commentsuite.db.YouTubeComment;
import mattw.youtube.commentsuite.io.BrowserUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static javafx.application.Platform.runLater;

/**
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

    SearchCommentsListItem(YouTubeComment comment) throws IOException {
        this.comment = comment;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SearchCommentsListItem.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        channel = comment.getChannel();

        thumbnail.setImage(channel.getDefaultThumb());
        checkProfileThumb();

        author.setText(channel.getTitle());
        author.setOnAction(ae -> browserUtil.open(channel.getYouTubeLink()));
        author.setBorder(Border.EMPTY);

        commentText.setText(StringEscapeUtils.unescapeHtml4(comment.getText()));
        commentText.setTextOverrun(OverrunStyle.ELLIPSIS);

        date.setText(comment.getDate().toString());

        if(comment.getReplyCount() > 0 || comment.isReply()) {
            viewTree.setManaged(true);
            viewTree.setVisible(true);
            if(!comment.isReply()) {
                viewTree.setText(String.format("View Tree (%,d)", comment.getReplyCount()));
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

        reply.setManaged(false);
        reply.setVisible(false);
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
