package mattw.youtube.commentsuite;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.commentsuite.db.YouTubeObject;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class SettingsPane extends StackPane {

    private static final String RELEASE = "v1.3.0-a";

    public SettingsPane(StackPane parent, OAuth2Handler oauth2) {
        Config config = CommentSuite.config();

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
            if(parent.getChildren().contains(this)) {
                parent.getChildren().remove(this);
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

        CheckBox prefixReplies = new CheckBox("Prefix +{name} when replying to comments.");
        prefixReplies.setSelected(config.prefixReplies());

        CheckBox loadStats = new CheckBox("Auto-load stats while managing a group.");
        loadStats.setSelected(config.autoLoadStats());

        CheckBox downloadThumbs = new CheckBox("Download thumbnails for archiving.");
        downloadThumbs.setSelected(config.downloadThumbs());

        Label label2 = new Label("YouTube Accounts");
        label2.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

        Button signIn = new Button("Add Account");

        ListView<YouTubeAccountView> accountList = new ListView<>();
        accountList.setId("listView");
        accountList.setMinHeight(150);
        accountList.setMaxHeight(150);
        accountList.setCellFactory(cf -> new ListViewEmptyCellFactory<>(40));
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

        Label about = new Label("MIT License. Copyright (c) 2018 Matthew Wright.");

        ImageView gitImg = new ImageView("/mattw/youtube/commentsuite/img/github.png");
        gitImg.setFitWidth(20);
        gitImg.setFitHeight(20);

        Hyperlink git = new Hyperlink("mattwright324/youtube-comment-suite");
        git.setMaxHeight(20);
        git.setGraphic(gitImg);
        git.setOnAction(ae -> CommentSuite.openInBrowser("https://github.com/mattwright324/youtube-comment-suite"));

        VBox vbox2 = new VBox(10);
        vbox2.setPadding(new Insets(10));
        vbox2.setAlignment(Pos.TOP_LEFT);
        vbox2.getChildren().addAll(label1, prefixReplies, loadStats, downloadThumbs, label2, signIn, accountList, label4, hbox2, label3, about, release, git);

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
            config.setDownloadThumbs(downloadThumbs.isSelected());
            config.save();
            close.fire();
        });

        HBox hbox3 = new HBox();
        hbox3.setFillHeight(true);
        hbox3.setAlignment(Pos.CENTER_RIGHT);
        hbox3.getChildren().addAll(vboxWeb, vbox);
        VBox.setVgrow(hbox3, Priority.ALWAYS);

        getChildren().add(hbox3);
        setAlignment(Pos.CENTER_RIGHT);
        setStyle("-fx-background-color: rgba(127,127,127,0.4)");

        vacuum.setOnAction(ae -> new Thread(() -> {
            Label label = new Label("Performing VACUUM");
            label.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

            ProgressIndicator prog = new ProgressIndicator();
            prog.setMaxWidth(25);
            prog.setMaxHeight(25);

            VBox vbox0 = new VBox(10);
            vbox0.setId("overlayMenu");
            vbox0.setMaxHeight(100);
            vbox0.setMaxWidth(200);
            vbox0.setPadding(new Insets(25));
            vbox0.getChildren().addAll(label, prog);

            StackPane overlay = new StackPane(vbox0);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");

            Platform.runLater(() -> getChildren().add(overlay));
            try { CommentSuite.db().vacuum(); } catch (SQLException ignored) {}
            Platform.runLater(() -> getChildren().remove(overlay));
        }).start());

        reset.setOnAction(ae -> new Thread(() -> {
            Label label = new Label("Resetting Database");
            label.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

            ProgressIndicator prog = new ProgressIndicator();
            prog.setMaxWidth(25);
            prog.setMaxHeight(25);

            VBox vbox0 = new VBox(10);
            vbox0.setAlignment(Pos.CENTER);
            vbox0.setMaxHeight(100);
            vbox0.setMaxWidth(200);
            vbox0.setId("overlayMenu");
            vbox0.setPadding(new Insets(25));
            vbox0.getChildren().addAll(label, prog);

            StackPane overlay = new StackPane(vbox0);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");

            Platform.runLater(() -> getChildren().add(overlay));
            try {
                File thumbs = YouTubeObject.thumbFolder;
                if(thumbs.exists()) {
                    for(File f : thumbs.listFiles()) {
                        f.delete();
                    }
                    thumbs.delete();
                }
                CommentSuite.db().reset();
                for(Group group : CommentSuite.db().globalGroupList) {
                    group.reloadGroupItems();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> getChildren().remove(overlay));
            System.out.println("Database Reset");
        }).start());
    }

}
