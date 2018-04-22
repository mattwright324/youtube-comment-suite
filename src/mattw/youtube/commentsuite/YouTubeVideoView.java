package mattw.youtube.commentsuite;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.commentsuite.db.YouTubeVideo;

import java.text.SimpleDateFormat;

public class YouTubeVideoView extends HBox {

    private YouTubeVideo video;

    public YouTubeVideoView(YouTubeVideo video) {
        super(10);
        setAlignment(Pos.CENTER_LEFT);
        this.video = video;

        ImageView thumb = new ImageView();
        thumb.setFitHeight(50);
        thumb.setFitWidth(89);

        new Thread(() -> thumb.setImage(video.getThumbnail())).start();

        Label title = new Label(video.getTitle());
        title.setFont(Font.font(("Tahoma"), FontWeight.SEMI_BOLD, 15));

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
        Label published = new Label("Published "+sdf.format(video.getPublishedDate()));
        published.setPrefWidth(200);
        Label views = new Label(String.format("%,d",video.getViews())+" views");

        HBox data = new HBox(5);
        data.getChildren().addAll(published, views);

        VBox vbox = new VBox(4);
        vbox.getChildren().addAll(title, data);

        getChildren().addAll(thumb, vbox);
    }

    public YouTubeVideo getVideo() { return video; }

}
