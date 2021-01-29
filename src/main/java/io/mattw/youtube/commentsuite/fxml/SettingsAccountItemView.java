package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.*;
import io.mattw.youtube.commentsuite.oauth2.OAuth2Manager;
import io.mattw.youtube.commentsuite.oauth2.YouTubeAccount;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SettingsAccountItemView extends HBox implements ImageCache {

    private static final Logger logger = LogManager.getLogger();

    @FXML private ImageView accountThumb;
    @FXML private Label accountName;
    @FXML private Button btnRemove;

    private final ConfigFile<ConfigData> configFile;
    private final ConfigData configData;
    private final OAuth2Manager oAuth2Manager;

    private final YouTubeAccount account;

    public SettingsAccountItemView(final YouTubeAccount account) {
        this.account = account;

        configFile = CommentSuite.getConfig();
        configData = configFile.getDataObject();
        oAuth2Manager = CommentSuite.getOauth2Manager();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingsAccountItemView.fxml"));
        loader.setController(this);
        loader.setRoot(this);

        try {
            loader.load();

            accountThumb.setImage(ImageCache.findOrGetImage(account));
            accountName.setText(account.getUsername());

            btnRemove.setOnAction(ae -> new Thread(() -> {
                try {
                    oAuth2Manager.revokeAccessTo(account);
                    configData.removeAccount(account);
                } catch (IOException e) {
                    logger.error("Problem while remove account");
                }
            }).start());
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
