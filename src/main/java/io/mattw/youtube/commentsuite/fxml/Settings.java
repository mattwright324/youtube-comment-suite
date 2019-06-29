package io.mattw.youtube.commentsuite.fxml;

import com.google.api.services.youtube.YouTube;
import io.mattw.youtube.commentsuite.*;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
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

/**
 * @author mattwright324
 */
public class Settings implements Initializable {

    private static final Logger logger = LogManager.getLogger();

    private BrowserUtil browserUtil = new BrowserUtil();
    private ConfigFile<ConfigData> config;
    private OAuth2Handler oauth2;
    private CommentDatabase database;
    private YouTube youtube;

    private @FXML Pane settingsPane;

    private @FXML VBox vboxSignIn;
    private @FXML Button btnExitSignIn;

    private @FXML WebView webView;
    private @FXML ProgressIndicator webViewLoading;

    private @FXML VBox vboxSettings;
    private @FXML Button btnClose;
    private @FXML ImageView closeIcon;
    private @FXML CheckBox prefixReply;
    private @FXML CheckBox autoLoadStats;
    private @FXML CheckBox downloadThumbs;
    private @FXML CheckBox customKey;
    private @FXML TextField youtubeApiKey;
    private @FXML Button btnAddAccount;
    private @FXML ListView<SettingsAccountItemView> accountList;

    private @FXML ProgressIndicator cleanProgress;
    private @FXML Button btnClean;
    private @FXML ProgressIndicator resetProgress;
    private @FXML Button btnReset;
    private @FXML ProgressIndicator removeProgress;
    private @FXML Button btnRemoveThumbs;

    private @FXML Hyperlink github;
    private @FXML ImageView githubIcon;
    private @FXML Button btnSave;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialize Settings");

        oauth2 = FXMLSuite.getOauth2();
        config = FXMLSuite.getConfig();
        database = FXMLSuite.getDatabase();
        youtube = FXMLSuite.getYouTube();

        ConfigData configData = config.getDataObject();
        configData.refreshAccounts();
        autoLoadStats.setSelected(configData.getAutoLoadStats());
        prefixReply.setSelected(configData.getPrefixReplies());
        downloadThumbs.setSelected(configData.getArchiveThumbs());
        customKey.setSelected(configData.usingCustomApiKey());
        youtubeApiKey.setText(configData.getYoutubeApiKey());

        CookieManager cm = new CookieManager();
        CookieHandler.setDefault(cm);
        WebEngine webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.titleProperty().addListener((o, ov, nv) -> {
            if(nv != null) {
                logger.debug("YouTubeSignIn [loading-page={}]", nv);
                if(nv.contains("code=")) {
                    configData.refreshAccounts();

                    String code = Stream.of(nv.split("&"))
                            .filter(query -> query.startsWith("Success code="))
                            .collect(Collectors.joining()).substring(13);

                    logger.debug("YouTubeSignIn [returned-code={}]", code);
                    try {
                        OAuth2Tokens tokens = oauth2.getAccessTokens(code);
                        oauth2.setTokens(tokens);

                        YouTubeAccount account = new YouTubeAccount(tokens);

                        configData.addAccount(account);

                        btnExitSignIn.fire();
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error(e);
                    } finally {
                        config.save();
                    }
                } else if(nv.contains("error=")) {
                    logger.debug("YouTubeSignIn Failed [{}]", nv);
                }
            }
        });
        webViewLoading.visibleProperty().bind(webEngine.getLoadWorker().stateProperty().isEqualTo(Worker.State.SUCCEEDED).not());

        configData.accountListChangedProperty().addListener((lcl) -> {
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
        });

        configData.triggerAccountListChanged();

        btnSave.setOnAction(ae -> runLater(() -> btnClose.fire()));

        closeIcon.setImage(ImageLoader.CLOSE.getImage());
        btnClose.setOnAction(ae -> runLater(() -> {
            logger.debug("Saving Settings");
            ConfigData data = config.getDataObject();
            data.setAutoLoadStats(autoLoadStats.isSelected());
            data.setPrefixReplies(prefixReply.isSelected());
            data.setArchiveThumbs(downloadThumbs.isSelected());
            data.setCustomApiKey(customKey.isSelected());
            data.setYoutubeApiKey(youtubeApiKey.getText());

            config.setDataObject(data);
            config.save();

            if(customKey.isSelected()) {
                FXMLSuite.setYoutubeApiKey(data.getYoutubeApiKey());
            } else {
                FXMLSuite.setYoutubeApiKey(data.getDefaultApiKey());
            }

            logger.debug("Closing Settings");
            settingsPane.setManaged(false);
            settingsPane.setVisible(false);
        }));

        youtubeApiKey.disableProperty().bind(customKey.selectedProperty().not());

        githubIcon.setImage(ImageLoader.GITHUB.getImage());

        btnAddAccount.setOnAction(ae -> runLater(() ->  {
            vboxSignIn.setManaged(true);
            vboxSignIn.setVisible(true);
            vboxSettings.setDisable(true);
            webView.getEngine().load(oauth2.getAuthUrl());
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

    private void deleteDirectoryContents(String dir) {
        File file = new File(dir);

        for(File f : file.listFiles()) {
            f.delete();
        }
    }
}
