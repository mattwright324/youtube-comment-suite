package mattw.youtube.commentsuite.fxml;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controls the header: switching content and opening the settings view.
 *
 * @author mattwright324
 */
public class MainController implements Initializable {

    public final Image IMG_MANAGE = new Image("/mattw/youtube/commentsuite/img/manage.png");
    public final Image IMG_SEARCH = new Image("/mattw/youtube/commentsuite/img/search.png");
    public final Image IMG_YOUTUBE = new Image("/mattw/youtube/commentsuite/img/youtube.png");

    public final Image IMG_SETTINGS = new Image("/mattw/youtube/commentsuite/img/settings.png");

    @FXML ImageView headerIcon;
    @FXML ToggleGroup headerToggleGroup;
    @FXML ToggleButton btnSearchComments;
    @FXML ToggleButton btnManageGroups;
    @FXML ToggleButton btnSearchYoutube;
    @FXML StackPane content;

    @FXML Button btnSettings;
    @FXML ImageView settingsView;

    @FXML Pane searchComments;
    @FXML Pane manageGroups;
    @FXML Pane searchYoutube;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        headerToggleGroup.getToggles().addListener((ListChangeListener<Toggle>) c -> {
            while(c.next()) {
                for(final Toggle addedToggle : c.getAddedSubList()) {
                    ((ToggleButton) addedToggle).addEventFilter(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
                        if(addedToggle.equals(headerToggleGroup.getSelectedToggle()))
                            mouseEvent.consume();
                    });
                }
            }
        });

        settingsView.setImage(IMG_SETTINGS);

        btnSearchComments.setOnAction(ae -> Platform.runLater(() -> {
            headerIcon.setImage(IMG_SEARCH);
            content.getChildren().clear();
            content.getChildren().add(searchComments);
        }));
        btnManageGroups.setOnAction(ae -> Platform.runLater(() -> {
            headerIcon.setImage(IMG_MANAGE);
            content.getChildren().clear();
            content.getChildren().add(manageGroups);
        }));
        btnSearchYoutube.setOnAction(ae -> Platform.runLater(() -> {
            headerIcon.setImage(IMG_YOUTUBE);
            content.getChildren().clear();
            content.getChildren().add(searchYoutube);
        }));

        btnManageGroups.fire();
    }
}
