package mattw.youtube.commentsuite;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.db.YouTubeVideo;

import java.text.SimpleDateFormat;

public class YouTubeVideoView extends HBox {

    private YouTubeVideo video;

    public YouTubeVideoView(YouTubeVideo video) {
        super(10);
        this.video = video;

        ImageView thumb = new ImageView();
        thumb.setFitHeight(50);
        thumb.setFitWidth(89);

        new Thread(() -> {
            thumb.setImage(new Image(video.getThumbUrl()));
        }).start();

        Label title = new Label(video.getTitle());

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
