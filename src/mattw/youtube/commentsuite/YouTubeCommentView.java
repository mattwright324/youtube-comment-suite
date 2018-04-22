package mattw.youtube.commentsuite;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.commentsuite.db.YouTubeChannel;
import mattw.youtube.commentsuite.db.YouTubeComment;
import mattw.youtube.commentsuite.db.YouTubeObject;
import mattw.youtube.commentsuite.io.Browser;
import org.jsoup.Jsoup;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Display for comments in the database for the "Search Comments" ListView.
 */
public class YouTubeCommentView extends HBox {

    private ImageView thumb = new ImageView(SearchCommentsPane.IMG_BLANK_PROFILE);

    private YouTubeComment comment;
    private YouTubeChannel channel;

    private String parsedText;

    public YouTubeCommentView(YouTubeComment comment, boolean tree) {
        super(10);
        setMinHeight(100);
        setPrefHeight(100);
        setMaxHeight(100);
        this.comment = comment;
        this.channel = CommentSuite.db().getChannel(comment.getChannelId());

        boolean signedIn = CommentSuite.config().isSignedIn(channel.getYouTubeId());

        thumb.setImage(channel.getDefaultThumb());
        if(channel.fetchThumb() || channel.thumbCached() || signedIn) {
            updateProfileThumb();
        }
        thumb.setFitHeight(32);
        thumb.setFitWidth(32);

        ImageView type = new ImageView();
        type.setFitHeight(24);
        type.setFitWidth(24);
        if(comment.isReply()) {
            type.setImage(new Image("/mattw/youtube/commentsuite/img/reply.png"));
            Tooltip.install(type, new Tooltip("This comment is a reply."));
        }

        VBox vbox0 = new VBox(5);
        vbox0.setAlignment(Pos.CENTER);
        vbox0.setMinWidth(75);
        vbox0.setPrefWidth(75);
        vbox0.setMaxWidth(75);
        vbox0.getChildren().addAll(thumb, type/*, new Label(comment.isReply() ? "Reply" : "Comment")*/);

        Hyperlink author = new Hyperlink(channel.getTitle());
        author.setPadding(new Insets(0));
        author.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 15));
        author.setOnAction(ae -> Browser.open(channel.getYouTubeLink()));
        if(signedIn) {
            author.setStyle("-fx-font-weight: bold");
            author.setTextFill(Color.GREEN);
        }

        parsedText = Jsoup.parse(comment.getText().replace("<br />", "\r\n")).text();

        Label text = new Label(parsedText);
        text.setMinWidth(0);
        text.setPrefWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

        Label date = new Label(sdf.format(comment.getDate()));
        date.setTextFill(Color.LIGHTGRAY);

        Hyperlink showMore = new Hyperlink("Show more");
        showMore.setOnAction(ae -> CommentSuite.instance().searchComments.showMore(this));

        Hyperlink reply = new Hyperlink("Reply");
        reply.setManaged(!CommentSuite.config().getAccounts().isEmpty());
        reply.setOnAction(ae -> CommentSuite.instance().searchComments.reply(this));

        Hyperlink viewTree = new Hyperlink("View Thread"+(comment.getReplyCount() > 0 ? " ("+comment.getReplyCount()+")":""));
        viewTree.setManaged(tree && (comment.isReply() || comment.getReplyCount() > 0));
        viewTree.setOnAction(ae -> CommentSuite.instance().searchComments.viewTree(this));

        Label likes = new Label("+"+comment.getLikes());
        likes.setTextFill(Color.CORNFLOWERBLUE);

        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getChildren().add(date);
        if(comment.getLikes() > 0) hbox.getChildren().add(likes);
        hbox.getChildren().addAll(reply, viewTree, showMore);

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

    public boolean isThumbLoaded() {
        return YouTubeObject.thumbCache.containsKey(channel.getYouTubeId());
    }

    /**
     * Checks if the thumbnail has been cached before loading.
     */
    public void checkProfileThumb() {
        if(isThumbLoaded()) {
            updateProfileThumb();
        }
    }
}
