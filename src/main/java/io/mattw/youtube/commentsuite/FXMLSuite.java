package io.mattw.youtube.commentsuite;

import com.google.api.services.youtube.YouTube;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.guice.GuiceModule;
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
import java.sql.SQLException;

/**
 * Application Window
 *
 * @author mattwright324
 */
public class FXMLSuite extends Application {

    private static final Logger logger = LogManager.getLogger();

    private static Location location = new Location<IpApiProvider, IpApiProvider.Location>(
            new IpApiProvider(), IpApiProvider.Location.class);
    private static ConfigFile<ConfigData> config = new ConfigFile<>("commentsuite.json", new ConfigData());
    private static OAuth2Handler oauth2 = new OAuth2Handler("972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com",
            "QuTdoA-KArupKMWwDrrxOcoS", "urn:ietf:wg:oauth:2.0:oob");
    //private static CommentDatabase database;

    //private static YouTube youtube;
    private static String youtubeApiKey = "";

    public void start(Stage stage) {
        try {
            //youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null)
            //        .setApplicationName("youtube-comment-suite")
            //        .build();

            // System.setProperty("glass.win.uiScale", "100%");
            youtubeApiKey = config.getDataObject().getYoutubeApiKey();

            //database = new CommentDatabase("commentsuite.sqlite3");

            Injector injector = Guice.createInjector(new GuiceModule());
            FXMLLoader loader = injector.getInstance(FXMLLoader.class);

            String resource = "io/mattw/youtube/commentsuite/fxml/Main.fxml";
            loader.setLocation(getClass().getResource("/" + resource));
            Parent parent = loader.load(ClassLoader.getSystemResourceAsStream(resource));

            Scene scene = new Scene(parent);
            scene.getStylesheets().add("SuiteStyles.css");
            stage.setScene(scene);
            stage.setTitle("YouTube Comment Suite");
            stage.getIcons().add(ImageLoader.YCS_ICON.getImage());
            stage.setOnCloseRequest(we -> {
                /*try {
                    //database.commit();

                    logger.debug("Closing - Closing DB Connection");
                    //database.close();
                } catch (SQLException | IOException e) {
                    logger.error(e);
                }*/
                logger.debug("Closing - Exiting Application");
                Platform.exit();
                System.exit(0);
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
            Platform.exit();
            System.exit(0);
        }
    }

    public static ConfigFile<ConfigData> getConfig() {
        return config;
    }

    @Deprecated
    public static YouTube getYouTube() {
        return null;
    }

    public static String getYouTubeApiKey() {
        return youtubeApiKey;
    }

    public static void setYoutubeApiKey(String apiKey) {
        youtubeApiKey = apiKey;
    }

    public static OAuth2Handler getOauth2() {
        return oauth2;
    }

    @Deprecated
    public static CommentDatabase getDatabase() {
        return null;
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
