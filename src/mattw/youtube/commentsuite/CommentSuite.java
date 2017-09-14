package mattw.youtube.commentsuite;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import mattw.youtube.commentsuite.io.Geolocation;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.SearchList;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

public class CommentSuite extends Application {

    private YouTubeData3 data = new YouTubeData3("AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ");
    private CommentDatabase database;

    private final Image IMG_MANAGE = new Image("/mattw/youtube/commentsuite/img/manage.png");
    private final Image IMG_SEARCH = new Image("/mattw/youtube/commentsuite/img/search.png");
    private final Image IMG_YOUTUBE = new Image("/mattw/youtube/commentsuite/img/youtube.png");

    private StackPane main = new StackPane();
    private StackPane display = new StackPane();

    private boolean doNewSearch = true;
    private StackPane searchYouTube = buildSearchYouTubePane();

    private StackPane manageGroups = buildManageGroupsPane();
    private StackPane searchComments = buildSearchCommentsPane();

    {
        data.setRequestHeader("Referer", "https://github.com/mattwright324/youtube-data-youtubeList");
        try {
            database = new CommentDatabase("commentsuite.sqlite3");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) {
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
        toggleSearch.setId("menuButton");
        toggleSearch.setToggleGroup(tg);
        toggleSearch.setOnAction(ae -> {
            Platform.runLater(() -> {
                img.setImage(IMG_YOUTUBE);
                display.getChildren().clear();
                display.getChildren().add(searchYouTube);
            });
        });

        ToggleButton toggleManage = new ToggleButton("Manage Groups");
        toggleManage.setId("menuButton");
        toggleManage.setToggleGroup(tg);
        toggleManage.setOnAction(ae -> {
            Platform.runLater(() -> {
                img.setImage(IMG_MANAGE);
                display.getChildren().clear();
                display.getChildren().add(manageGroups);
            });
        });

        ToggleButton toggleComments = new ToggleButton("Search Comments");
        toggleComments.setId("menuButton");
        toggleComments.setToggleGroup(tg);
        toggleComments.setOnAction(ae -> {
            Platform.runLater(() -> {
                img.setImage(IMG_SEARCH);
                display.getChildren().clear();
                display.getChildren().add(searchComments);
            });
        });

        HBox bgroup = new HBox();
        bgroup.getChildren().addAll(toggleComments, toggleManage, toggleSearch);

        toggleSearch.fire();

        ComboBox<String> quickAccount =  new ComboBox<>();
        quickAccount.setStyle("-fx-background-color: transparent;");
        quickAccount.getItems().add("No accounts.");
        quickAccount.disableProperty().bind(quickAccount.getSelectionModel().selectedItemProperty().isEqualTo("No accounts."));
        quickAccount.getSelectionModel().select(0);

        Label lbl = new Label();
        lbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lbl, Priority.ALWAYS);

        ImageView settingsImg = new ImageView("/mattw/youtube/commentsuite/img/settings.png");
        settingsImg.setFitWidth(25);
        settingsImg.setFitHeight(25);

        StackPane settings = new StackPane(settingsImg);
        settings.setCursor(Cursor.HAND);
        settings.setOnMouseClicked(me -> {
            System.out.println("Settings!");
        });

        HBox control = new HBox(10);
        control.setPadding(new Insets(0, 10, 0, 10));
        control.setAlignment(Pos.CENTER_LEFT);
        control.setFillHeight(true);
        control.getChildren().addAll(img, bgroup, new Label("Use account: "), quickAccount, lbl, settings);

        Label divider = new Label();
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setMinHeight(4);
        divider.setMaxHeight(4);
        divider.setStyle("-fx-background-color: derive(firebrick, 80%);");

        display.setStyle("-fx-background-color: rgba(255,127,127,0.1)");
        VBox.setVgrow(display, Priority.ALWAYS);

        VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.getChildren().addAll(control, divider, display);

        main.setAlignment(Pos.TOP_CENTER);
        main.getChildren().add(vbox);

        Scene scene = new Scene(main, 900, 550);
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

    public StackPane buildSearchCommentsPane() {


        StackPane stack = new StackPane();
        stack.setPadding(new Insets(10));
        return stack;
    }

    public StackPane buildManageGroupsPane() {

        Label label = new Label("Select a group: ");

        ComboBox<String> groupList = new ComboBox<>();
        groupList.setPrefWidth(200);
        groupList.setMaxWidth(200);
        groupList.setStyle("-fx-background-color: transparent");
        groupList.getItems().addAll("No groups.");
        groupList.setDisable(true);
        groupList.setOnAction(ae -> {

        });
        groupList.getSelectionModel().select(0);

        Button create = new Button("Create Group");

        HBox control = new HBox(10);
        control.setPadding(new Insets(0, 0, 10, 0));
        control.setAlignment(Pos.CENTER);
        control.getChildren().addAll(label, groupList, create);

        Label divider = new Label();
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setMinHeight(4);
        divider.setMaxHeight(4);
        divider.setStyle("-fx-background-color: derive(cornflowerblue, 80%);");

        StackPane manDisplay = new StackPane();
        manDisplay.setStyle("-fx-background-color: rgba(255,255,255,0.1)");
        VBox.setVgrow(manDisplay, Priority.ALWAYS);

        VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().addAll(control, divider, manDisplay);

        StackPane stack = new StackPane(vbox);
        stack.setPadding(new Insets(10));
        return stack;
    }

    public StackPane buildSearchYouTubePane() {
        TextField searchTerms = new TextField();
        searchTerms.setPromptText("Search");
        HBox.setHgrow(searchTerms, Priority.ALWAYS);

        TextField location = new TextField();
        location.setPromptText("40.7058253,-74.1180864");

        ImageView img = new ImageView("/mattw/youtube/commentsuite/img/location.png");
        img.setFitHeight(20);
        img.setFitWidth(20);

        Button grabLoc = new Button();
        grabLoc.setTooltip(new Tooltip("Get your coordinates through ip-geolocation."));
        grabLoc.setGraphic(img);

        HBox hbox = new HBox();
        hbox.getChildren().addAll(location, grabLoc);

        ComboBox<String> distance = new ComboBox<>();
        distance.getItems().addAll("1km", "2km", "5km", "10km", "15km", "20km", "25km", "30km", "50km", "75km", "100km", "200km", "500km", "1000km");
        distance.getSelectionModel().select(0);

        ImageView imgSearch = new ImageView(IMG_SEARCH);
        imgSearch.setFitWidth(20);
        imgSearch.setFitHeight(20);

        Button search = new Button();
        search.setGraphic(imgSearch);

        HBox hbox2 = new HBox();
        hbox2.getChildren().addAll(searchTerms, search);
        HBox.setHgrow(hbox2, Priority.ALWAYS);

        ComboBox<String> orderBy = new ComboBox<>();
        orderBy.getItems().addAll("Relevance", "Date", "Title", "Rating", "Views");
        orderBy.getSelectionModel().select(0);

        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("All types", "Video", "Channel", "Playlist");
        type.getSelectionModel().select(0);

        ComboBox<String> method = new ComboBox<>();
        method.getItems().addAll("Normal", "Location");
        method.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            int index = method.getSelectionModel().getSelectedIndex();
            Platform.runLater(() -> {
                boolean manage = index == 1;
                hbox.setManaged(manage);
                hbox.setVisible(manage);
                distance.setManaged(manage);
                distance.setVisible(manage);
                if(manage) {
                    type.getSelectionModel().select(1);
                    type.setDisable(true);
                } else {
                    type.getSelectionModel().select(0);
                    type.setDisable(false);
                }
            });
        });
        method.getSelectionModel().select(0);

