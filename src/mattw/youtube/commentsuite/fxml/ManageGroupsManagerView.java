package mattw.youtube.commentsuite.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.db.Group;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Random;

/**
 * Manages a specific group; refreshing, stats, renaming, deletion, adding group items, etc.
 *
 * Loads template FXML and displays info from database.
 *
 * @author mattwright324
 */
public class ManageGroupsManagerView extends StackPane implements ImageCache {

    private Logger logger = LogManager.getLogger(this.toString());
    private Image loading = new Image("/mattw/youtube/commentsuite/img/loading.png");
    private Image edit = new Image("/mattw/youtube/commentsuite/img/pencil.png");
    private Image close = new Image("/mattw/youtube/commentsuite/img/close.png");

    private Random random = new Random();

    @FXML TextField title;
    @FXML ImageView editIcon;
    @FXML Hyperlink rename;

    public ManageGroupsManagerView(Group group) throws IOException {
        logger.debug(String.format("Initialize for Group [id=%s,name=%s]", group.getId(), group.getName()));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("ManageGroupsManager.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        this.setStyle(String.format("-fx-background-color: linear-gradient(to right, rgba(%s,%s,%s,%s), transparent);",
                220-random.nextInt(60), 220-random.nextInt(60), 220-random.nextInt(60), 0.4));

        editIcon.setImage(edit);

        title.setMinWidth(Region.USE_PREF_SIZE);
        title.setMaxWidth(Region.USE_PREF_SIZE);
        title.textProperty().addListener((ov, prevText, currText) -> resizeTextField(title));
        title.fontProperty().addListener((o, ov, nv) -> { resizeTextField(title); });
        title.setText(group.getName());
        title.setOnKeyPressed(ke -> {
            if(ke.getCode() == KeyCode.ENTER) {
                rename.fire();
            }
        });

        rename.setOnAction(ae -> Platform.runLater(() -> {
            if(editIcon.getImage().equals(edit)) {
                editIcon.setImage(close);
                title.getStyleClass().remove("clearTextField");
                title.setEditable(true);
                rename.setTooltip(new Tooltip("Save Changes"));
            } else {
                editIcon.setImage(edit);
                title.getStyleClass().add("clearTextField");
                title.setEditable(false);
                rename.setTooltip(new Tooltip("Rename"));
            }
        }));
    }

    /**
     * Source: https://stackoverflow.com/a/25643696/2650847
     * Modifies a TextField's preferred width based on it's text content.
     * @param field
     */
    private void resizeTextField(TextField field) {
        Platform.runLater(() -> {
            Text text = new Text(field.getText());
            text.setFont(field.getFont());
            double width = text.getLayoutBounds().getWidth()
                    + field.getPadding().getLeft() + field.getPadding().getRight()
                    + 3d;
            field.setPrefWidth(width);
            field.positionCaret(field.getCaretPosition());
        });
    }

}
