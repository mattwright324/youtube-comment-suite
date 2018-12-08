package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import mattw.youtube.commentsuite.*;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.io.BrowserUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author mattwright324
 */
public class SettingsController implements Initializable {

    private static Logger logger = LogManager.getLogger(SettingsController.class.getSimpleName());

    private BrowserUtil browserUtil = new BrowserUtil();
    private ConfigFile<ConfigData> config;
    private OAuth2Handler oauth2;
    private CommentDatabase database;

    private @FXML Pane settingsPane;

    private @FXML VBox vboxSignIn;
    private @FXML Button btnExitSignIn;

    private @FXML WebView webView;
    private @FXML ProgressIndicator webViewLoading;
    private WebEngine webEngine;

    private @FXML VBox vboxSettings;
    private @FXML Button btnClose;
    private @FXML ImageView closeIcon;
    private @FXML CheckBox prefixReply;
    private @FXML CheckBox autoLoadStats;
    private @FXML CheckBox downloadThumbs;
    private @FXML CheckBox customKey;
    private @FXML TextField youtubeApiKey;
    private @FXML Button btnAddAccount;
    private @FXML ListView accountList;

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
        logger.debug("Initialize SettingsController");

        oauth2 = FXMLSuite.getOauth2();
        config = FXMLSuite.getConfig();
        database = FXMLSuite.getDatabase();

        ConfigData cdata = config.getDataObject();
        autoLoadStats.setSelected(cdata.getAutoLoadStats());
        prefixReply.setSelected(cdata.getPrefixReplies());
        downloadThumbs.setSelected(cdata.getArchiveThumbs());
        customKey.setSelected(cdata.usingCustomApiKey());
        youtubeApiKey.setText(cdata.getYoutubeApiKey());

        CookieManager cm = new CookieManager();
        CookieHandler.setDefault(cm);
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.titleProperty().addListener((o, ov, nv) -> {
            if(nv != null) {
                logger.debug(String.format("YouTubeSignIn [loading-page=%s]", nv));
                if(nv.contains("code=")) {
                    String code = nv.substring(nv.indexOf("code=")+5);
                    logger.debug(String.format("YouTubeSignIn [returned-code=%s]", code));
                    try {
                        OAuth2Tokens tokens = oauth2.getAccessTokens(code);
                        oauth2.setTokens(tokens);

                        YouTubeAccount account = new YouTubeAccount(tokens);
                        // TODO: Check if config contains account, if not add.
                        btnExitSignIn.fire();
                    } catch (Exception e) {
                        logger.error(e);
                    }
                } else if(nv.contains("error=")) {
                    logger.debug(String.format("YouTubeSignIn Failed [%s]", nv));
                }
            }
        });
        webViewLoading.visibleProperty().bind(webEngine.getLoadWorker().stateProperty().isEqualTo(Worker.State.SUCCEEDED).not());
        
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
            // TODO: Set account data.
            // data.getAccounts().addAll()
            config.setDataObject(data);
            config.save();

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
            try {
                webView.getEngine().load(oauth2.getAuthURL());
            } catch (UnsupportedEncodingException e) {
                logger.error(e);
            }
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

        github.setOnAction(ae -> browserUtil.open("https://github.com/mattwright324/youtube-comment-suite"));
    }
}
