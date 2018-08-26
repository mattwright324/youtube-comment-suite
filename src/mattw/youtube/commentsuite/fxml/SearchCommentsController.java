package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.ImageCache;

import java.net.URL;
import java.util.ResourceBundle;

public class SearchCommentsController implements Initializable, ImageCache {

    private Image IMG_VIDEO = new Image("/mattw/youtube/commentsuite/img/videoPlaceholder.png");
    private Image IMG_TOGGLE_CONTEXT = new Image("/mattw/youtube/commentsuite/img/toggleContext.png");

    private @FXML VBox contextPane;
    private @FXML ImageView videoThumb, authorThumb, toggleIcon;
    private @FXML TextField videoTitle, author;
    private @FXML Label toggleContext;

    private @FXML TextField groupTitle; // TODO: Rename, this is the pagination manual number field.

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        videoThumb.setImage(IMG_VIDEO);
        authorThumb.setImage(ImageCache.toLetterAvatar('m'));
        toggleIcon.setImage(IMG_TOGGLE_CONTEXT);

        contextPane.visibleProperty().bind(contextPane.managedProperty());
        toggleContext.setOnMouseClicked(me -> runLater(() -> contextPane.setManaged(!contextPane.isManaged())));


        groupTitle.setOnMouseClicked(me -> runLater(() -> {
            groupTitle.setEditable(true);
            groupTitle.getStyleClass().remove("clearTextField");
        }));
        groupTitle.focusedProperty().addListener((o, ov, nv) -> {
            if(!nv) {
                runLater(() -> {
                    groupTitle.setEditable(false);
                    groupTitle.getStyleClass().add("clearTextField");
                });
                interpretPageValue(groupTitle.getText());
            }
        });
        groupTitle.setOnKeyPressed(ke -> {
            if(ke.getCode() == KeyCode.ENTER || ke.getCode() == KeyCode.SPACE) {
                runLater(() -> {
                    groupTitle.setEditable(false);
                    groupTitle.getStyleClass().add("clearTextField");
                });
                interpretPageValue(groupTitle.getText());
            }
        });
    }

    public void interpretPageValue(String value) {
        value = value.replaceAll("[^0-9]", "").trim();
        if(value.isEmpty()) {
            value = "1";
        }
        int pageNum = Integer.valueOf(value);
        runLater(() -> groupTitle.setText(String.valueOf(pageNum)));
        // TODO: Use pagination value.
    }
}
