package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import mattw.youtube.commentsuite.ImageLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controls the header: switching content and opening the settings view.
 *
 * @author mattwright324
 */
public class MainController implements Initializable {

    private static Logger logger = LogManager.getLogger(MainController.class.getSimpleName());

    private @FXML ImageView headerIcon;
    private @FXML ToggleGroup headerToggleGroup;
    private @FXML ToggleButton btnSearchComments;
    private @FXML ToggleButton btnManageGroups;
    private @FXML ToggleButton btnSearchYoutube;
    private @FXML StackPane content;

    private @FXML Button btnSettings;
    private @FXML ImageView settingsIcon;

    private @FXML Pane searchComments;
    private @FXML Pane manageGroups;
    private @FXML Pane searchYoutube;
    private @FXML Pane settings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialize MainController");

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

        settingsIcon.setImage(ImageLoader.SETTINGS.getImage());
        btnSettings.setOnAction(ae -> runLater(() -> {
            logger.debug("Open Settings");
            settings.setManaged(true);
            settings.setVisible(true);
        }));

        btnSearchComments.setOnAction(ae -> runLater(() -> {
            headerIcon.setImage(ImageLoader.SEARCH.getImage());
            content.getChildren().clear();
            content.getChildren().add(searchComments);
        }));
        btnManageGroups.setOnAction(ae -> runLater(() -> {
            headerIcon.setImage(ImageLoader.MANAGE.getImage());
            content.getChildren().clear();
            content.getChildren().add(manageGroups);
        }));
        btnSearchYoutube.setOnAction(ae -> runLater(() -> {
            headerIcon.setImage(ImageLoader.YOUTUBE.getImage());
            content.getChildren().clear();
            content.getChildren().add(searchYoutube);
        }));

        btnSearchComments.fire();
    }
}