        HBox control = new HBox(10);
        control.getChildren().addAll(method, hbox2, hbox, distance, orderBy, type);
        control.setOnKeyPressed(ke -> {
            if(ke.getCode().equals(KeyCode.ENTER)) search.fire();
        });

        grabLoc.setOnAction(ae ->
            new Thread(() -> {
                Platform.runLater(() -> control.setDisable(true));
                try {
                    Geolocation.Location loc = Geolocation.getMyLocation();
                    double lat = loc.geolocation_data.latitude;
                    double lng = loc.geolocation_data.longitude;
                    Platform.runLater(() -> location.setText(lat+","+lng));
                } catch (Exception ignore) {}
                Platform.runLater(() -> control.setDisable(false));
            }).start()
        );

        MenuItem openBrowser = new MenuItem("Open in Browser");

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(openBrowser);

        ListView<SearchListView> youtubeList = new ListView();
        youtubeList.setContextMenu(menu);
        youtubeList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox.setVgrow(youtubeList, Priority.ALWAYS);

        SimpleStringProperty pageTokenProperty = new SimpleStringProperty("12345");

        openBrowser.setOnAction(ae -> {
            List<SearchList.Item> items = youtubeList.getSelectionModel()
                    .getSelectedItems().stream()
                    .map(slv -> slv.getItem()).collect(Collectors.toList());
            for(SearchList.Item item : items) {
                if(item.id.channelId != null) {
                    openInBrowser("https://youtube.com/channel/"+item.id.channelId);
                } else if(item.id.playlistId != null) {
                    openInBrowser("https://youtube.com/playlist?youtubeList="+item.id.playlistId);
                } else if (item.id.videoId != null) {
                    openInBrowser("https://youtu.be/"+item.id.videoId);
                } else {
                    openInBrowser("https://youtube.com/ycs_error/"+item.id.videoId);
                }
            }
        });

