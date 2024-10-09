package io.mattw.youtube.commentsuite;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.common.eventbus.EventBus;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Application Window
 *
 */
public class CommentSuite extends Application {

    private static final Logger logger = LogManager.getLogger();

    private static final ConfigFile<ConfigData> config = new ConfigFile<>("commentsuite.json", new ConfigData());
    private static final EventBus eventBus = new EventBus();

    private static CommentDatabase database;
    private static YouTube youTube;
    private static final Properties properties = new Properties();

    public static void main(String[] args) {
        logger.debug("Starting Application");

        /*
         * https://stackoverflow.com/a/24419312/2650847
         */
        System.setProperty("prism.lcdtext", "false");

        launch(args);
    }

    public void start(final Stage stage) {
        try {
            youTube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null)
                    .setApplicationName("youtube-comment-suite")
                    .build();
            database = new CommentDatabase("commentsuite.sqlite3");
            try (InputStream is = CommentSuite.class.getResourceAsStream("/application.properties")) {
                properties.load(is);
            }

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

    public static void postEvent(Object event) {
        eventBus.post(event);
    }

    public static ConfigFile<ConfigData> getConfig() {
        return config;
    }

    public static EventBus getEventBus() {
        return eventBus;
    }

    public static CommentDatabase getDatabase() {
        return database;
    }

    public static YouTube getYouTube() {
        return youTube;
    }

    public static String getYouTubeApiKey() {
        return getConfig().getDataObject().getApiKeyOrDefault();
    }

    public static Properties getProperties() {
        return properties;
    }

}
