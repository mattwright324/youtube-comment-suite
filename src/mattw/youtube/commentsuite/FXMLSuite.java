package mattw.youtube.commentsuite;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.io.Geolocation;
import mattw.youtube.datav3.YouTubeData3;
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

    private static Logger logger = LogManager.getLogger(FXMLSuite.class.getSimpleName());

    private static Geolocation geolocation = new Geolocation();
    private static ConfigFile<ConfigData> config = new ConfigFile<>("commentsuite.json", new ConfigData());
    private static YouTubeData3 youtubeApi;
    private static OAuth2Handler oauth2 = new OAuth2Handler("972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com",
            "QuTdoA-KArupKMWwDrrxOcoS", "urn:ietf:wg:oauth:2.0:oob");
    private static CommentDatabase database;

    public void start(Stage stage) {
        try {
            youtubeApi = new YouTubeData3(config.getDataObject().getYoutubeApiKey());
            database = new CommentDatabase("commentsuite.sqlite3");

            Parent parent = FXMLLoader.load(getClass().getResource("/mattw/youtube/commentsuite/fxml/Main.fxml"));

            Scene scene = new Scene(parent);
            scene.getStylesheets().add("/mattw/youtube/commentsuite/fxml/SuiteStyles.css");
            stage.setTitle("FXML CommentSuite");
            stage.setScene(scene);
            stage.getIcons().add(new Image("/mattw/youtube/commentsuite/img/icon.png"));
            stage.setOnCloseRequest(we -> {
                try {
                    database.commit();

                    logger.debug("Closing - Closing DB Connection");
                    database.getConnection().close();
                } catch (SQLException e) {
                    logger.error(e);
                }
                logger.debug("Closing - Exiting Application");
                Platform.exit();
                System.exit(0);
            });
            stage.show();
        } catch (IOException | SQLException e) {
            logger.error(e);
            Platform.exit();
            System.exit(0);
        }
    }

    /* public void stop() {
        Injector.forgetAll();
    }*/

    public static ConfigFile<ConfigData> getConfig() {
        return config;
    }

    public static YouTubeData3 getYoutubeApi() {
        return youtubeApi;
    }

    public static OAuth2Handler getOauth2() {
        return oauth2;
    }

    public static CommentDatabase getDatabase() {
        return database;
    }

    public static Geolocation getGeolocation() {
        return geolocation;
    }

    public static void main(String[] args) {
        logger.debug("Starting Application");

        // *Fix* for issue with WebView not working
        // when the Google 'Tap Yes' authentication method was used.
        // It would do nothing and an icon would flicker --
        // requiring alternative authentication such as entering
        // an SMS code.
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        launch(args);
    }
}
