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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CommentSuite extends Application {

    private static OAuth2Handler oauth2 = new OAuth2Handler("972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com", "QuTdoA-KArupKMWwDrrxOcoS", "urn:ietf:wg:oauth:2.0:oob");
    private static YouTubeData3 data = new YouTubeData3("AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ");
    private static CommentDatabase database;
    private static Config config = new Config("commentsuite.json");

    static {
        data.setRequestHeader("Referer", "https://github.com/mattwright324/youtube-data-youtubeList");
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
    public static final Image IMG_BLANK_PROFILE = new Image("/mattw/youtube/commentsuite/img/blankProfile.png");

    private StackPane main = new StackPane();
    private StackPane display = new StackPane();

    private boolean doNewSearch = true;
    private StackPane searchYouTube = buildSearchYouTubePane();

    private SimpleStringProperty managerGroupId = new SimpleStringProperty(Group.NO_GROUP);
    private StackPane managerDisplay = new StackPane();
    private StackPane manageGroups = buildManageGroupsPane();

    private SimpleStringProperty commentsGroupId = new SimpleStringProperty(Group.NO_GROUP);
    private StackPane searchComments = buildSearchCommentsPane();
    private StackPane settings = buildSettingsPane();

    public static OAuth2Handler oauth2() { return oauth2; }
    public static YouTubeData3 youtube() { return data; }
    public static CommentDatabase db() { return database; }

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
        title.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        ImageView closeImg = new ImageView("/mattw/youtube/commentsuite/img/close.png");
        closeImg.setFitWidth(22);
        closeImg.setFitHeight(22);

        Button close = new Button();
        close.setStyle("-fx-border-color: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        close.setGraphic(closeImg);
        close.setCursor(Cursor.HAND);
        close.setOnAction(ae -> {
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

        // TODO Add settings for Config

        Label label1 = new Label("General");
        label1.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        CheckBox prefixReplies = new CheckBox("Use prefix +{name} when replying to comments.");
        prefixReplies.setSelected(true);

        CheckBox saveLocally = new CheckBox("Save thumbnails locally (./thumbs/)");
        saveLocally.setDisable(true);

        Label label2 = new Label("YouTube Accounts");
        label2.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        Button signIn = new Button("Add Account");

        ListView<Node> accountList = new ListView<>();
        accountList.setMinHeight(150);
        accountList.setMaxHeight(150);
        VBox.setVgrow(accountList, Priority.ALWAYS);

        Label label4 = new Label("Maintenance");
        label4.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        Button vacuum = new Button("Clean");
        vacuum.setTooltip(new Tooltip("Performs a 'VACUUM' command to reduce database size."));
        vacuum.setOnAction(ae -> {

        });

        Button reset = new Button("Reset");
        reset.setStyle("-fx-base: firebrick;");
        reset.setTooltip(new Tooltip("Completely wipes the database and starts over."));
        reset.setOnAction(ae -> {

        });

        HBox hbox2 = new HBox(10);
        hbox2.getChildren().addAll(vacuum, reset);

        Label label3 = new Label("About");
        label3.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        Label about = new Label("MIT License. Copyright (c) 2017 Matthew Wright.");

        ImageView gitImg = new ImageView("/mattw/youtube/commentsuite/img/github.png");
        gitImg.setFitWidth(20);
        gitImg.setFitHeight(20);

        Hyperlink git = new Hyperlink("mattwright324/youtube-comment-suite");
        git.setMaxHeight(20);
        git.setGraphic(gitImg);
        git.setOnAction(ae -> openInBrowser("https://github.com/mattwright324/youtube-comment-suite"));

        VBox vbox2 = new VBox(10);
        vbox2.setPadding(new Insets(10));
        vbox2.setAlignment(Pos.TOP_LEFT);
        vbox2.getChildren().addAll(label1, prefixReplies, saveLocally, label2, signIn, accountList, label4, hbox2, label3, about, git);

        ScrollPane scroll = new ScrollPane(vbox2);
        scroll.setStyle("-fx-border-color: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button save = new Button("Save and Close");

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(10));
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.getChildren().add(save);

        VBox vbox = new VBox();
        vbox.setMaxWidth(420);
        vbox.setPrefWidth(420);
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.setStyle("-fx-background-color: #eeeeee; -fx-opacity: 1;");
        vbox.getChildren().addAll(control, divider, scroll, hbox);

        Label title2 = new Label("YouTube Account Sign-in");
        title2.setPadding(new Insets(0, 10, 0, 10));
        title2.setMaxHeight(33);
        title2.setMinHeight(33);
        title2.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));
        title2.setMaxWidth(Double.MAX_VALUE);

        Label divider2 = new Label();
        divider2.setMaxWidth(Double.MAX_VALUE);
        divider2.setMinHeight(4);
        divider2.setMaxHeight(4);
        divider2.setStyle("-fx-background-color: derive(green, 80%);");

        WebView wv = new WebView();
        wv.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(wv, Priority.ALWAYS);

        WebEngine engine = wv.getEngine();

        ScrollPane scroll2 = new ScrollPane(wv);
        scroll2.setFitToHeight(true);
        scroll2.setFitToWidth(true);

        Button exit = new Button("Exit");

        HBox ebox = new HBox();
        ebox.setPadding(new Insets(10));
        ebox.setAlignment(Pos.CENTER_RIGHT);
        ebox.getChildren().add(exit);

        VBox vboxWeb = new VBox();
        vboxWeb.setVisible(false);
        vboxWeb.setManaged(false);
        vboxWeb.setMaxWidth(420);
        vboxWeb.setPrefWidth(420);
        vboxWeb.setFillWidth(true);
        vboxWeb.setAlignment(Pos.TOP_CENTER);
        vboxWeb.setStyle("-fx-background-color: #eeeeee; -fx-opacity: 1;");
        vboxWeb.getChildren().addAll(title2, divider2, scroll2, ebox);
        vbox.disableProperty().bind(vboxWeb.managedProperty());

        exit.setOnAction(ae -> {
            Platform.runLater(() -> {
                vboxWeb.setVisible(false);
                vboxWeb.setManaged(false);
            });
        });

        signIn.setOnAction(ae -> {
            Platform.runLater(() -> {
                vboxWeb.setVisible(true);
                vboxWeb.setManaged(true);
                try { engine.load(oauth2.getAuthURL()); } catch (Exception ignored) {}
            });
        });

        save.setOnAction(ae -> {
            // TODO
            close.fire();
        });

        HBox hbox3 = new HBox();
        hbox3.setFillHeight(true);
        hbox3.setAlignment(Pos.CENTER_RIGHT);
        hbox3.getChildren().addAll(vboxWeb, vbox);
        VBox.setVgrow(hbox3, Priority.ALWAYS);

        StackPane stack = new StackPane(hbox3);
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

        YouTubeChannel channel = new YouTubeChannel("channelId", "John Smith", "/mattw/youtube/commentsuite/img/youtube.png", false);
        YouTubeComment comment = new YouTubeComment("commentId", "Hello world", System.currentTimeMillis(), "12345678", "channelId", 15, 7, false, "parentId");
        YouTubeCommentView ycv = new YouTubeCommentView(comment, channel);

        class ListViewEmptyCellFactory extends ListCell<YouTubeCommentView> {
            private double height = 25;
            public ListViewEmptyCellFactory(double height) {
                this.height = height;
            }
            protected void updateItem(YouTubeCommentView item, boolean empty) {
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

        MenuItem copyText = new MenuItem("Copy Comment Text(s)");

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(openProfile, copyText);

        ListView<YouTubeCommentView> commentsList = new ListView<>();
        commentsList.setCellFactory(cf -> new ListViewEmptyCellFactory(70));
        commentsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        commentsList.getItems().addAll(ycv);
        commentsList.setContextMenu(menu);
        commentsList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> System.out.println("Selection change"));
        VBox.setVgrow(commentsList, Priority.ALWAYS);

        openProfile.setOnAction(ae -> {
            List<String> toOpen = new ArrayList<>();
            for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                if(!toOpen.contains(yc.getComment().getChannelId()))
                    toOpen.add(yc.getComment().getChannelId());
            }
            for(String channelId : toOpen) openInBrowser("https://youtube.com/channel/"+channelId);
        });

        copyText.setOnAction(ae -> {
            List<String> toCopy = new ArrayList<>();
            for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                toCopy.add(yc.getComment().getText());
            }
            Clipboards.setClipboard(toCopy.stream().collect(Collectors.joining("\r\n")));
        });

        VBox commentsBox = new VBox(10);
        commentsBox.setPadding(new Insets(10));
        commentsBox.getChildren().addAll(commentsList);
        HBox.setHgrow(commentsBox, Priority.ALWAYS);

        Label label1 = new Label("Select Group");
        label1.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 16));

        ComboBox<GroupItem> groupItem = new ComboBox<>();
        groupItem.setId("control");
        groupItem.setDisable(true);
        groupItem.setMaxWidth(Double.MAX_VALUE);
        groupItem.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> groupItem.setDisable(nv == null || nv.getItemId().equals(GroupItem.NO_ITEMS)));

        ComboBox<Group> group = new ComboBox<>();
        group.setId("control");
        group.setDisable(true);
        group.setMaxWidth(Double.MAX_VALUE);
        group.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            group.setDisable(nv == null || nv.getId().equals(Group.NO_GROUP));
            if(nv != null) commentsGroupId.setValue(nv.getId());
            if(!group.isDisabled()) {
                List<GroupItem> items = database.getGroupItems(nv);
                System.out.println(items);
                GroupItem allItems = new GroupItem(GroupItem.ALL_ITEMS, "All items ("+items.size()+")");
                Platform.runLater(() -> {
                    groupItem.getItems().clear();
                    if(!items.isEmpty()) {
                        if(!items.get(0).getItemId().equals(GroupItem.NO_ITEMS)) {
                            groupItem.getItems().add(allItems);
                        }
                        groupItem.getItems().addAll(items);
                        groupItem.getSelectionModel().select(0);
                    }
                });
            }
        });
        group.itemsProperty().addListener((o, ov, nv) -> {
            if(group.getSelectionModel().getSelectedIndex() == -1 && group.getItems().size() > 0) {
                group.getSelectionModel().select(0);
            }
        });
        group.setItems(database.groupsList);

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
        type.setId("control");
        type.getItems().addAll("Comments and Replies", "Comments Only", "Replies Only");
        type.getSelectionModel().select(0);
        grid.addRow(row++, new Label("Type"), type);

        ChoiceBox<String> orderBy = new ChoiceBox<>();
        orderBy.setId("control");
        orderBy.getItems().addAll("Most Recent", "Least Recent", "Most Likes", "Most Replies", "Longest Comment", "Names (A to Z)", "Comments (A to Z)");
        orderBy.getSelectionModel().select(0);
        grid.addRow(row++, new Label("Order by"), orderBy);

        TextField nameLike = new TextField();
        nameLike.setId("control");
        nameLike.setPromptText("Username contains...");
        grid.addRow(row++, new Label("Name like"), nameLike);

        TextField textLike = new TextField();
        textLike.setId("control");
        textLike.setPromptText("Text contains...");
        grid.addRow(row++, new Label("Text like"), textLike);

        DatePicker dateFrom = new DatePicker();
        dateFrom.setId("control");
        setDatePickerTime(dateFrom, 0);
        grid.addRow(row++, new Label("Date from"), dateFrom);

        DatePicker dateTo = new DatePicker();
        dateTo.setId("control");
        setDatePickerTime(dateTo, System.currentTimeMillis());
        grid.addRow(row++, new Label("Date to"), dateTo);

        Button search = new Button("Find Comments");
        search.setId("control");

        Button clear = new Button("Clear Results");
        clear.setId("control");

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
        Label message = new Label("Create groups of YouTube videos, playlists, and channels.");
        message.setTextFill(Color.LIGHTGRAY);

        Label label = new Label("Select a group: ");

        ComboBox<Group> groupList = new ComboBox<>();
        groupList.setDisable(true);
        groupList.setId("control");
        groupList.setPrefWidth(200);
        groupList.setMaxWidth(200);
        groupList.setStyle("-fx-background-color: transparent");
        groupList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            groupList.setDisable(nv == null || nv.getId().equals(Group.NO_GROUP));
            if(nv != null) managerGroupId.setValue(nv.getId());
            managerDisplay.getChildren().clear();
            if(!groupList.isDisabled()) {
                managerDisplay.getChildren().add(new Label(nv.getId()));
            } else {
                managerDisplay.getChildren().add(message);
            }
        });
        groupList.itemsProperty().addListener((o, ov, nv) -> {
            if(groupList.getSelectionModel().getSelectedIndex() == -1 && groupList.getItems().size() > 0) {
                groupList.getSelectionModel().select(0);
            }
        });
        groupList.setItems(database.groupsList);

        Button create = new Button("Create Group");
        create.setId("control");

        HBox control = new HBox(10);
        control.setPadding(new Insets(0, 0, 10, 0));
        control.setAlignment(Pos.CENTER);
        control.getChildren().addAll(label, groupList, create);

        Label divider = new Label();
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setMinHeight(4);
        divider.setMaxHeight(4);
        divider.setStyle("-fx-background-color: derive(firebrick, 95%);");

        managerDisplay.setPadding(new Insets(10));
        // manDisplay.setStyle("-fx-background-color: #eeeeee; -fx-opacity: 1;");
        managerDisplay.setAlignment(Pos.CENTER);
        managerDisplay.getChildren().add(message);
        VBox.setVgrow(managerDisplay, Priority.ALWAYS);

        VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().addAll(control, divider, managerDisplay);

        StackPane stack = new StackPane(vbox);
        stack.setPadding(new Insets(10,0,0,0));
        return stack;
    }

    public StackPane buildSearchYouTubePane() {
        TextField searchTerms = new TextField();
        searchTerms.setId("control");
        searchTerms.setPromptText("Search");
        HBox.setHgrow(searchTerms, Priority.ALWAYS);

        TextField location = new TextField();
        location.setId("control");
        location.setPromptText("40.7058253,-74.1180864");

        ImageView img = new ImageView("/mattw/youtube/commentsuite/img/location.png");
        img.setFitHeight(18);
        img.setFitWidth(18);

        Button grabLoc = new Button();
        grabLoc.setId("control");
        grabLoc.maxHeightProperty().bind(location.heightProperty());
        grabLoc.setTooltip(new Tooltip("Get your coordinates through ip-geolocation."));
        grabLoc.setGraphic(img);

        HBox hbox = new HBox();
        hbox.setId("control");
        hbox.getChildren().addAll(location, grabLoc);

        ComboBox<String> distance = new ComboBox<>();
        distance.setId("control");
        distance.getItems().addAll("1km", "2km", "5km", "10km", "15km", "20km", "25km", "30km", "50km", "75km", "100km", "200km", "500km", "1000km");
        distance.getSelectionModel().select(3);

        ImageView imgSearch = new ImageView(IMG_SEARCH);
        imgSearch.setFitWidth(18);
        imgSearch.setFitHeight(18);

        Button search = new Button();
        search.setId("control");
        search.setGraphic(imgSearch);

        HBox hbox2 = new HBox();
        hbox2.getChildren().addAll(searchTerms, search);
        HBox.setHgrow(hbox2, Priority.ALWAYS);

        ComboBox<String> orderBy = new ComboBox<>();
        orderBy.setId("control");
        orderBy.getItems().addAll("Relevance", "Date", "Title", "Rating", "Views");
        orderBy.getSelectionModel().select(0);

        ComboBox<String> type = new ComboBox<>();
        type.setId("control");
        type.getItems().addAll("All types", "Video", "Channel", "Playlist");
        type.getSelectionModel().select(0);

        ComboBox<String> method = new ComboBox<>();
        method.setId("control");
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

        ListView<SearchListView> youtubeList = new ListView<>();
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
        addGroup.setTooltip(new Tooltip("Select results to add to a group."));
        addGroup.disableProperty().bind(youtubeList.getSelectionModel().selectedIndexProperty().isEqualTo(-1));
        addGroup.setOnAction(ae -> {
            List<SearchList.Item> items = youtubeList.getSelectionModel()
                    .getSelectedItems().stream()
                    .map(SearchListView::getItem).collect(Collectors.toList());
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
                    Platform.runLater(() -> control.setDisable(false));
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
