package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.ImageLoader;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

import static javafx.application.Platform.runLater;

/**
 * Controls the header: switching content and opening the settings view.
 *
 */
public class Main implements Initializable {

    private static final Logger logger = LogManager.getLogger();

    @FXML private ImageView headerIcon;
    @FXML private ToggleGroup headerToggleGroup;
    @FXML private ToggleButton btnSearchComments;
    @FXML private ToggleButton btnManageGroups;
    @FXML private ToggleButton btnSearchYoutube;
    @FXML private StackPane content;

    @FXML private Button btnQuota, btnSettings;
    @FXML private ImageView settingsIcon, quotaIcon;

    @FXML private Pane searchComments;
    @FXML private Pane manageGroups;
    @FXML private Pane searchYoutube;
    @FXML private Pane settings;

    @FXML private OverlayModal<MainQuotaModal> quotaInfoModal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialize Main");

        headerToggleGroup.getToggles().addListener((ListChangeListener<Toggle>) c -> {
            while (c.next()) {
                for (final Toggle addedToggle : c.getAddedSubList()) {
                    ((ToggleButton) addedToggle).addEventFilter(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
                        if (addedToggle.equals(headerToggleGroup.getSelectedToggle()))
                            mouseEvent.consume();
                    });
                }
            }
        });

        quotaIcon.setImage(ImageLoader.QUOTA.getImage());
        btnQuota.setOnAction(ae -> runLater(() -> logger.debug("Open Quota info")));

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

        MainQuotaModal quotaModal = new MainQuotaModal();
        quotaInfoModal.setContent(quotaModal);
        quotaModal.getBtnClose().setOnAction(ae -> quotaInfoModal.setVisible(false));
        btnQuota.setOnAction(ae -> quotaInfoModal.setVisible(true));

        btnManageGroups.fire();
    }
}
