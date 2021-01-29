package io.mattw.youtube.commentsuite.fxml;

import com.google.common.eventbus.Subscribe;
import io.mattw.youtube.commentsuite.*;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.events.AccountAddEvent;
import io.mattw.youtube.commentsuite.events.AccountDeleteEvent;
import io.mattw.youtube.commentsuite.oauth2.OAuth2Manager;
import io.mattw.youtube.commentsuite.util.BrowserUtil;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;

public class Settings implements Initializable {

    private static final Logger logger = LogManager.getLogger();

    private BrowserUtil browserUtil = new BrowserUtil();
    private ConfigFile<ConfigData> config;
    private ConfigData configData;
    private OAuth2Manager oAuth2Manager;
    private CommentDatabase database;

    @FXML private Pane settingsPane;

    @FXML private VBox vboxSignIn;
    @FXML private Button btnExitSignIn;

    @FXML private WebView webView;
    @FXML private ProgressIndicator webViewLoading;

    @FXML private VBox vboxSettings;
    @FXML private Button btnClose;
    @FXML private ImageView closeIcon;
    @FXML private CheckBox prefixReply;
    @FXML private CheckBox autoLoadStats;
    @FXML private CheckBox downloadThumbs;
    @FXML private CheckBox customKey;
    @FXML private CheckBox filterDuplicatesOnCopy;
    @FXML private TextField youtubeApiKey;
    @FXML private Button btnAddAccount;
    @FXML private ListView<SettingsAccountItemView> accountList;
    @FXML private CheckBox grabHeldForReview;

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
        oAuth2Manager = CommentSuite.getOauth2Manager();
        config = CommentSuite.getConfig();
        configData = config.getDataObject();
        CommentSuite.getEventBus().register(this);

        autoLoadStats.setSelected(configData.isAutoLoadStats());
        prefixReply.setSelected(configData.isPrefixReplies());
        downloadThumbs.setSelected(configData.isArchiveThumbs());
        customKey.setSelected(configData.isCustomApiKey());
        youtubeApiKey.setText(configData.getYoutubeApiKey());
        filterDuplicatesOnCopy.setSelected(configData.isFilterDuplicatesOnCopy());
        grabHeldForReview.setSelected(configData.isGrabHeldForReview());

        CookieHandler.setDefault(new CookieManager());

        WebEngine webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.titleProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                logger.debug("YouTubeSignIn [loading-page={}]", nv);
                if (nv.contains("code=")) {
                    //configData.refreshAccounts();

                    final String authorizationCode = Stream.of(nv.split("&"))
                            .filter(query -> query.startsWith("Success code="))
                            .collect(Collectors.joining())
                            .substring(13);

                    logger.debug("YouTubeSignIn [returned-code={}]", authorizationCode);

                    try {
                        oAuth2Manager.addAccount(authorizationCode);

                        btnExitSignIn.fire();
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error(e);
                    } finally {
                        config.save();
                    }
                } else if (nv.contains("error=")) {
                    logger.debug("YouTubeSignIn Failed [{}]", nv);
                }
            }
        });
        webViewLoading.visibleProperty().bind(webEngine.getLoadWorker().stateProperty().isEqualTo(Worker.State.SUCCEEDED).not());

        reloadAccountList();

        btnSave.setOnAction(ae -> runLater(() -> btnClose.fire()));

        closeIcon.setImage(ImageLoader.CLOSE.getImage());
        btnClose.setOnAction(ae -> runLater(() -> {
            logger.debug("Saving Settings");

            ConfigData data = config.getDataObject();
            data.setArchiveThumbs(downloadThumbs.isSelected());
            data.setAutoLoadStats(autoLoadStats.isSelected());
            data.setCustomApiKey(customKey.isSelected());
            data.setFilterDuplicatesOnCopy(filterDuplicatesOnCopy.isSelected());
            data.setGrabHeldForReview(grabHeldForReview.isSelected());
            data.setPrefixReplies(prefixReply.isSelected());
            data.setYoutubeApiKey(youtubeApiKey.getText());

            config.setDataObject(data);
            config.save();

            if (customKey.isSelected()) {
                CommentSuite.setYouTubeApiKey(data.getYoutubeApiKey());
            } else {
                CommentSuite.setYouTubeApiKey(ConfigData.DEFAULT_API_KEY);
            }

            logger.debug("Closing Settings");
            settingsPane.setManaged(false);
            settingsPane.setVisible(false);
        }));

        youtubeApiKey.disableProperty().bind(customKey.selectedProperty().not());

        githubIcon.setImage(ImageLoader.GITHUB.getImage());

        btnAddAccount.setOnAction(ae -> runLater(() -> {
            vboxSignIn.setManaged(true);
            vboxSignIn.setVisible(true);
            vboxSettings.setDisable(true);
            webView.getEngine().load(OAuth2Manager.WEB_LOGIN_URL);
        }));

        btnExitSignIn.setOnAction(ae -> runLater(() -> {
            vboxSignIn.setManaged(false);
            vboxSignIn.setVisible(false);
            vboxSettings.setDisable(false);
        }));

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

    private void reloadAccountList() {
        config.save();

        List<SettingsAccountItemView> listItems = configData.getAccounts()
                .stream()
                .filter(Objects::nonNull)
                .filter(account -> account.getChannelId() != null && account.getThumbUrl() != null
                        && account.getUsername() != null)
                .map(SettingsAccountItemView::new)
                .collect(Collectors.toList());

        runLater(() -> {
            accountList.getItems().clear();
            accountList.getItems().addAll(listItems);
        });
    }

    @Subscribe
    public void accountAddEvent(final AccountAddEvent accountAddEvent) {
        reloadAccountList();
    }

    @Subscribe
    public void accountDeleteEvent(final AccountDeleteEvent accountDeleteEvent) {
        reloadAccountList();
    }

    private void deleteDirectoryContents(String dir) {
        File file = new File(dir);

        for (File f : Objects.requireNonNull(file.listFiles())) {
            f.delete();
        }
    }
}
