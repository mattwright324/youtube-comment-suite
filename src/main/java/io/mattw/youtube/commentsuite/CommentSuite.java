package io.mattw.youtube.commentsuite;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.common.eventbus.EventBus;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.oauth2.OAuth2Manager;
import io.mattw.youtube.commentsuite.util.IpApiProvider;
import io.mattw.youtube.commentsuite.util.Location;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

/**
 * Application Window
 *
 */
public class CommentSuite extends Application {

    private static final Logger logger = LogManager.getLogger();

    private static final ConfigFile<ConfigData> config = new ConfigFile<>("commentsuite.json", new ConfigData());
    private static final EventBus eventBus = new EventBus();
    private static final Location<IpApiProvider, IpApiProvider.Location> location = new Location<>(new IpApiProvider(), IpApiProvider.Location.class);

    private static CommentDatabase database;
    private static YouTube youTube;
    private static String youTubeApiKey;
    private static OAuth2Manager oauth2Manager;

    public void start(final Stage stage) {
        try {
            youTube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null)
                    .setApplicationName("youtube-comment-suite")
                    .build();
            youTubeApiKey = config.getDataObject().getYoutubeApiKey();
            database = new CommentDatabase("commentsuite.sqlite3");
            oauth2Manager = new OAuth2Manager();

            final Parent parent = FXMLLoader.load(getClass().getResource("/io/mattw/youtube/commentsuite/fxml/Main.fxml"));

            final Scene scene = new Scene(parent);
            scene.getStylesheets().add("SuiteStyles.css");
            stage.setTitle("YouTube Comment Suite");
            stage.setScene(scene);
            stage.getIcons().add(ImageLoader.YCS_ICON.getImage());
            stage.setOnCloseRequest(we -> {
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
        } catch (IOException | SQLException | GeneralSecurityException e) {
            e.printStackTrace();
            logger.error(e);
            Platform.exit();
            System.exit(0);
        }
    }

    public static ConfigFile<ConfigData> getConfig() {
        return config;
    }

    public static EventBus getEventBus() {
        return eventBus;
    }

    public static Location<IpApiProvider, IpApiProvider.Location> getLocation() {
        return location;
    }

    public static OAuth2Manager getOauth2Manager() {
        return oauth2Manager;
    }

    public static CommentDatabase getDatabase() {
        return database;
    }

    public static YouTube getYouTube() {
        return youTube;
    }

    public static String getYouTubeApiKey() {
        return youTubeApiKey;
    }

    public static void setYouTubeApiKey(String apiKey) {
        youTubeApiKey = apiKey;
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

        /*
         * https://stackoverflow.com/a/24419312/2650847
         */
        System.setProperty("prism.lcdtext", "false");

        launch(args);
    }
}
