package mattw.youtube.commentsuite;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Application Window
 *
 * @author mattwright324
 */
public class FXMLSuite extends Application {

    public void start(Stage stage) throws IOException {
        Pane root = FXMLLoader.load(getClass().getResource("/mattw/youtube/commentsuite/fxml/Main.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/mattw/youtube/commentsuite/fxml/SuiteStyles.css");
        stage.setTitle("FXML CommentSuite");
        stage.setScene(scene);
        stage.getIcons().add(new Image("/mattw/youtube/commentsuite/img/icon.png"));
        stage.setOnCloseRequest(we -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
