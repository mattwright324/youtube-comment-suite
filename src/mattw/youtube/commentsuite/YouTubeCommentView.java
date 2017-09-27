package mattw.youtube.commentsuite;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jsoup.Jsoup;

import java.text.SimpleDateFormat;

/**
 * Display for comments in the database for the "Search Comments" ListView.
 */
public class YouTubeCommentView extends HBox {

    private ImageView thumb = new ImageView(CommentSuite.IMG_BLANK_PROFILE);

    private YouTubeComment comment;
    private YouTubeChannel channel;

    private String parsedText;

    public YouTubeCommentView(YouTubeComment comment, YouTubeChannel channel) {
        super(10);
        this.comment = comment;
        this.channel = channel;

        if(channel.fetchThumb() || channel.thumbCached()) { updateProfileThumb(); }
        thumb.setFitHeight(30);
        thumb.setFitWidth(30);

        VBox vbox0 = new VBox(5);
        vbox0.setAlignment(Pos.CENTER);
        vbox0.setMinWidth(75);
        vbox0.setPrefWidth(75);
        vbox0.setMaxWidth(75);
        vbox0.getChildren().addAll(thumb, new Label(comment.isReply() ? "Reply" : "Comment"));

        Hyperlink author = new Hyperlink(channel.getTitle());
        author.setOnAction(ae -> CommentSuite.openInBrowser(channel.getYouTubeLink()));

        parsedText = Jsoup.parse(comment.getText().replace("<br />", "\r\n")).text();

        Label text = new Label(parsedText);
        text.setMinWidth(0);
        text.setPrefWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

        Label date = new Label(sdf.format(comment.getDate()));
        date.setTextFill(Color.LIGHTGRAY);

        Hyperlink showMore = new Hyperlink("Show more");
        Hyperlink reply = new Hyperlink("Reply");
        reply.setManaged(false); // false
        Hyperlink allReplies = new Hyperlink("View Tree"+(comment.getReplyCount() > 0 ? " ("+comment.getReplyCount()+")":""));
        allReplies.setManaged(comment.isReply() || comment.getReplyCount() > 0);

        Label likes = new Label("+"+comment.getLikes());
        likes.setTextFill(Color.CORNFLOWERBLUE);

        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getChildren().add(date);
        if(comment.getLikes() > 0) hbox.getChildren().add(likes);
        hbox.getChildren().addAll(reply, allReplies, showMore);

        VBox vbox1 = new VBox(5);
        vbox1.setMinWidth(0);
        vbox1.setPrefWidth(0);
        vbox1.setMaxWidth(Double.MAX_VALUE);
        vbox1.setAlignment(Pos.CENTER_LEFT);
        vbox1.setFillWidth(true);
        vbox1.getChildren().addAll(author, text, hbox);
        HBox.setHgrow(vbox1, Priority.ALWAYS);

        if(comment.isReply()) {
            setStyle("-fx-background-color: linear-gradient(to right, rgba(127,255,127,0.2), transparent);");
        }
        setPadding(new Insets(5));
        setFillHeight(true);
        getChildren().addAll(vbox0, vbox1);
    }

    public String getParsedText() { return parsedText; }
    public YouTubeComment getComment() { return comment; }
    public YouTubeChannel getChannel() { return channel; }

    /**
     * Forces the channel to grab and cache the profile thumbnail.
     */
    public void updateProfileThumb() { thumb.setImage(channel.getThumbnail()); }

    /**
     * Checks if the thumbnail has been cached before loading.
     */
    public void checkProfileThumb() {
        if(YouTubeObject.thumbCache.containsKey(channel.getYouTubeId())) {
            updateProfileThumb();
        }
    }
}
