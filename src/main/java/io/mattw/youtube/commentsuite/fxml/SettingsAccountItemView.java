package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class SettingsAccountItemView extends HBox implements ImageCache {

    private static Logger logger = LogManager.getLogger(SettingsAccountItemView.class.getName());

    private @FXML ImageView accountThumb;
    private @FXML Label accountName;
    private @FXML Button btnRemove;

    private ConfigFile<ConfigData> configFile;
    private ConfigData configData;

    private YouTubeAccount account;

    public SettingsAccountItemView(YouTubeAccount account) {
        this.account = account;

        configFile = FXMLSuite.getConfig();
        configData = configFile.getDataObject();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingsAccountItemView.fxml"));
        loader.setController(this);
        loader.setRoot(this);

        try {
            loader.load();

            accountThumb.setImage(ImageCache.findOrGetImage(account));
            accountName.setText(account.getUsername());

            btnRemove.setOnAction(ae -> configData.removeAccount(account));
        } catch (IOException e) { logger.error(e); }
    }
}
