package mattw.youtube.commentsuite;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import mattw.youtube.commentsuite.db.*;
import mattw.youtube.commentsuite.io.Clipboards;
import mattw.youtube.commentsuite.io.Geolocation;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.CommentsList;
import mattw.youtube.datav3.resources.SearchList;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CommentSuite extends Application {

    private static final YouTubeData3 data = new YouTubeData3("AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ");
    private static final Config config = new Config("commentsuite.json");
    private static CommentDatabase database;
    private static CommentSuite instance;
    private final OAuth2Handler oauth2 = new OAuth2Handler("972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com", "QuTdoA-KArupKMWwDrrxOcoS", "urn:ietf:wg:oauth:2.0:oob");

    static {
        config.load();
        try {
            database = new CommentDatabase("commentsuite.sqlite3");
            database.refreshGroups();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final Image IMG_MANAGE = new Image("/mattw/youtube/commentsuite/img/manage.png");
    public static final Image IMG_SEARCH = new Image("/mattw/youtube/commentsuite/img/search.png");
    public static final Image IMG_YOUTUBE = new Image("/mattw/youtube/commentsuite/img/youtube.png");

    private StackPane main = new StackPane();
    private StackPane display = new StackPane();

    private SearchYouTubePane searchYouTube = new SearchYouTubePane(data);
    private ManageGroupsPane manageGroups = new ManageGroupsPane();
    protected SearchCommentsPane searchComments = new SearchCommentsPane(oauth2);
    private SettingsPane settings = new SettingsPane(main, oauth2);

    public static Config config() { return config; }
    public static YouTubeData3 youtube() { return data; }
    public static CommentDatabase db() { return database; }
    public static CommentSuite instance() { return instance; }

    public static void main(String[] args) { launch(args); }

    public void start(Stage stage) {
        instance = this;

        ImageView img = new ImageView("/mattw/youtube/commentsuite/img/icon.png");
        img.setFitWidth(25);
        img.setFitHeight(25);

        ToggleGroup tg = new ToggleGroup();
        tg.getToggles().addListener((ListChangeListener<Toggle>) c -> {
            while(c.next()) {
                for(final Toggle addedToggle : c.getAddedSubList()) {
                    ((ToggleButton) addedToggle).addEventFilter(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
                        if (addedToggle.equals(tg.getSelectedToggle()))
                            mouseEvent.consume();
                    });
                }
            }
        });

        ToggleButton toggleSearch = new ToggleButton("Search YouTube");
        toggleSearch.setMaxHeight(33);
        toggleSearch.setMinHeight(33);
        toggleSearch.setId("menuButton");
        toggleSearch.setToggleGroup(tg);
        toggleSearch.setOnAction(ae -> Platform.runLater(() -> {
            img.setImage(IMG_YOUTUBE);
            display.getChildren().clear();
            display.getChildren().add(searchYouTube);
        }));

        ToggleButton toggleManage = new ToggleButton("Manage Groups");
        toggleManage.setMaxHeight(33);
        toggleManage.setMinHeight(33);
        toggleManage.setId("menuButton");
        toggleManage.setToggleGroup(tg);
        toggleManage.setOnAction(ae -> Platform.runLater(() -> {
            img.setImage(IMG_MANAGE);
            display.getChildren().clear();
            display.getChildren().add(manageGroups);
        }));

        ToggleButton toggleComments = new ToggleButton("Search Comments");
        toggleComments.setMaxHeight(33);
        toggleComments.setMinHeight(33);
        toggleComments.setId("menuButton");
        toggleComments.setToggleGroup(tg);
        toggleComments.setOnAction(ae -> Platform.runLater(() -> {
            img.setImage(IMG_SEARCH);
            display.getChildren().clear();
            display.getChildren().add(searchComments);
        }));

        HBox bgroup = new HBox();
        bgroup.getChildren().addAll(toggleComments, toggleManage, toggleSearch);

        toggleManage.fire();

        Label lbl = new Label();
        lbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lbl, Priority.ALWAYS);

        ImageView settingsImg = new ImageView("/mattw/youtube/commentsuite/img/settings.png");
        settingsImg.setFitWidth(25);
        settingsImg.setFitHeight(25);

        StackPane settingsBtn = new StackPane(settingsImg);
        settingsBtn.setCursor(Cursor.HAND);
        settingsBtn.setOnMouseClicked(me -> {
            if(!main.getChildren().contains(settings)) {
                main.getChildren().add(settings);
            }
        });

        HBox control = new HBox(10);
        control.setMaxHeight(33);
        control.setMinHeight(33);
        control.setPadding(new Insets(0, 10, 0, 10));
        control.setAlignment(Pos.CENTER_LEFT);
        control.setFillHeight(true);
        control.getChildren().addAll(img, bgroup, lbl, settingsBtn);

        Label divider = new Label();
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setMinHeight(4);
        divider.setMaxHeight(4);
        divider.setStyle("-fx-background-color: derive(firebrick, 80%);");

        VBox.setVgrow(display, Priority.ALWAYS);

        VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.getChildren().addAll(control, divider, display);

        main.setAlignment(Pos.TOP_CENTER);
        main.getChildren().add(vbox);

        Scene scene = new Scene(main, 980, 550);
        scene.getStylesheets().add(getClass().getResource("/mattw/youtube/commentsuite/commentsuite.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("YouTube Comment Suite");
        stage.getIcons().add(new Image("/mattw/youtube/commentsuite/img/icon.png"));
        stage.setOnCloseRequest(we -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }
}
