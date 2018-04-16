package mattw.youtube.commentsuite;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * To display account and profile.
 */
public class YouTubeAccountView extends HBox {

    private YouTubeAccount account;
    private SimpleBooleanProperty signedOut = new SimpleBooleanProperty(false);

    public YouTubeAccountView(YouTubeAccount account) {
        super(10);
        this.account = account;

        Image image = new Image(account.thumbUrl);
        if(image.isError()) {
            image = SearchCommentsPane.IMG_BLANK_PROFILE;
        }

        ImageView thumb = new ImageView(image);
        thumb.setFitWidth(24);
        thumb.setFitHeight(24);

        Label name = new Label(account.username);
        name.setMinWidth(0);
        name.setPrefWidth(0);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 15));
        HBox.setHgrow(name, Priority.ALWAYS);

        Button signOut = new Button("Sign out");
        signOut.setStyle("-fx-base: firebrick");
        signOut.setOnAction(ae -> signedOut.setValue(true));

        setPadding(new Insets(5));
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(thumb, name, signOut);
    }

    public YouTubeAccount getAccount() { return account; }
    public SimpleBooleanProperty signedOutProperty() { return signedOut; }
}
