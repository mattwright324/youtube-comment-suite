package io.mattw.youtube.commentsuite;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.util.EurekaProvider;
import io.mattw.youtube.commentsuite.util.Location;
import mattw.youtube.datav3.YouTubeData3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Application Window
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public class FXMLSuite extends Application {

    private static Logger logger = LogManager.getLogger(FXMLSuite.class.getSimpleName());

    private static Location location = new Location<EurekaProvider, EurekaProvider.Location>(
            new EurekaProvider(), EurekaProvider.Location.class);
    private static ConfigFile<ConfigData> config = new ConfigFile<>("commentsuite.json", new ConfigData());
    private static YouTubeData3 youtubeApi, youtubeApiForAccounts;
    private static OAuth2Handler oauth2 = new OAuth2Handler("972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com",
            "QuTdoA-KArupKMWwDrrxOcoS", "urn:ietf:wg:oauth:2.0:oob");
    private static CommentDatabase database;

    public void start(Stage stage) {
        try {

            // System.setProperty("glass.win.uiScale", "100%");
            youtubeApi = new YouTubeData3(config.getDataObject().getYoutubeApiKey());
            youtubeApiForAccounts = new YouTubeData3(config.getDataObject().getYoutubeApiKey());
            database = new CommentDatabase("commentsuite.sqlite3");

            Parent parent = FXMLLoader.load(getClass().getResource("/mattw/youtube/commentsuite/fxml/Main.fxml"));

            Scene scene = new Scene(parent);
            scene.getStylesheets().add("SuiteStyles.css");
            stage.setTitle("YouTube Comment Suite");
            stage.setScene(scene);
            stage.getIcons().add(ImageLoader.YCS_ICON.getImage());
            stage.setOnCloseRequest(we -> {
                logger.debug("Closing - [totalSpentQuota={} units]", youtubeApi.getTotalSpentCost());

                try {
                    database.commit();

                    logger.debug("Closing - Closing DB Connection");
                    database.close();
                } catch (SQLException | IOException e) {
                    logger.error(e);
                }
                logger.debug("Closing - Exiting Application");
                Platform.exit();
                System.exit(0);
            });
            stage.show();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            logger.error(e);
            Platform.exit();
            System.exit(0);
        }
    }

    public static ConfigFile<ConfigData> getConfig() {
        return config;
    }

    public static YouTubeData3 getYoutubeApi() {
        return youtubeApi;
    }

    public static YouTubeData3 getYoutubeApiForAccounts() {
        return youtubeApiForAccounts;
    }

    public static OAuth2Handler getOauth2() {
        return oauth2;
    }

    public static CommentDatabase getDatabase() {
        return database;
    }

    public static Location getLocation() {
        return location;
    }

    public static void main(String[] args) {
        logger.debug("Starting Application");

        /*
         * Setting this system property is a fix for the JavaFX Webview behaving improperly.
         * The 'Tap Yes' authentication when signing in from {@link mattw.youtube.commentsuite.fxml.Settings)
         * would do nothing and the icon would flicker when not set, requiring the user to use SMS
         * authentication instead.
         */
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        launch(args);
    }
}
