package mattw.youtube.commentsuite;

import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Display for comments in the database.
 */
public class YouTubeCommentView extends HBox {

    private YouTubeComment comment;
    private YouTubeChannel channel;

    public YouTubeCommentView(YouTubeComment comment, YouTubeChannel channel) {
        super(10);
        this.channel = channel;
        this.comment = comment;

        ImageView iv = new ImageView(CommentSuite.IMG_BLANK_PROFILE);
        iv.setFitHeight(30);
        iv.setFitWidth(30);

        VBox vbox0 = new VBox(5);
        vbox0.setAlignment(Pos.CENTER);
        vbox0.getChildren().addAll(iv, new Label("Comment"));

        Label author = new Label("Name");
        author.setMinWidth(0);
        author.setPrefWidth(0);
        author.setMaxWidth(Double.MAX_VALUE);

        Label alltext = new Label("Text");
        alltext.setMinWidth(0);
        alltext.setPrefWidth(0);
        alltext.setMaxWidth(Double.MAX_VALUE);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

        Label date = new Label(sdf.format(new Date(System.currentTimeMillis())));
        date.setTextFill(Color.LIGHTGRAY);

        Hyperlink showMore = new Hyperlink("Show more");
        Hyperlink reply = new Hyperlink("Reply");
        Hyperlink allReplies = new Hyperlink("View Tree");

        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getChildren().addAll(date, reply, allReplies, showMore);

        VBox vbox1 = new VBox(5);
        vbox1.setMinWidth(0);
        vbox1.setPrefWidth(0);
        vbox1.setMaxWidth(Double.MAX_VALUE);
        vbox1.setAlignment(Pos.CENTER_LEFT);
        vbox1.getChildren().addAll(author, alltext, hbox);
        HBox.setHgrow(vbox1, Priority.ALWAYS);

        setFillHeight(true);
        getChildren().addAll(vbox0, vbox1);
    }
}
