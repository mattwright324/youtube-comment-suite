package mattw.youtube.commentsuite;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.datav3.resources.SearchList;

import java.text.SimpleDateFormat;

/**
 * Display for videos, playlists, and channels for "YouTube Search" ListView.
 */
public class SearchListView extends HBox {

    private SearchList.Item item;

    public SearchListView(SearchList.Item item, int number) {
        super(10);
        this.item = item;

        ImageView view = new ImageView(item.snippet.thumbnails.medium.url.toString());
        view.setFitHeight(80);
        view.setFitWidth(80 * view.getImage().getWidth() / view.getImage().getHeight());
        Label type = new Label(getType());

        VBox vbox = new VBox(5);
        vbox.setMaxWidth(160);
        vbox.setPrefWidth(160);
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().addAll(view, type);

        Label title = new Label(item.snippet.title);
        title.setMinWidth(0);
        title.setPrefWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 18));

        Label author = new Label(item.snippet.channelTitle);
        author.setPrefWidth(0);
        author.setMaxWidth(Double.MAX_VALUE);
        author.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

        Label desc = new Label("Published on "+sdf.format(item.snippet.publishedAt)+"  "+item.snippet.description);
        desc.setPrefWidth(0);
        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        Label num = new Label("# "+String.valueOf(number));
        num.setTextFill(Color.LIGHTGRAY);
        num.setFont(Font.font("Arial", FontWeight.NORMAL, 15));

        VBox vbox2 = new VBox(5);
        vbox2.setFillWidth(true);
        vbox2.setAlignment(Pos.CENTER_LEFT);
        vbox2.getChildren().addAll(title, author, desc, num);
        HBox.setHgrow(vbox2, Priority.ALWAYS);

        setPadding(new Insets(15));
        setFillHeight(true);
        getChildren().addAll(vbox, vbox2);
    }

    public SearchList.Item getItem() {
        return item;
    }

    public String getYouTubeLink() {
        if(item.id.channelId != null) {
            return "https://youtube.com/channel/"+item.id.channelId;
        } else if(item.id.playlistId != null) {
            return "https://youtube.com/playlist?list="+item.id.playlistId;
        } else if (item.id.videoId != null) {
            return "https://youtu.be/"+item.id.videoId;
        } else {
            return "https://youtube.com/error/no_id";
        }
    }

    public String getType() {
        if(item.id.channelId != null) {
            return "Channel";
        } else if(item.id.playlistId != null) {
            return "Playlist";
        } else if (item.id.videoId != null) {
            return "Video";
        } else {
            return "Error";
        }
    }
}
