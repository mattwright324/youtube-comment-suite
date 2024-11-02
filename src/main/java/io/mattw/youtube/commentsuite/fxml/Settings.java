package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.ConfigFile;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.util.BrowserUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static javafx.application.Platform.runLater;

public class Settings implements Initializable {

    private static final Logger logger = LogManager.getLogger();

    private BrowserUtil browserUtil = new BrowserUtil();
    private ConfigFile<ConfigData> config;
    private ConfigData configData;
    private CommentDatabase database;

    @FXML private Pane settingsPane;

    @FXML private VBox vboxSettings;
    @FXML private Button btnClose;
    @FXML private ImageView closeIcon;
    @FXML private CheckBox autoLoadStats;
    @FXML private CheckBox downloadThumbs;
    @FXML private CheckBox customKey;
    @FXML private CheckBox filterDuplicatesOnCopy;
    @FXML private TextField youtubeApiKey;

    @FXML private ProgressIndicator cleanProgress;
    @FXML private Button btnClean;
    @FXML private ProgressIndicator resetProgress;
    @FXML private Button btnReset;
    @FXML private ProgressIndicator removeProgress;
    @FXML private Button btnRemoveThumbs;

    @FXML private Hyperlink github;
    @FXML private ImageView githubIcon;
    @FXML private Button btnSave;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialize Settings");

        database = CommentSuite.getDatabase();
        config = CommentSuite.getConfig();
        configData = config.getDataObject();
        CommentSuite.getEventBus().register(this);

        autoLoadStats.setSelected(configData.isAutoLoadStats());
        downloadThumbs.setSelected(configData.isArchiveThumbs());
        customKey.setSelected(configData.isCustomApiKey());
        youtubeApiKey.setText(configData.getYoutubeApiKey());
        filterDuplicatesOnCopy.setSelected(configData.isFilterDuplicatesOnCopy());

        btnSave.setOnAction(ae -> runLater(() -> btnClose.fire()));

        closeIcon.setImage(ImageLoader.CLOSE.getImage());
        btnClose.setOnAction(ae -> runLater(() -> {
            logger.debug("Saving Settings");

            ConfigData data = config.getDataObject();
            data.setArchiveThumbs(downloadThumbs.isSelected());
            data.setAutoLoadStats(autoLoadStats.isSelected());
            data.setCustomApiKey(customKey.isSelected());
            data.setFilterDuplicatesOnCopy(filterDuplicatesOnCopy.isSelected());
            data.setYoutubeApiKey(youtubeApiKey.getText());

            config.setDataObject(data);
            config.save();

            logger.debug("Closing Settings");
            settingsPane.setManaged(false);
            settingsPane.setVisible(false);
        }));

        youtubeApiKey.disableProperty().bind(customKey.selectedProperty().not());

        githubIcon.setImage(ImageLoader.GITHUB.getImage());

        btnClean.setOnAction(ae -> new Thread(() -> {
            runLater(() -> {
                btnClean.setDisable(true);
                btnReset.setDisable(true);
                cleanProgress.setVisible(true);
            });
            try {
                logger.warn("Starting DB Clean");
                database.cleanUp();
                database.vacuum();
            } catch (Exception e) {
                logger.error(e);
            }
            runLater(() -> {
                btnClean.setDisable(false);
                btnReset.setDisable(false);
                cleanProgress.setVisible(false);
            });
        }).start());

        btnReset.setOnAction(ae -> new Thread(() -> {
            runLater(() -> {
                btnClean.setDisable(true);
                btnReset.setDisable(true);
                resetProgress.setVisible(true);
            });
            try {
                logger.warn("Starting DB Reset");
                database.reset();
            } catch (Exception e) {
                logger.error(e);
            }
            runLater(() -> {
                btnClean.setDisable(false);
                btnReset.setDisable(false);
                resetProgress.setVisible(false);
            });
        }).start());

        btnRemoveThumbs.setOnAction(ae -> new Thread(() -> {
            runLater(() -> {
                btnRemoveThumbs.setDisable(true);
                removeProgress.setVisible(true);
            });

            deleteDirectoryContents("thumbs/");

            runLater(() -> {
                btnRemoveThumbs.setDisable(false);
                removeProgress.setVisible(false);
            });
        }).start());

        github.setOnAction(ae -> browserUtil.open("https://github.com/mattwright324/youtube-comment-suite"));
    }

    private void deleteDirectoryContents(String dir) {
        File file = new File(dir);

        for (File f : Objects.requireNonNull(file.listFiles())) {
            f.delete();
        }
    }
}
