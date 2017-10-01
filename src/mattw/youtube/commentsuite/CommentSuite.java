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
import mattw.youtube.commentsuite.io.Clipboards;
import mattw.youtube.commentsuite.io.Geolocation;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.CommentsList;
import mattw.youtube.datav3.resources.SearchList;

import java.awt.*;
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

    private static final String RELEASE = "v1.3.0";
    private static final OAuth2Handler oauth2 = new OAuth2Handler("972416191049-htqcmg31u2t7hbd1ncen2e2jsg68cnqn.apps.googleusercontent.com", "QuTdoA-KArupKMWwDrrxOcoS", "urn:ietf:wg:oauth:2.0:oob");
    private static final YouTubeData3 data = new YouTubeData3("AIzaSyD9SzQFnmOn08ESZC-7gIhnHWVn0asfrKQ");
    private static final Config config = new Config("commentsuite.json");
    private static CommentDatabase database;
    private static CommentSuite instance;

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
    public static final Image IMG_BLANK_PROFILE = new Image("/mattw/youtube/commentsuite/img/blankProfile.png");

    private StackPane main = new StackPane();
    private StackPane display = new StackPane();

    private boolean doNewSearch = true;
    private ChangeListener<Number> cl;
    private StackPane searchYouTube = buildSearchYouTubePane();

    private SimpleStringProperty managerGroupId = new SimpleStringProperty(Group.NO_GROUP);
    private Map<String,GroupManageView> managerMap = new HashMap<>();
    private StackPane managerDisplay = new StackPane();
    private StackPane manageGroups = buildManageGroupsPane();

    private CommentDatabase.CommentQuery query;
    private ObservableList<YouTubeCommentView> originalComments = FXCollections.observableArrayList();
    private ObservableList<YouTubeCommentView> treeComments = FXCollections.observableArrayList();
    private ListView<YouTubeCommentView> commentsList = new ListView<>();
    private YouTubeCommentView actionComment = null;
    private Button showMore = new Button(), reply = new Button(), viewTree = new Button();
    private SimpleStringProperty commentsGroupId = new SimpleStringProperty(Group.NO_GROUP);
    private SimpleStringProperty selectedVideoId = new SimpleStringProperty("");
    private YouTubeVideo selectedVideo = null;
    private StackPane searchComments = buildSearchCommentsPane();
    private StackPane settings = buildSettingsPane();

    public static OAuth2Handler oauth2() { return oauth2; }
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

        Label label1 = new Label("General");
        label1.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        CheckBox prefixReplies = new CheckBox("(Search Comments) Prefix +{name} when replying to comments.");
        prefixReplies.setSelected(config.prefixReplies());

        CheckBox loadStats = new CheckBox("(Manage Groups) Auto-load stats while managing a group.");
        loadStats.setSelected(config.autoLoadStats());

        Label label2 = new Label("YouTube Accounts");
        label2.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        Button signIn = new Button("Add Account");

        ListView<YouTubeAccountView> accountList = new ListView<>();
        accountList.setId("listView");
        accountList.setMinHeight(150);
        accountList.setMaxHeight(150);
        accountList.setCellFactory(cf -> new ListViewEmptyCellFactory(40));
        accountList.getItems().addListener((ListChangeListener<YouTubeAccountView>) c -> {
            while(c.next()) {
                if(c.wasAdded() || c.wasRemoved()) {
                    for (YouTubeAccountView removed : c.getRemoved()) {
                        removed.signedOutProperty().unbind();
                    }
                    for (YouTubeAccountView added : c.getAddedSubList()) {
                        added.signedOutProperty().addListener((o1, ov1, nv1) -> {
                            accountList.getItems().remove(added);
                            config.getAccounts().remove(added.getAccount());
                            config.save();
                            System.out.println("Signed out of "+added.getAccount().getUsername());
                        });
                    }
                }
            }
        });
        VBox.setVgrow(accountList, Priority.ALWAYS);
        accountList.getItems().addAll(config.getAccounts().stream().map(YouTubeAccountView::new).collect(Collectors.toList()));

        Label label4 = new Label("Maintenance");
        label4.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        Button vacuum = new Button("Clean");
        vacuum.setTooltip(new Tooltip("Performs a 'VACUUM' command to reduce database size."));

        Button reset = new Button("Reset");
        reset.setStyle("-fx-base: firebrick;");
        reset.setTooltip(new Tooltip("Completely wipes the database and starts over."));
        HBox hbox2 = new HBox(10);
        hbox2.setAlignment(Pos.CENTER_LEFT);
        hbox2.getChildren().addAll(new Label("Database"), vacuum, reset);

        Label label3 = new Label("About");
        label3.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        Label release = new Label("This release version: "+RELEASE);

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
        vbox2.getChildren().addAll(label1, prefixReplies, loadStats, label2, signIn, accountList, label4, hbox2, label3, about, release, git);

        ScrollPane scroll = new ScrollPane(vbox2);
        scroll.setStyle("-fx-border-color: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button save = new Button("Save and Close");
        save.setStyle("-fx-base: cornflowerblue");

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(10));
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.getChildren().add(save);

        VBox vbox = new VBox();
        vbox.setMaxWidth(420);
        vbox.setPrefWidth(420);
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.setId("overlayMenu");
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

        Button exit = new Button("Exit");

        CookieManager cm = new CookieManager();
        CookieHandler.setDefault(cm);
        WebView wv = new WebView();
        WebEngine engine = wv.getEngine();
        engine.setJavaScriptEnabled(true);
        StackPane wvStack = new StackPane(wv);
        engine.titleProperty().addListener((o, ov, nv) -> {
            System.out.println("Load page: "+nv);
            if(nv != null) {
                if(nv.contains("code=")) {
                    String code = nv.substring(13, nv.length());
                    try {
                        OAuth2Tokens tokens = oauth2.getAccessTokens(code);
                        oauth2.setTokens(tokens);
                        YouTubeAccount account = new YouTubeAccount(tokens);
                        if(config.getAccounts().stream().noneMatch(acc -> acc.getChannelId().equals(account.channelId))) {
                            config.getAccounts().add(account);
                            config.save();
                            Platform.runLater(() -> {
                                accountList.getItems().clear();
                                accountList.getItems().addAll(config.getAccounts().stream().map(YouTubeAccountView::new).collect(Collectors.toList()));
                            });
                            exit.fire();
                        } else {
                            System.out.println("Account Already Signed-in: "+account.getUsername());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if(nv.contains("error=")) {
                    System.err.println(nv);
                }
            }
        });

        ScrollPane scroll2 = new ScrollPane(wvStack);
        scroll2.setFitToHeight(true);
        scroll2.setFitToWidth(true);

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
        vboxWeb.setId("overlayMenu");
        vboxWeb.getChildren().addAll(title2, divider2, scroll2, ebox);
        vbox.disableProperty().bind(vboxWeb.managedProperty());

        exit.setOnAction(ae -> Platform.runLater(() -> {
            vboxWeb.setVisible(false);
            vboxWeb.setManaged(false);
        }));

        signIn.setOnAction(ae -> Platform.runLater(() -> {
            vboxWeb.setVisible(true);
            vboxWeb.setManaged(true);
            try {
                cm.getCookieStore().removeAll();
                engine.load(oauth2.getAuthURL());
            } catch (Exception ignored) {}
        }));

        save.setOnAction(ae -> {
            config.setPrefixReplies(prefixReplies.isSelected());
            config.setAutoLoadStats(loadStats.isSelected());
            config.save();
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

        vacuum.setOnAction(ae -> new Thread(() -> {
            Label label = new Label("Performing VACUUM");

            ProgressIndicator prog = new ProgressIndicator();
            prog.setMaxWidth(25);
            prog.setMaxHeight(25);

            VBox vbox0 = new VBox(10);
            vbox0.setId("overlayMenu");
            vbox0.setPadding(new Insets(25));
            vbox0.getChildren().addAll(label, prog);

            StackPane overlay = new StackPane(vbox0);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");

            Platform.runLater(() -> stack.getChildren().add(overlay));
            try { database.vacuum(); } catch (SQLException ignored) {}
            Platform.runLater(() -> stack.getChildren().remove(overlay));
        }).start());

        reset.setOnAction(ae -> new Thread(() -> {
            Label label = new Label("Resetting Database");

            ProgressIndicator prog = new ProgressIndicator();
            prog.setMaxWidth(25);
            prog.setMaxHeight(25);

            VBox vbox0 = new VBox(10);
            vbox0.setAlignment(Pos.CENTER);
            vbox0.setMaxHeight(0);
            vbox0.setMaxWidth(0);
            vbox0.setId("overlayMenu");
            vbox0.setPadding(new Insets(25));
            vbox0.getChildren().addAll(label, prog);

            StackPane overlay = new StackPane(vbox0);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");

            Platform.runLater(() -> stack.getChildren().add(overlay));
            try { database.reset(); } catch (SQLException ignored) { ignored.printStackTrace(); }
            Platform.runLater(() -> stack.getChildren().remove(overlay));
        }).start());

        return stack;
    }

    public StackPane buildSearchCommentsPane() {
        ImageView videoThumb = new ImageView("/mattw/youtube/commentsuite/img/videoPlaceholder.png");
        videoThumb.setFitWidth(300);
        videoThumb.setFitHeight(168);
        videoThumb.setCursor(Cursor.HAND);
        videoThumb.setOnMouseClicked(me -> {
            if(selectedVideo != null) {
                openInBrowser(selectedVideo.getYouTubeLink());
            }
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
            if(selectedVideo != null) {
                openInBrowser(selectedVideo.getChannel().getYouTubeLink());
            }
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

        ImageView browser = new ImageView("/mattw/youtube/commentsuite/img/browser.png");
        browser.setFitHeight(20);
        browser.setFitWidth(20);

        ImageView thumb = new ImageView("/mattw/youtube/commentsuite/img/thumbnail.png");
        thumb.setFitHeight(20);
        thumb.setFitWidth(20);

        MenuItem openBrowser = new MenuItem("Open in Browser");
        openBrowser.setGraphic(browser);
        MenuItem loadThumb = new MenuItem("Load Thumbnail(s)");
        loadThumb.setGraphic(thumb);
        MenuItem copyName = new MenuItem("Copy Name(s)");
        MenuItem copyText = new MenuItem("Copy Text(s)");
        MenuItem copyChannelLink = new MenuItem("Copy Channel Link(s)");
        MenuItem copyVideoLink = new MenuItem("Copy Video Link(s)");
        MenuItem copyCommentLink = new MenuItem("Copy Comment Link(s)");

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(openBrowser, loadThumb, copyName, copyText, copyChannelLink, copyVideoLink, copyCommentLink);

        commentsList.setItems(originalComments);
        commentsList.setId("listView");
        commentsList.setCellFactory(cf -> new ListViewEmptyCellFactory(96));
        commentsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        commentsList.setContextMenu(menu);
        commentsList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if(nv != null) { Platform.runLater(() -> selectedVideoId.setValue(nv.getComment().getVideoId())); }
        });
        VBox.setVgrow(commentsList, Priority.ALWAYS);

        selectedVideoId.addListener((o, ov, nv) -> new Thread(() -> {
            try {
                this.selectedVideo = database.getVideo(nv);
                if(selectedVideo != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                    Image vt = selectedVideo.getThumbnail();
                    Image vat = selectedVideo.getChannel().getThumbnail();
                    Platform.runLater(() -> {
                        videoThumb.setImage(vt);
                        videoTitle.setText(selectedVideo.getTitle());
                        videoAuthor.setText(selectedVideo.getChannel().getTitle());
                        videoAuthorThumb.setImage(vat);
                        String desc = selectedVideo.getDescription();
                        try { desc = URLDecoder.decode(desc, "UTF-8"); } catch (Exception ignored) {}
                        videoDesc.setText("Published "+sdf.format(selectedVideo.getPublishedDate())+"  "+desc);
                        views.setText(selectedVideo.getViews()+" views");
                        likes.setText("+"+selectedVideo.getLikes());
                        dislikes.setText("-"+selectedVideo.getDislikes());
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start());

        openBrowser.setOnAction(ae -> {
            List<String> toOpen = new ArrayList<>();
            for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                if(!toOpen.contains(yc.getComment().getYouTubeLink()))
                    toOpen.add(yc.getComment().getYouTubeLink());
            }
            for(String link : toOpen) {
                openInBrowser(link);
            }
        });

        loadThumb.setOnAction(ae -> {
            loadThumb.setDisable(true);
            new Thread(() -> {
                for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                    yc.updateProfileThumb();
                }
                for(YouTubeCommentView yc : commentsList.getItems()) {
                    yc.checkProfileThumb();
                }
                loadThumb.setDisable(false);
            }).start();
        });

        copyName.setOnAction(ae -> {
            List<String> toCopy = new ArrayList<>();
            for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                if(!toCopy.contains(yc.getChannel().getTitle()))
                    toCopy.add(yc.getChannel().getTitle());
            }
            Clipboards.setClipboard(toCopy.stream().collect(Collectors.joining("\r\n")));
        });

        copyText.setOnAction(ae -> {
            List<String> toCopy = new ArrayList<>();
            for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                toCopy.add(yc.getParsedText());
            }
            Clipboards.setClipboard(toCopy.stream().collect(Collectors.joining("\r\n")));
        });

        copyChannelLink.setOnAction(ae -> {
            List<String> toCopy = new ArrayList<>();
            for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                if(!toCopy.contains(yc.getChannel().getYouTubeLink()))
                    toCopy.add(yc.getChannel().getYouTubeLink());
            }
            Clipboards.setClipboard(toCopy.stream().collect(Collectors.joining("\r\n")));
        });

        copyVideoLink.setOnAction(ae -> {
            List<String> toCopy = new ArrayList<>();
            for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                if(!toCopy.contains("https://youtu.be/"+yc.getComment().getVideoId()))
                    toCopy.add("https://youtu.be/"+yc.getComment().getVideoId());
            }
            Clipboards.setClipboard(toCopy.stream().collect(Collectors.joining("\r\n")));
        });

        copyCommentLink.setOnAction(ae -> {
            List<String> toCopy = new ArrayList<>();
            for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                toCopy.add(yc.getComment().getYouTubeLink());
            }
            Clipboards.setClipboard(toCopy.stream().collect(Collectors.joining("\r\n")));
        });

        Button backToResults = new Button("Back to Results");
        backToResults.setVisible(false);
        backToResults.setManaged(false);
        backToResults.setStyle("-fx-base: forestgreen");
        backToResults.setOnAction(ae -> Platform.runLater(() -> {
            treeComments.clear();
            commentsList.setItems(originalComments);
            backToResults.setVisible(false);
            backToResults.setManaged(false);
        }));

        SimpleIntegerProperty queryUpdate = new SimpleIntegerProperty(0);
        SimpleIntegerProperty pageNum = new SimpleIntegerProperty(1);
        SimpleIntegerProperty lastPageNum = new SimpleIntegerProperty(1);

        Button firstPage = new Button("<<");
        firstPage.disableProperty().bind(backToResults.managedProperty().or(pageNum.isEqualTo(1)));

        Button prevPage = new Button("<");
        prevPage.disableProperty().bind(backToResults.managedProperty().or(pageNum.isEqualTo(1)));

        Button nextPage = new Button(">");
        nextPage.disableProperty().bind(backToResults.managedProperty().or(pageNum.greaterThanOrEqualTo(lastPageNum)));

        Button lastPage = new Button(">>");
        lastPage.disableProperty().bind(backToResults.managedProperty().or(pageNum.greaterThanOrEqualTo(lastPageNum)));

        Label results = new Label("Page 0 of 0. Showing 0 of 0.");
        queryUpdate.addListener((o, ov, nv) -> Platform.runLater(() -> results.setText(String.format("Page %s of %s. Showing %s of %s.", query.getPage(), query.getPageCount(), commentsList.getItems().size(), query.getTotalResults()))));

        HBox hbox0 = new HBox();
        hbox0.setAlignment(Pos.CENTER);
        hbox0.getChildren().addAll(firstPage, prevPage, backToResults, nextPage, lastPage);

        VBox commentsBox = new VBox(10);
        commentsBox.setAlignment(Pos.TOP_CENTER);
        commentsBox.setPadding(new Insets(10));
        commentsBox.getChildren().addAll(commentsList, hbox0, results);
        HBox.setHgrow(commentsBox, Priority.ALWAYS);

        Label label1 = new Label("Select Group");
        label1.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 16));

        ComboBox<GroupItem> groupItem = new ComboBox<>();
        groupItem.setId("control");
        groupItem.setDisable(true);
        groupItem.setMaxWidth(Double.MAX_VALUE);
        groupItem.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> groupItem.setDisable(nv == null || nv.getYouTubeId().equals(GroupItem.NO_ITEMS)));

        ComboBox<Group> group = new ComboBox<>();
        group.setId("control");
        group.setMaxWidth(Double.MAX_VALUE);
        group.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if(ov != null) {
                ov.itemsUpdatedProperty().removeListener(cl);
                ov.itemsUpdatedProperty().unbind();
                ov.nameProperty().unbind();
            }
            if(nv != null) {
                commentsGroupId.setValue(nv.getId());
                nv.nameProperty().addListener((o1, ov1, nv1) -> {
                    group.setItems(database.globalGroupList);
                    group.setValue(nv);
                });
            }
            if(!group.isDisabled() && nv != null) {
                nv.itemsUpdatedProperty().addListener(cl = (o1, ov1, nv1) -> {
                    GroupItem allItems = new GroupItem(GroupItem.ALL_ITEMS, "All items ("+nv.getGroupItems().size()+")");
                    Platform.runLater(() -> {
                        groupItem.getItems().clear();
                        if(!nv.getGroupItems().isEmpty()) {
                            if(!nv.getGroupItems().get(0).getYouTubeId().equals(GroupItem.NO_ITEMS)) {
                                groupItem.getItems().add(allItems);
                            }
                            groupItem.getItems().addAll(nv.getGroupItems());
                            groupItem.getSelectionModel().select(0);
                        }
                    });
                });
                nv.incrementItemsUpdated();
            }
        });
        group.setItems(database.globalGroupList);
        group.getItems().addListener((ListChangeListener<Group>) c -> {
            if(group.getSelectionModel().getSelectedIndex() == -1 && group.getItems() != null && group.getItems().size() > 0) {
                group.getSelectionModel().select(0);
            }
        });
        if(group.getSelectionModel().getSelectedIndex() == -1 && group.getItems() != null && group.getItems().size() > 0) {
            group.getSelectionModel().select(0);
        }

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

        Button search = new Button("Search");
        search.setId("control");
        search.setPrefWidth(130);

        Button clear = new Button("Clear Results");
        clear.setId("control");
        clear.setPrefWidth(130);
        clear.setOnAction(ae -> Platform.runLater(() -> commentsList.getItems().clear()));

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
        searchBox.setOnKeyPressed(ke -> {
            if(ke.getCode().equals(KeyCode.ENTER)) {
                search.fire();
            }
        });

        HBox hbox = new HBox();
        hbox.setFillHeight(true);
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().addAll(contextBox, contextToggle, commentsBox, searchBox);

        firstPage.setOnAction(ae -> new Thread(() -> {
            searchBox.setDisable(true);
            try {
                List<YouTubeCommentView> commentViews = query.get(1, group.getValue(), groupItem.getValue().getYouTubeId().equals(GroupItem.ALL_ITEMS) ? null : groupItem.getValue())
                        .stream().map(c -> new YouTubeCommentView(c, true)).collect(Collectors.toList());
                Platform.runLater(() -> {
                    treeComments.clear();
                    originalComments.clear();
                    originalComments.addAll(commentViews);
                    commentsList.setItems(originalComments);
                    lastPageNum.setValue(query.getPageCount());
                    pageNum.setValue(query.getPage());
                    queryUpdate.setValue(queryUpdate.getValue()+1);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
            searchBox.setDisable(false);
        }).start());

        nextPage.setOnAction(ae -> new Thread(() -> {
            searchBox.setDisable(true);
            try {
                List<YouTubeCommentView> commentViews = query.get(query.getPage()+1, group.getValue(), groupItem.getValue().getYouTubeId().equals(GroupItem.ALL_ITEMS) ? null : groupItem.getValue())
                        .stream().map(c -> new YouTubeCommentView(c, true)).collect(Collectors.toList());
                Platform.runLater(() -> {
                    treeComments.clear();
                    originalComments.clear();
                    originalComments.addAll(commentViews);
                    commentsList.setItems(originalComments);
                    lastPageNum.setValue(query.getPageCount());
                    pageNum.setValue(query.getPage());
                    queryUpdate.setValue(queryUpdate.getValue()+1);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
            searchBox.setDisable(false);
        }).start());

        prevPage.setOnAction(ae -> new Thread(() -> {
            searchBox.setDisable(true);
            try {
                List<YouTubeCommentView> commentViews = query.get(query.getPage()-1, group.getValue(), groupItem.getValue().getYouTubeId().equals(GroupItem.ALL_ITEMS) ? null : groupItem.getValue())
                        .stream().map(c -> new YouTubeCommentView(c, true)).collect(Collectors.toList());
                Platform.runLater(() -> {
                    treeComments.clear();
                    originalComments.clear();
                    originalComments.addAll(commentViews);
                    commentsList.setItems(originalComments);
                    lastPageNum.setValue(query.getPageCount());
                    pageNum.setValue(query.getPage());
                    queryUpdate.setValue(queryUpdate.getValue()+1);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
            searchBox.setDisable(false);
        }).start());

        lastPage.setOnAction(ae -> new Thread(() -> {
            searchBox.setDisable(true);
            try {
                List<YouTubeCommentView> commentViews = query.get(query.getPageCount(), group.getValue(), groupItem.getValue().getYouTubeId().equals(GroupItem.ALL_ITEMS) ? null : groupItem.getValue())
                        .stream().map(c -> new YouTubeCommentView(c, true)).collect(Collectors.toList());
                Platform.runLater(() -> {
                    treeComments.clear();
                    originalComments.clear();
                    originalComments.addAll(commentViews);
                    commentsList.setItems(originalComments);
                    lastPageNum.setValue(query.getPageCount());
                    pageNum.setValue(query.getPage());
                    queryUpdate.setValue(queryUpdate.getValue()+1);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
            searchBox.setDisable(false);
        }).start());

        search.setOnAction(ae -> new Thread(() -> {
            searchBox.setDisable(true);
            try {
                query = database.commentQuery()
                        .limit(500)
                        .ctype(type.getSelectionModel().getSelectedIndex())
                        .after(getDatePickerDate(dateFrom, false).getTime())
                        .before(getDatePickerDate(dateTo, true).getTime())
                        .orderBy(orderBy.getSelectionModel().getSelectedIndex())
                        .textLike(textLike.getText())
                        .nameLike(nameLike.getText());
                List<YouTubeCommentView> commentViews = query.get(1, group.getValue(), groupItem.getValue().getYouTubeId().equals(GroupItem.ALL_ITEMS) ? null : groupItem.getValue())
                        .stream().map(c -> new YouTubeCommentView(c, true)).collect(Collectors.toList());
                Platform.runLater(() -> {
                    treeComments.clear();
                    originalComments.clear();
                    originalComments.addAll(commentViews);
                    commentsList.setItems(originalComments);
                    lastPageNum.setValue(query.getPageCount());
                    pageNum.setValue(query.getPage());
                    queryUpdate.setValue(queryUpdate.getValue()+1);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
            searchBox.setDisable(false);
        }).start());

        StackPane stack = new StackPane(hbox);
        stack.setPadding(new Insets(0));

        showMore.setOnAction(ae -> {
            ImageView profile = new ImageView(actionComment.getChannel().getThumbUrl());
            profile.setFitHeight(32);
            profile.setFitWidth(32);

            Label name = new Label(actionComment.getChannel().getTitle());
            name.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 15));

            HBox hbox3 = new HBox(10);
            hbox3.setAlignment(Pos.CENTER_LEFT);
            hbox3.getChildren().addAll(profile, name);

            TextArea textArea = new TextArea();
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setText(actionComment.getParsedText());
            VBox.setVgrow(textArea, Priority.ALWAYS);

            Button close = new Button("Close");

            HBox hbox4 = new HBox();
            hbox4.setAlignment(Pos.CENTER_RIGHT);
            hbox4.getChildren().add(close);

            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(25));
            vbox.setId("overlayMenu");
            vbox.setMinWidth(400);
            vbox.setMaxHeight(400);
            vbox.setMaxWidth(400);
            vbox.getChildren().addAll(hbox3, textArea, hbox4);

            StackPane overflow = new StackPane(vbox);
            overflow.setStyle("-fx-background-color: rgba(127,127,127,0.4)");
            stack.getChildren().addAll(overflow);

            close.setOnAction(ae2 -> stack.getChildren().remove(overflow));
        });

        reply.setOnAction(ae -> {
            ImageView profile = new ImageView(actionComment.getChannel().getThumbUrl());
            profile.setFitHeight(32);
            profile.setFitWidth(32);

            Label name = new Label(actionComment.getChannel().getTitle());
            name.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 15));

            HBox hbox3 = new HBox(10);
            hbox3.setAlignment(Pos.CENTER_LEFT);
            hbox3.getChildren().addAll(profile, name);

            TextArea textArea = new TextArea();
            textArea.setWrapText(true);
            if(config().prefixReplies()) {
                textArea.setText("+"+actionComment.getChannel().getTitle()+" ");
            }
            VBox.setVgrow(textArea, Priority.ALWAYS);

            Button close = new Button("Close");

            Button reply = new Button("Post");
            reply.setStyle("-fx-base: cornflowerblue");

            class AccButtonCell extends ListCell<YouTubeAccount> {
                public void updateItem(YouTubeAccount item, boolean empty) {
                    super.updateItem(item, empty);
                    if(empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getUsername());
                    }
                }
            }

            ComboBox<YouTubeAccount> replyAs = new ComboBox<>();
            replyAs.setButtonCell(new AccButtonCell());
            replyAs.setStyle("-fx-background-color: transparent;");
            replyAs.setMaxWidth(150);
            replyAs.setPrefWidth(150);
            replyAs.getItems().addAll(config.getAccounts());
            replyAs.getSelectionModel().select(0);

            HBox hbox4 = new HBox(10);
            hbox4.setAlignment(Pos.CENTER_RIGHT);
            hbox4.getChildren().addAll(replyAs, reply, close);

            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(25));
            vbox.setId("overlayMenu");
            vbox.setMinWidth(400);
            vbox.setMaxHeight(400);
            vbox.setMaxWidth(400);
            vbox.getChildren().addAll(hbox3, textArea, hbox4);

            StackPane overflow = new StackPane(vbox);
            overflow.setStyle("-fx-background-color: rgba(127,127,127,0.4)");
            stack.getChildren().addAll(overflow);

            close.setOnAction(ae2 -> stack.getChildren().remove(overflow));

            reply.setOnAction(ae2 -> {
                try {
                    oauth2.setTokens(replyAs.getValue().getTokens());
                    CommentsList.Item item = oauth2.postReply(actionComment.getComment().getYouTubeId(), textArea.getText());
                    YouTubeComment replyMade = new YouTubeComment(item, actionComment.getComment().getVideoId());
                    List<YouTubeComment> list = new ArrayList<>();
                    list.add(replyMade);
                    database.insertComments(list);
                    database.commit();
                    treeComments.add(new YouTubeCommentView(replyMade, false));
                    close.fire();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        viewTree.setOnAction(ae -> {
            try {
                String parentId = actionComment.getComment().isReply() ? actionComment.getComment().getParentId() : actionComment.getComment().getYouTubeId();
                treeComments.addAll(database.getCommentTree(parentId)
                        .stream().map(c -> new YouTubeCommentView(c, false)).collect(Collectors.toList()));
                Platform.runLater(() -> {
                    commentsList.setItems(treeComments);
                    backToResults.setManaged(true);
                    backToResults.setVisible(true);
                });
            } catch (Exception ignored) {}
        });

        return stack;
    }

    protected void showMore(YouTubeCommentView ycv) {
        actionComment = ycv;
        showMore.fire();
    }

    protected void reply(YouTubeCommentView ycv) {
        actionComment = ycv;
        reply.fire();
    }

    protected void viewTree(YouTubeCommentView ycv) {
        actionComment = ycv;
        viewTree.fire();
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
        groupList.setId("control");
        groupList.setPrefWidth(200);
        groupList.setMaxWidth(200);
        groupList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if(ov != null) {
                ov.nameProperty().unbind();
            }
            if(nv != null) {
                managerGroupId.setValue(nv.getId());
                nv.nameProperty().addListener((o1, ov1, nv1) -> {
                    groupList.setItems(database.globalGroupList);
                    groupList.setValue(nv);
                });
            }
            Platform.runLater(() -> {
                managerDisplay.getChildren().clear();
                if(!groupList.isDisabled() && nv != null) {
                    if(!managerMap.containsKey(nv.getId()) || managerMap.get(nv.getId()).deletedProperty().getValue()) {
                        managerMap.put(nv.getId(), new GroupManageView(nv));
                    }
                    managerDisplay.getChildren().add(managerMap.get(nv.getId()));
                } else {
                    managerDisplay.getChildren().add(message);
                }
            });
        });
        groupList.setItems(database.globalGroupList);
        groupList.getItems().addListener((ListChangeListener<Group>) c -> {
            if(groupList.getSelectionModel().getSelectedIndex() == -1 && groupList.getItems() != null && groupList.getItems().size() > 0) {
                groupList.getSelectionModel().select(0);
            }
        });
        if(groupList.getSelectionModel().getSelectedIndex() == -1 && groupList.getItems() != null && groupList.getItems().size() > 0) {
            groupList.getSelectionModel().select(0);
        }

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

        managerDisplay.setAlignment(Pos.CENTER);
        managerDisplay.getChildren().add(message);
        VBox.setVgrow(managerDisplay, Priority.ALWAYS);

        VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().addAll(control, divider, managerDisplay);

        StackPane stack = new StackPane(vbox);
        stack.setPadding(new Insets(10,0,0,0));

        create.setOnAction(ae -> {
            Label title = new Label("Create Group");
            title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

            TextField nameField = new TextField();
            nameField.setMinWidth(250);
            nameField.setPromptText("Group name...");

            Label error = new Label("");
            error.setManaged(false);

            Button doCreate = new Button("Create");
            doCreate.setStyle("-fx-base: derive(cornflowerblue, 70%)");

            Button cancel = new Button("Cancel");

            HBox hbox0 = new HBox(10);
            hbox0.setAlignment(Pos.CENTER_RIGHT);
            hbox0.getChildren().addAll(cancel, doCreate);

            VBox vbox0 = new VBox(10);
            vbox0.setAlignment(Pos.CENTER);
            vbox0.setMaxWidth(0);
            vbox0.setMaxHeight(0);
            vbox0.setId("overlayMenu");
            vbox0.setPadding(new Insets(25));
            vbox0.getChildren().addAll(title, nameField, error, hbox0);

            StackPane overlay = new StackPane(vbox0);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");
            stack.getChildren().add(overlay);

            cancel.setOnAction(ae0 -> stack.getChildren().remove(overlay));

            doCreate.setOnAction(ae0 -> {
                try {
                    Group group = database.createGroup(nameField.getText());
                    groupList.setValue(group);
                    cancel.fire();
                } catch (SQLException e) {
                    error.setText(e.getMessage());
                    error.setManaged(true);
                }
            });
        });

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
        youtubeList.setCellFactory(cf -> new ListViewEmptyCellFactory(143));
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

        addGroup.setOnAction(ae -> {
            List<SearchList.Item> items = youtubeList.getSelectionModel()
                    .getSelectedItems().stream()
                    .map(SearchListView::getItem).collect(Collectors.toList());

            Label title = new Label("Selected "+items.size()+" items");
            title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 16));

            ToggleGroup tg = new ToggleGroup();

            RadioButton existing = new RadioButton("Add to existing group:");
            existing.setToggleGroup(tg);

            ComboBox<Group> groups = new ComboBox<>();
            groups.setMaxWidth(250);
            groups.setPrefWidth(250);
            groups.setItems(database.globalGroupList);
            groups.disableProperty().bind(existing.selectedProperty().not());

            RadioButton newgroup = new RadioButton("Add to new group:");
            newgroup.setToggleGroup(tg);

            TextField groupName = new TextField();
            groupName.setPromptText("Group name here...");
            groupName.disableProperty().bind(existing.selectedProperty());

            Button cancel = new Button("Cancel");
            Button finish = new Button("Finish");
            finish.setStyle("-fx-base: derive(cornflowerblue, 95%)");
            finish.disableProperty().bind(
                    groups.getSelectionModel().selectedIndexProperty().isEqualTo(-1).and(groups.disabledProperty().not())
                            .or(groupName.textProperty().isEqualTo("").and(groupName.disabledProperty().not()))
            );

            HBox hbox0 = new HBox(10);
            hbox0.setAlignment(Pos.CENTER_RIGHT);
            hbox0.getChildren().addAll(cancel, finish);

            Label warn = new Label();
            warn.setTextFill(Color.RED);
            warn.setVisible(false);
            warn.setManaged(false);

            VBox vbox0 = new VBox(10);
            vbox0.setFillWidth(true);
            vbox0.setPadding(new Insets(25));
            vbox0.setMaxHeight(0);
            vbox0.setMaxWidth(250);
            vbox0.setAlignment(Pos.CENTER);
            vbox0.setId("overlayMenu");
            vbox0.getChildren().addAll(title, existing, groups, newgroup, groupName, warn, hbox0);

            StackPane overlay = new StackPane(vbox0);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4)");
            overlay.getChildren().addAll();
            stack.getChildren().add(overlay);

            cancel.setOnAction(ae0 -> stack.getChildren().remove(overlay));
            finish.setOnAction(ae0 -> {
                try {
                    Group group = null;
                    if(existing.isSelected()) {
                        group = groups.getValue();
                    } else if(newgroup.isSelected()) {
                        group = database.createGroup(groupName.getText());
                    }
                    if(group != null) {
                        List<GroupItem> insertItems = items.stream().map(GroupItem::new).collect(Collectors.toList());
                        database.insertGroupItems(group, insertItems);
                        database.commit();
                        group.reloadGroupItems();
                        cancel.fire();
                    }
                } catch (SQLException e) {
                    Platform.runLater(() -> {
                        warn.setManaged(true);
                        warn.setVisible(true);
                        warn.setText(e.getMessage());
                    });
                    e.printStackTrace();
                }
            });
        });
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
