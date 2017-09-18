package mattw.youtube.commentsuite;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
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
import javafx.stage.Stage;
import mattw.youtube.commentsuite.io.Clipboards;
import mattw.youtube.commentsuite.io.Geolocation;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.SearchList;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CommentSuite extends Application {

    private OAuth2Handler oauth2 = new OAuth2Handler("972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com", "QuTdoA-KArupKMWwDrrxOcoS", "urn:ietf:wg:oauth:2.0:oob");
    private YouTubeData3 data = new YouTubeData3("AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ");
    private CommentDatabase database;

    public static final Image IMG_MANAGE = new Image("/mattw/youtube/commentsuite/img/manage.png");
    public static final Image IMG_SEARCH = new Image("/mattw/youtube/commentsuite/img/search.png");
    public static final Image IMG_YOUTUBE = new Image("/mattw/youtube/commentsuite/img/youtube.png");
    public static final Image IMG_BLANK_PROFILE = new Image("/mattw/youtube/commentsuite/img/blankProfile.png");

    private StackPane main = new StackPane();
    private StackPane display = new StackPane();

    private boolean doNewSearch = true;
    private StackPane searchYouTube = buildSearchYouTubePane();
    private StackPane manageGroups = buildManageGroupsPane();
    private StackPane searchComments = buildSearchCommentsPane();
    private StackPane settings = buildSettingsPane();

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
        control.getChildren().addAll(img, bgroup, new Label("Use account: "), quickAccount, lbl, settingsBtn);

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

    public StackPane buildSettingsPane() {
        Label title = new Label("Settings");
        // title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 14));
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        ImageView closeImg = new ImageView("/mattw/youtube/commentsuite/img/close.png");
        closeImg.setFitWidth(25);
        closeImg.setFitHeight(25);

        StackPane close = new StackPane(closeImg);
        close.setCursor(Cursor.HAND);
        close.setOnMouseClicked(me -> {
            if(main.getChildren().contains(settings)) {
                main.getChildren().remove(settings);
            }
        });

        HBox control = new HBox(10);
        control.setMaxHeight(33);
        control.setMinHeight(33);
        control.setPadding(new Insets(0, 10, 0, 10));
        control.setAlignment(Pos.CENTER_LEFT);
        control.getChildren().addAll(title, close);

        Label divider = new Label();
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setMinHeight(4);
        divider.setMaxHeight(4);
        divider.setStyle("-fx-background-color: derive(firebrick, 80%);");

        VBox vbox2 = new VBox(10);
        vbox2.setPadding(new Insets(10));

        ScrollPane scroll = new ScrollPane(vbox2);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox vbox = new VBox();
        vbox.setMaxWidth(420);
        vbox.setPrefWidth(420);
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.setStyle("-fx-background-color: #eeeeee; -fx-opacity: 1;");
        vbox.getChildren().addAll(control, divider, scroll);

        StackPane stack = new StackPane(vbox);
        stack.setAlignment(Pos.CENTER_RIGHT);
        stack.setStyle("-fx-background-color: rgba(127,127,127,0.4)");
        return stack;
    }

    public StackPane buildSearchCommentsPane() {
        ImageView videoThumb = new ImageView("/mattw/youtube/commentsuite/img/videoPlaceholder.png");
        videoThumb.setFitWidth(300);
        videoThumb.setFitHeight(168);
        videoThumb.setCursor(Cursor.HAND);
        videoThumb.setOnMouseClicked(me -> {

        });

        TextField videoTitle = new TextField("YouTube Comment Suite - Video Placeholder");
        videoTitle.setEditable(false);
        videoTitle.setId("context");
        videoTitle.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

        ImageView videoAuthorThumb = new ImageView(IMG_BLANK_PROFILE);
        videoAuthorThumb.setFitHeight(30);
        videoAuthorThumb.setFitWidth(30);
        videoAuthorThumb.setCursor(Cursor.HAND);
        videoAuthorThumb.setOnMouseClicked(me -> {

        });

        TextField videoAuthor = new TextField("mattwright324");
        videoAuthor.setEditable(false);
        videoAuthor.setId("context");
        videoAuthor.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 14));

        Label views = new Label("8675309 views");

        Label likes = new Label("+8675");
        likes.setTextFill(Color.GREEN);

        Label dislikes = new Label("-309");
        dislikes.setTextFill(Color.RED);

        HBox likeBox = new HBox(5);
        likeBox.getChildren().addAll(likes, dislikes);

        VBox stats = new VBox();
        stats.setAlignment(Pos.TOP_CENTER);
        stats.getChildren().addAll(views, likeBox);

        TextArea videoDesc = new TextArea("Published Nov 18, 1918  This is an example description. You may select this text, the title, and author's name. Right click to copy or select all."
                + "\n\nThe thumbnail and author's picture are clickable to open either the video or channel in your browser."
                + "\n\nComments may be replied to if you are signed in. Commentor names may be clicked to open their channel in browser."
                + "\n\nNote that grabbed comment numbers may be slightly off due to YouTube spam detection and the channel's user and phrase filters.");
        videoDesc.setMaxHeight(Double.MAX_VALUE);
        videoDesc.setEditable(false);
        videoDesc.setWrapText(true);
        VBox.setVgrow(videoDesc, Priority.ALWAYS);

        HBox hbox1 = new HBox(10);
        hbox1.setAlignment(Pos.CENTER_LEFT);
        hbox1.getChildren().addAll(videoAuthorThumb, videoAuthor, stats);

        VBox contextBox = new VBox(10);
        contextBox.setMinWidth(320);
        contextBox.setMaxWidth(320);
        contextBox.setPrefWidth(320);
        contextBox.setAlignment(Pos.TOP_CENTER);
        contextBox.setPadding(new Insets(10));
        contextBox.getChildren().addAll(videoThumb, videoTitle, hbox1, videoDesc);

        ImageView toggleImg = new ImageView("/mattw/youtube/commentsuite/img/toggleContext.png");
        toggleImg.setFitWidth(15);
        toggleImg.setFitHeight(100);

        Label contextToggle = new Label();
        contextToggle.setGraphic(toggleImg);
        contextToggle.setCursor(Cursor.HAND);
        contextToggle.setMaxHeight(Double.MAX_VALUE);
        contextToggle.setStyle("-fx-background-color: linear-gradient(to right, lightgray, transparent)");
        contextToggle.setTooltip(new Tooltip("Click on a comment to view the video context."));
        contextToggle.setOnMouseClicked(me -> {
            boolean disable = !contextBox.isManaged();
            contextBox.setManaged(disable);
            contextBox.setVisible(disable);
        });


        class CommentView extends HBox {
            public CommentView(String name, String profile, String text) {
                super(10);

                ImageView iv = new ImageView(profile);
                iv.setFitHeight(30);
                iv.setFitWidth(30);

                VBox vbox0 = new VBox(5);
                vbox0.setAlignment(Pos.CENTER);
                vbox0.getChildren().addAll(iv, new Label("Comment"));

                Label author = new Label(name);
                author.setMinWidth(0);
                author.setPrefWidth(0);
                author.setMaxWidth(Double.MAX_VALUE);

                Label alltext = new Label(text);
                alltext.setMinWidth(0);
                alltext.setPrefWidth(0);
                alltext.setMaxWidth(Double.MAX_VALUE);

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

                Label date = new Label(sdf.format(new Date(System.currentTimeMillis())));
                date.setTextFill(Color.LIGHTGRAY);

                Hyperlink showMore = new Hyperlink("Show more");
                Hyperlink reply = new Hyperlink("Reply");
                Hyperlink allReplies = new Hyperlink("View Tree");

                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.getChildren().addAll(date, reply, allReplies, showMore);

                VBox vbox1 = new VBox(5);
                vbox1.setMinWidth(0);
                vbox1.setPrefWidth(0);
                vbox1.setMaxWidth(Double.MAX_VALUE);
                vbox1.setAlignment(Pos.CENTER_LEFT);
                vbox1.getChildren().addAll(author, alltext, hbox);
                HBox.setHgrow(vbox1, Priority.ALWAYS);

                setFillHeight(true);
                getChildren().addAll(vbox0, vbox1);
            }
        }


        class ListViewEmptyCellFactory extends ListCell<Node> {
            private double height = 25;
            public ListViewEmptyCellFactory(double height) {
                this.height = height;
            }
            protected void updateItem(Node item, boolean empty) {
                super.updateItem(item, empty);
                if(empty) {
                    setPrefHeight(height);
                    setGraphic(null);
                } else {
                    setPrefHeight(Region.USE_COMPUTED_SIZE);
                    setGraphic(item);
                }
            }
        }

        MenuItem openProfile = new MenuItem("Open Profile(s)");

        MenuItem copyText = new MenuItem("Copy Comment Text");

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(openProfile, copyText);

        ListView<Node> commentsList = new ListView<>();
        commentsList.setCellFactory(cf -> new ListViewEmptyCellFactory(70));
        commentsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        commentsList.getItems().addAll(
                new CommentView("John Doe", "/mattw/youtube/commentsuite/img/blankProfile.png", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
                new CommentView("Sue Smith", "/mattw/youtube/commentsuite/img/blankProfile.png", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
                new CommentView("Pennywise", "/mattw/youtube/commentsuite/img/blankProfile.png", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
        );
        commentsList.setContextMenu(menu);
        commentsList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            System.out.println("Selection change");
        });
        VBox.setVgrow(commentsList, Priority.ALWAYS);

        VBox commentsBox = new VBox(10);
        commentsBox.setPadding(new Insets(10));
        commentsBox.getChildren().addAll(commentsList);
        HBox.setHgrow(commentsBox, Priority.ALWAYS);

        Label label1 = new Label("Select Group");
        label1.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 16));

        ChoiceBox<String> group = new ChoiceBox<>();
        group.setMaxWidth(Double.MAX_VALUE);

        ChoiceBox<String> groupItem = new ChoiceBox<>();
        groupItem.setMaxWidth(Double.MAX_VALUE);

        Label label2 = new Label("Restrict Results");
        label2.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 16));

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        int row = 1;

        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setFillWidth(true);
        grid.getColumnConstraints().addAll(col1, col2);

        ChoiceBox<String> type = new ChoiceBox<>();
        type.getItems().addAll("Comments and Replies", "Comments Only", "Replies Only");
        type.getSelectionModel().select(0);
        grid.addRow(row++, new Label("Type"), type);

        ChoiceBox<String> orderBy = new ChoiceBox<>();
        orderBy.getItems().addAll("Most Recent", "Least Recent", "Most Likes", "Most Replies", "Longest Comment", "Names (A to Z)", "Comments (A to Z)");
        orderBy.getSelectionModel().select(0);
        grid.addRow(row++, new Label("Order by"), orderBy);

        TextField nameLike = new TextField();
        nameLike.setPromptText("Username contains...");
        grid.addRow(row++, new Label("Name like"), nameLike);

        TextField textLike = new TextField();
        textLike.setPromptText("Text contains...");
        grid.addRow(row++, new Label("Text like"), textLike);

        DatePicker dateFrom = new DatePicker();
        setDatePickerTime(dateFrom, 0);
        grid.addRow(row++, new Label("Date from"), dateFrom);

        DatePicker dateTo = new DatePicker();
        setDatePickerTime(dateTo, System.currentTimeMillis());
        grid.addRow(row++, new Label("Date to"), dateTo);

        Button search = new Button("Find Comments");

        Button clear = new Button("Clear Results");

        HBox hbox2 = new HBox(10);
        hbox2.setAlignment(Pos.CENTER);
        hbox2.getChildren().addAll(search, clear);

        VBox searchBox = new VBox(10);
        searchBox.setPadding(new Insets(10, 10, 10, 0));
        searchBox.setAlignment(Pos.TOP_CENTER);
        searchBox.setMinWidth(320);
        searchBox.setMaxWidth(320);
        searchBox.setPrefWidth(320);
        searchBox.getChildren().addAll(label1, group, groupItem, label2, grid, hbox2);

        HBox hbox = new HBox();
        hbox.setFillHeight(true);
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().addAll(contextBox, contextToggle, commentsBox, searchBox);

        StackPane stack = new StackPane(hbox);
        stack.setPadding(new Insets(0));
        return stack;
    }

    private void setDatePickerTime(DatePicker picker, long time) {
        LocalDate date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();
        picker.setValue(date);
    }

    private Date getDatePickerDate(DatePicker picker, boolean midnightTonight) {
        LocalDate localDate = picker.getValue();
        Instant instant = Instant.from(localDate.atStartOfDay(ZoneId.systemDefault()));

        Calendar cal = new GregorianCalendar();
        cal.setTime(Date.from(instant));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if(midnightTonight) {
            cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR)+1);
        }
        System.out.println(cal.getTime().toString());
        return cal.getTime();
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
        divider.setStyle("-fx-background-color: derive(firebrick, 95%);");

        Label message = new Label("Create groups of YouTube videos, playlists, and channels.");
        message.setTextFill(Color.LIGHTGRAY);

        StackPane manDisplay = new StackPane(message);
        manDisplay.setPadding(new Insets(10));
        // manDisplay.setStyle("-fx-background-color: #eeeeee; -fx-opacity: 1;");
        manDisplay.setAlignment(Pos.CENTER);
        VBox.setVgrow(manDisplay, Priority.ALWAYS);

        VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().addAll(control, divider, manDisplay);

        StackPane stack = new StackPane(vbox);
        stack.setPadding(new Insets(10,0,0,0));
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
        distance.getSelectionModel().select(3);

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

        MenuItem copyLinks = new MenuItem("Copy Link(s)");

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(openBrowser, copyLinks);

        ListView<SearchListView> youtubeList = new ListView();
        youtubeList.setContextMenu(menu);
        youtubeList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox.setVgrow(youtubeList, Priority.ALWAYS);

        SimpleStringProperty pageTokenProperty = new SimpleStringProperty("12345");

        openBrowser.setOnAction(ae -> {
            List<SearchListView> items = youtubeList.getSelectionModel().getSelectedItems();
            for(SearchListView item : items) {
                openInBrowser(item.getYouTubeLink());
            }
        });

        copyLinks.setOnAction(ae -> {
            List<SearchListView> items = youtubeList.getSelectionModel().getSelectedItems();
            List<String> links = new ArrayList<>();
            for(SearchListView item : items) {
                links.add(item.getYouTubeLink());
            }
            Clipboards.setClipboard(links.stream().collect(Collectors.joining("\r\n")));
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
                    final long totalResults = sl.pageInfo.totalResults;
                    int number = youtubeList.getItems().size();
                    for(SearchList.Item item : sl.items) {
                        SearchListView slv = new SearchListView(item, number);
                        Platform.runLater(() -> {
                            youtubeList.getItems().add(slv);
                            results.setText("Showing "+ youtubeList.getItems().size()+" out of "+totalResults);
                        });
                        number++;
                    }
                    Platform.runLater(() -> {
                        control.setDisable(false);
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