        Button clear = new Button("Clear");
        youtubeList.itemsProperty().addListener((o, ov, nv) -> clear.setDisable(nv.isEmpty()));
        clear.setOnAction(ae -> {
            pageTokenProperty.setValue("12345");
            Platform.runLater(() -> youtubeList.getItems().clear());
        });

        Button addGroup = new Button("Add to Group");
        addGroup.disableProperty().bind(youtubeList.getSelectionModel().selectedIndexProperty().isEqualTo(-1));
        addGroup.setOnAction(ae -> {
            List<SearchList.Item> items = youtubeList.getSelectionModel()
                    .getSelectedItems().stream()
                    .map(slv -> slv.getItem()).collect(Collectors.toList());
            // TODO: Add items to group...
        });

        Button nextPage = new Button("Next Page >");
        nextPage.disableProperty().bind(clear.disableProperty().or(pageTokenProperty.isEqualTo("12345").or(pageTokenProperty.isEqualTo(""))));

        HBox control2 = new HBox(10);
        control2.setAlignment(Pos.CENTER);
        control2.getChildren().addAll(addGroup, clear, nextPage);

        Label results = new Label("");
        results.managedProperty().bind(results.textProperty().isEmpty().not());

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().addAll(control, youtubeList, control2, results);

        search.setOnAction(ae ->
            new Thread(() -> {
                if(doNewSearch || pageTokenProperty.getValue().equals("12345")) {
                    clear.fire();
                    pageTokenProperty.setValue("");
                }
                Platform.runLater(() -> control.setDisable(true));
                try {
                    int index = method.getSelectionModel().getSelectedIndex();
                    String order = orderBy.getSelectionModel().getSelectedItem().toLowerCase();
                    SearchList sl = data.searchList().order(order);
                    if(index == 0) {
                        int tindex = type.getSelectionModel().getSelectedIndex();
                        String searchType = SearchList.TYPE_ALL;
                        if(tindex == 1) {
                            searchType = SearchList.TYPE_VIDEO;
                        } else if(tindex == 2) {
                            searchType = SearchList.TYPE_CHANNEL;
                        } else if(tindex == 3) {
                            searchType = SearchList.TYPE_PLAYLIST;
                        }
                        System.out.println("Token: ["+pageTokenProperty.getValue()+"]");
                        sl = sl.get(SearchList.PART_SNIPPET, URLEncoder.encode(searchTerms.getText(), "UTF-8"), searchType, pageTokenProperty.getValue());
                    } else {
                        sl = sl.getByLocation(SearchList.PART_SNIPPET, URLEncoder.encode(searchTerms.getText(), "UTF-8"), pageTokenProperty.getValue(), location.getText(), distance.getSelectionModel().getSelectedItem());
                    }
                    if(sl.nextPageToken == null) {
                        pageTokenProperty.setValue("12345");
                    } else {
                        pageTokenProperty.setValue(sl.nextPageToken);
                    }
                    int number = youtubeList.getItems().size();
                    for(SearchList.Item item : sl.items) {
                        SearchListView slv = new SearchListView(item, number);
                        Platform.runLater(() -> youtubeList.getItems().add(slv));
                        number++;
                    }
                    final long totalResults = sl.pageInfo.totalResults;
                    Platform.runLater(() -> {
                        control.setDisable(false);
                        results.setText("Showing "+ youtubeList.getItems().size()+" out of "+totalResults);
                    });
                } catch (YouTubeErrorException | IOException e) {
                    e.printStackTrace();
                }
                doNewSearch = true;
                Platform.runLater(() -> control.setDisable(false));
            }).start()
        );

        nextPage.setOnAction(ae -> {
            doNewSearch = false;
            search.fire();
        });

        StackPane stack = new StackPane(vbox);
        stack.setPadding(new Insets(10));
        return stack;
    }

    public static void openInBrowser(String link) {
        link = link.replace(" ", "%20");
        try {
            URL url = new URL(link);
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(url.toURI());
            } else {
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("xdg-open "+url.getPath());
            }
        } catch (Throwable e2) {
            e2.printStackTrace();
        }
    }
}
