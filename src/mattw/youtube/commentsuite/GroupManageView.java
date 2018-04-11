package mattw.youtube.commentsuite;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.commentsuite.db.GroupItem;
import mattw.youtube.commentsuite.db.YouTubeChannel;
import mattw.youtube.commentsuite.db.YouTubeObject;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.ChannelsList;
import mattw.youtube.datav3.resources.PlaylistsList;
import mattw.youtube.datav3.resources.SearchList;
import mattw.youtube.datav3.resources.VideosList;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GroupManageView extends StackPane {

    private static Image IMG_GROUP = new Image("/mattw/youtube/commentsuite/img/group.png");
    private static Random rand = new Random();

    private Group group;
    private Label lblChecked = new Label("Last refreshed: [...].");
    private long lastChecked = Long.MAX_VALUE;

    private Button reload = new Button("Reload");
    private ProgressIndicator prog = new ProgressIndicator();

    private XYChart.Series<String,Number> commentSeries = new XYChart.Series<>();
    private XYChart.Series<String,Number> videoSeries = new XYChart.Series<>();
    private Label commentCount = new Label("...");
    private Label totalLikes = new Label("...");
    private Label videoCount = new Label("...");
    private Label totalViews = new Label("...");
    private TitledPane statsPane = new TitledPane();
    private ListView<YouTubeObjectView> activeViewers = new ListView<>();
    private ListView<YouTubeObjectView> popularViewers = new ListView<>();
    private TitledPane viewerPane = new TitledPane();
    private ListView<YouTubeObjectView> popularVideos = new ListView<>();
    private ListView<YouTubeObjectView> dislikedVideos = new ListView<>();
    private ListView<YouTubeObjectView> commentedVideos = new ListView<>();
    private ListView<YouTubeObjectView> disabledVideos = new ListView<>();
    private TitledPane videosPane = new TitledPane();

    private SimpleBooleanProperty deleted = new SimpleBooleanProperty(false);

    class YouTubeObjectView extends HBox {
        public YouTubeObjectView(YouTubeObject yto, String displayStat) {
            super(10);

            ImageView thumb = new ImageView(yto.getThumbUrl()); // 57 x 32, 89 x 50
            thumb.setFitHeight(50);
            thumb.setFitWidth(yto instanceof YouTubeChannel ? 50 : 89);
            thumb.setCursor(Cursor.HAND);
            thumb.setOnMouseClicked(me -> CommentSuite.openInBrowser(yto.getYouTubeLink()));

            Label title = new Label(yto.getTitle());
            title.setMinWidth(0);
            title.setPrefWidth(0);
            title.setMaxWidth(Double.MAX_VALUE);
            title.setFont(Font.font(("Tahoma"), FontWeight.SEMI_BOLD, 15));

            Label stat = new Label(displayStat);
            if(displayStat.equals("Comments Disabled")) {
                stat.setTextFill(Color.RED);
            }

            VBox vbox = new VBox(5);
            vbox.setAlignment(Pos.CENTER_LEFT);
            vbox.setMaxWidth(Double.MAX_VALUE);
            vbox.getChildren().addAll(title, stat);
            HBox.setHgrow(vbox, Priority.ALWAYS);

            setAlignment(Pos.CENTER_LEFT);
            getChildren().addAll(thumb, vbox);
        }
    }

    class GroupItemView extends HBox {
        private GroupItem item;
        public GroupItemView(GroupItem item) {
            super(10);
            this.item = item;

            Image image = new Image(item.getThumbUrl());
            if(image.isError()) {
                if(item.getTypeName().equals("Video")) {
                    image = new Image("/mattw/youtube/commentsuite/img/video.png");
                } else if(item.getTypeName().equals("Channel")) {
                    image = new Image("/mattw/youtube/commentsuite/img/channel.png");
                } else if(item.getTypeName().equals("Playlist")) {
                    image = new Image("/mattw/youtube/commentsuite/img/playlist.png");
                }
            }

            ImageView thumb = new ImageView(image);
            thumb.setFitWidth(30);
            thumb.setFitHeight(30);

            Label title = new Label(item.getTitle());
            title.setMinWidth(0);
            title.setPrefWidth(0);
            title.setMaxWidth(Double.MAX_VALUE);
            title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 15));

            Label subtitle = new Label(item.getChannelTitle());
            subtitle.setMinWidth(0);
            subtitle.setPrefWidth(0);
            subtitle.setMaxWidth(Double.MAX_VALUE);
            subtitle.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 12));
            HBox.setHgrow(subtitle, Priority.ALWAYS);

            Label type = new Label(item.getTypeName());
            type.setFont(Font.font("Tahoma", FontWeight.BOLD, 12));

            HBox hbox = new HBox(5);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.getChildren().addAll(subtitle, type);

            VBox vbox = new VBox(5);
            vbox.setFillWidth(true);
            vbox.setAlignment(Pos.CENTER_LEFT);
            vbox.getChildren().addAll(title, hbox);
            HBox.setHgrow(vbox, Priority.ALWAYS);

            setAlignment(Pos.CENTER_LEFT);
            getChildren().addAll(thumb, vbox);
            setPadding(new Insets(5));
        }
        public GroupItem getItem() { return item; }
    }

    public SimpleBooleanProperty deletedProperty() { return deleted; }

    public GroupManageView(Group group) {
        this.group = group;
        this.lastChecked = CommentSuite.db().getLastChecked(group);

        Color color = Color.color(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());

        int wh = 25;
        ColorAdjust monochrome = new ColorAdjust();
        monochrome.setBrightness(1.0);
        Blend blush = new Blend(BlendMode.MULTIPLY, monochrome, new ColorInput(0, 0, wh, wh, color));

        ImageView clip = new ImageView(IMG_GROUP);
        clip.setFitHeight(wh);
        clip.setFitWidth(wh);

        ImageView view = new ImageView(IMG_GROUP);
        view.setFitWidth(wh);
        view.setFitHeight(wh);
        view.setEffect(blush);
        view.setCache(true);
        view.setCacheHint(CacheHint.SPEED);
        view.setClip(clip);

        Font titleFont = Font.font("Tahoma", FontWeight.SEMI_BOLD, 18);
        Font subtitleFont = Font.font("Tahoma", FontWeight.BOLD, 15);

        Label title = new Label();
        title.setMinWidth(0);
        title.setPrefWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setFont(titleFont);
        title.textProperty().bind(group.nameProperty());
        HBox.setHgrow(title, Priority.ALWAYS);

        lblChecked.setMinWidth(0);
        lblChecked.setPrefWidth(0);
        lblChecked.setAlignment(Pos.CENTER_RIGHT);
        lblChecked.setMaxWidth(Double.MAX_VALUE);
        lblChecked.setTextFill(Color.LIGHTGRAY);
        HBox.setHgrow(lblChecked, Priority.ALWAYS);

        prog.setVisible(false);
        prog.setManaged(false);
        prog.setMaxHeight(wh);
        prog.setMaxWidth(wh);

        Button refresh = new Button("Refresh");
        refresh.setStyle("-fx-base: gold;");
        refresh.setTooltip(new Tooltip("Starts the refresh process checking GroupItems for new videos and comments."));
        refresh.setOnAction(ae -> beginGroupRefresh());

        Button delete = new Button("Delete");
        delete.setStyle("-fx-base: salmon;");
        delete.setTooltip(new Tooltip("Delete this group and all of its data."));

        Button rename = new Button("Rename");
        rename.setTooltip(new Tooltip("Rename this group."));

        reload.setTooltip(new Tooltip("Reload group data."));
        reload.setOnAction(ae -> loadAnalytics());

        HBox control = new HBox(10);
        control.setAlignment(Pos.CENTER_RIGHT);
        control.getChildren().addAll(view, title, lblChecked, prog, rename, reload, refresh, delete);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Weeks");
        NumberAxis yAxis = new NumberAxis();
        LineChart<String,Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setMinHeight(200);
        chart.setPrefWidth(300);
        chart.setMaxHeight(300);
        chart.setTitle("Comments By Week");
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.getData().add(commentSeries);

        CategoryAxis xAxis2 = new CategoryAxis();
        xAxis2.setLabel("Weeks");
        NumberAxis yAxis2 = new NumberAxis();
        LineChart<String,Number> chart2 = new LineChart<>(xAxis2, yAxis2);
        chart2.setMinHeight(200);
        chart2.setPrefWidth(300);
        chart2.setMaxHeight(300);
        chart2.setTitle("Videos By Week");
        chart2.setCreateSymbols(false);
        chart2.setLegendVisible(false);
        chart2.getData().add(videoSeries);

        Label label1 = new Label("Total Comments");
        label1.setFont(subtitleFont);
        Label label2 = new Label("Total Likes");
        label2.setFont(subtitleFont);
        totalLikes.setTextFill(Color.CORNFLOWERBLUE);
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHalignment(HPos.RIGHT);
        col2.setMaxWidth(100);
        col2.setPrefWidth(100);

        GridPane grid = new GridPane();
        grid.getColumnConstraints().addAll(col1, col2);
        grid.setHgap(10);
        grid.setVgap(5);
        int row = 1;
        grid.addRow(row++, label1, commentCount);
        grid.addRow(row++, label2, totalLikes);

        Label label3 = new Label("Total Videos");
        label3.setFont(subtitleFont);
        Label label4 = new Label("Total Views");
        label4.setFont(subtitleFont);
        ColumnConstraints col3 = new ColumnConstraints();
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setHalignment(HPos.RIGHT);
        col4.setMaxWidth(100);
        col4.setPrefWidth(100);

        GridPane grid2 = new GridPane();
        grid2.getColumnConstraints().addAll(col3, col4);
        grid2.setHgap(10);
        grid2.setVgap(5);
        int row2 = 1;
        grid2.addRow(row2++, label3, videoCount);
        grid2.addRow(row2++, label4, totalViews);

        VBox stats = new VBox(10);
        stats.setAlignment(Pos.TOP_LEFT);
        stats.setPadding(new Insets(10));
        stats.getChildren().addAll(chart, grid, chart2, grid2);

        ScrollPane statsScroll = new ScrollPane(stats);
        statsScroll.setFitToWidth(true);
        statsScroll.setFitToHeight(true);

        statsPane.setDisable(true);
        statsPane.setText("General Stats");
        statsPane.setContent(statsScroll);

        int height = 300;
        Label popularLabel2 = new Label("Most Popular Viewers");
        popularLabel2.setFont(subtitleFont);
        popularViewers.setCellFactory(cf -> new ListViewEmptyCellFactory<>(58));
        popularViewers.setStyle("-fx-border-color: green");
        popularViewers.setMinHeight(height);
        VBox.setVgrow(popularViewers, Priority.ALWAYS);

        Label activeLabel = new Label("Most Active Viewers");
        activeLabel.setFont(subtitleFont);
        activeViewers.setCellFactory(cf -> new ListViewEmptyCellFactory<>(58));
        activeViewers.setStyle("-fx-border-color: red");
        activeViewers.setMinHeight(height);
        activeViewers.setMaxHeight(height);
        VBox.setVgrow(activeViewers, Priority.ALWAYS);

        VBox vbox3 = new VBox(10);
        vbox3.setAlignment(Pos.TOP_CENTER);
        vbox3.setPadding(new Insets(0, 25, 0, 25));
        vbox3.getChildren().addAll(popularLabel2, popularViewers, activeLabel, activeViewers);

        StackPane viewerStack = new StackPane(vbox3);
        viewerStack.setPadding(new Insets(10));

        ScrollPane viewerScroll = new ScrollPane(viewerStack);
        viewerScroll.setFitToWidth(true);
        viewerScroll.setFitToHeight(true);

        viewerPane.setDisable(true);
        viewerPane.setText("About Viewers");
        viewerPane.setContent(viewerScroll);

        Label popularLabel = new Label("Most Popular Videos");
        popularLabel.setFont(subtitleFont);
        popularVideos.setCellFactory(cf -> new ListViewEmptyCellFactory<>(49));
        popularVideos.setStyle("-fx-border-color: green");
        popularVideos.setMinHeight(height);
        popularVideos.setMaxHeight(height);

        Label dislikeLabel = new Label("Most Disliked Videos");
        dislikeLabel.setFont(subtitleFont);
        dislikedVideos.setCellFactory(cf -> new ListViewEmptyCellFactory<>(58));
        dislikedVideos.setStyle("-fx-border-color: red");
        dislikedVideos.setMinHeight(height);
        dislikedVideos.setMaxHeight(height);

        Label comLabel = new Label("Most Commented Videos");
        comLabel.setFont(subtitleFont);
        commentedVideos.setCellFactory(cf -> new ListViewEmptyCellFactory<>(58));
        commentedVideos.setStyle("-fx-border-color: orange");
        commentedVideos.setMinHeight(height);
        commentedVideos.setMaxHeight(height);

        Label disabledLabel = new Label("Comments Disabled");
        disabledLabel.setFont(subtitleFont);
        disabledVideos.setCellFactory(cf -> new ListViewEmptyCellFactory<>(58));
        disabledVideos.setStyle("-fx-border-color: firebrick");
        disabledVideos.setMinHeight(height);
        disabledVideos.setMaxHeight(height);

        VBox vbox2 = new VBox(10);
        vbox2.setAlignment(Pos.TOP_CENTER);
        vbox2.setPadding(new Insets(0, 25, 0, 25));
        vbox2.getChildren().addAll(popularLabel, popularVideos, dislikeLabel, dislikedVideos, comLabel, commentedVideos, disabledLabel, disabledVideos);

        StackPane videoStack = new StackPane(vbox2);
        videoStack.setPadding(new Insets(10));

        ScrollPane videoScroll = new ScrollPane(videoStack);
        videoScroll.setFitToWidth(true);
        videoScroll.setFitToHeight(true);

        videosPane.setDisable(true);
        videosPane.setText("About Videos");
        videosPane.setContent(videoScroll);

        Accordion accordion = new Accordion();
        accordion.setDisable(false);
        accordion.setMinWidth(300);
        accordion.getPanes().addAll(statsPane, videosPane, viewerPane);
        accordion.setExpandedPane(statsPane);
        HBox.setHgrow(accordion, Priority.ALWAYS);

        Button addItem = new Button("Add Item");
        addItem.setMaxWidth(Double.MAX_VALUE);
        addItem.setTooltip(new Tooltip("Add videos, playlists, and channels."));
        HBox.setHgrow(addItem, Priority.ALWAYS);

        Button remove = new Button("Remove");
        remove.setMaxWidth(Double.MAX_VALUE);
        remove.setTooltip(new Tooltip("Remove "));
        remove.setStyle("-fx-base: derive(firebrick, 95%)");
        HBox.setHgrow(remove, Priority.ALWAYS);

        Button removeAll = new Button("Remove All");
        removeAll.setMaxWidth(Double.MAX_VALUE);
        removeAll.setStyle("-fx-base: derive(red, 95%)");
        HBox.setHgrow(removeAll, Priority.ALWAYS);

        ListView<GroupItemView> groupItem = new ListView<>();
        groupItem.setId("listView");
        groupItem.setCellFactory(cf -> new ListViewEmptyCellFactory<>(49));
        VBox.setVgrow(groupItem, Priority.ALWAYS);
        group.itemsUpdatedProperty().addListener((o, ov, nv) -> Platform.runLater(() -> {
            groupItem.getItems().clear();
            if(!group.getGroupItems().isEmpty()) {
                groupItem.setDisable(false);
                groupItem.getItems().addAll(group.getGroupItems().stream().map(GroupItemView::new).collect(Collectors.toList()));
                groupItem.getSelectionModel().select(0);
                remove.setDisable(false);
                removeAll.setDisable(false);
            } else {
                groupItem.setDisable(true);
                remove.setDisable(true);
                removeAll.setDisable(true);
            }
        }));
        group.reloadGroupItems();

        HBox btns = new HBox(10);
        btns.getChildren().addAll(addItem, remove, removeAll);

        VBox vbox0 = new VBox(10);
        vbox0.setPrefWidth(400);
        vbox0.setMaxWidth(400);
        vbox0.setAlignment(Pos.TOP_LEFT);
        vbox0.getChildren().addAll(btns, groupItem);

        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(vbox0, accordion);
        VBox.setVgrow(hbox, Priority.ALWAYS);

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(control, hbox);

        rename.setOnAction(ae -> {
            Label title1 = new Label("Rename Group");
            title1.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

            String currentName = group.getName();
            Label current = new Label("Current: "+currentName);

            TextField nameField = new TextField(currentName);
            nameField.setMinWidth(250);
            nameField.setPromptText("Group name...");

            Label error = new Label("");
            error.setManaged(false);

            Button doAction = new Button("Rename");
            doAction.setStyle("-fx-base: derive(cornflowerblue, 70%)");

            Button cancel = new Button("Cancel");

            HBox hbox0 = new HBox(10);
            hbox0.setAlignment(Pos.CENTER_RIGHT);
            hbox0.getChildren().addAll(cancel, doAction);

            VBox vbox1 = new VBox(10);
            vbox1.setAlignment(Pos.CENTER);
            vbox1.setMaxWidth(0);
            vbox1.setMaxHeight(0);
            vbox1.setId("overlayMenu");
            vbox1.setPadding(new Insets(25));
            vbox1.getChildren().addAll(title1, current, nameField, error, hbox0);

            StackPane overlay = new StackPane(vbox1);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");
            getChildren().add(overlay);

            cancel.setOnAction(ae0 -> getChildren().remove(overlay));

            doAction.setOnAction(ae0 -> {
                try {
                    CommentSuite.db().renameGroup(group, nameField.getText());
                    cancel.fire();
                } catch (SQLException e) {
                    error.setText(e.getMessage());
                    error.setManaged(true);
                }
            });
        });

        delete.setOnAction(ae -> {
            Label title1 = new Label("Delete Group");
            title1.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

            Label subtitle = new Label("This will delete the group and all of its data.");
            subtitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 13));
            subtitle.setTextFill(Color.RED);

            Label error = new Label("");
            error.setManaged(false);

            Button doAction = new Button("Yes, delete.");
            doAction.setStyle("-fx-base: firebrick");

            Button cancel = new Button("No, dont.");

            HBox hbox0 = new HBox(10);
            hbox0.setAlignment(Pos.CENTER_RIGHT);
            hbox0.getChildren().addAll(cancel, doAction);

            VBox vbox1 = new VBox(10);
            vbox1.setAlignment(Pos.CENTER);
            vbox1.setMaxWidth(400);
            vbox1.setMaxHeight(0);
            vbox1.setId("overlayMenu");
            vbox1.setPadding(new Insets(25));
            vbox1.getChildren().addAll(title1, subtitle, error, hbox0);

            StackPane overlay = new StackPane(vbox1);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");
            getChildren().add(overlay);

            cancel.setOnAction(ae0 -> getChildren().remove(overlay));

            doAction.setOnAction(ae0 -> {
                setDisable(true);
                new Thread(() -> {
                    try {
                        CommentSuite.db().deleteGroup(group);
                        CommentSuite.db().cleanUp();
                        CommentSuite.db().commit();
                        CommentSuite.db().refreshGroups();
                        Platform.runLater(() -> {
                            deleted.setValue(true);
                            setVisible(false);
                            setManaged(false);
                        });
                    } catch (SQLException e) {
                        e.printStackTrace();
                        error.setText(e.getLocalizedMessage());
                        error.setManaged(true);
                    }
                }).start();
            });
        });

        addItem.setOnAction(ae -> {
            Label title1 = new Label("Add GroupItem");
            title1.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

            TextField linkField = new TextField();
            linkField.setPromptText("http://youtu.be/dQw4w9WgXcQ");

            TextArea help = new TextArea("Directly add videos, playlists, and channels.\r\n" +
                    "Example links:\r\n" +
                    "1. https://youtu.be/dQw4w9WgXcQ\r\n" +
                    "2. https://www.youtube.com/watch?v=dQw4w9WgXcQ\r\n" +
                    "3. https://www.youtube.com/channel/UC38IQsAvIsxxjztdMZQtwHA\r\n" +
                    "4. https://www.youtube.com/user/RickAstleyVEVO\r\n" +
                    "5. https://www.youtube.com/playlist?list=PL2MI040U_GXocoLMj6w0tvzxHax_CdWNR");
            help.setEditable(false);
            help.setMinHeight(175);
            help.setPrefHeight(175);
            help.setStyle("-fx-background-color: transparent;");

            Label error = new Label("");
            error.setTextFill(Color.RED);
            error.setManaged(false);

            Button doAction = new Button("Add");
            doAction.setStyle("-fx-base: cornflowerblue");

            Button cancel = new Button("Cancel");

            HBox hbox0 = new HBox(10);
            hbox0.setAlignment(Pos.CENTER_RIGHT);
            hbox0.getChildren().addAll(cancel, doAction);

            VBox vbox1 = new VBox(10);
            vbox1.setAlignment(Pos.CENTER);
            vbox1.setMaxWidth(500);
            vbox1.setMaxHeight(0);
            vbox1.setId("overlayMenu");
            vbox1.setPadding(new Insets(25));
            vbox1.getChildren().addAll(title1, linkField, error, help, hbox0);

            StackPane overlay = new StackPane(vbox1);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");
            getChildren().add(overlay);

            cancel.setOnAction(ae0 -> Platform.runLater(() -> getChildren().remove(overlay)));

            doAction.setOnAction(ae0 -> {
                setDisable(true);
                new Thread(() -> {
                    boolean success = false;
                    try {
                        String link = linkField.getText();
                        String youtubeId;
                        GroupItem gitem = null;
                        if(link.matches("http[s]://(youtu\\.be/|www\\.youtube.com/watch\\?v=)[\\w_\\-]{11}")) {
                            youtubeId = link.substring(link.length()-11, link.length());
                            VideosList vl = CommentSuite.youtube().videosList().getByIds(SearchList.PART_SNIPPET, youtubeId, "");
                            if(vl.items != null) {
                                VideosList.Item item = vl.items[0];
                                gitem = new GroupItem(youtubeId, GroupItem.VIDEO, item.snippet.title, item.snippet.channelTitle,
                                        item.snippet.thumbnails.medium.url.toString(), item.snippet.publishedAt.getTime(), 0);
                                checkChannel(item.snippet.channelId);
                                System.out.println("Video GroupItem - "+gitem);
                            }
                        } else if(link.matches("http[s]://www\\.youtube\\.com/(channel|user)/[\\w_-]+")) {
                            youtubeId = link.substring(link.lastIndexOf("/")+1, link.length());
                            ChannelsList cl;
                            ChannelsList.Item item;
                            if(link.contains("channel")) {
                                cl = CommentSuite.youtube().channelsList().getByChannel(ChannelsList.PART_SNIPPET, youtubeId, "");
                            } else {
                                cl = CommentSuite.youtube().channelsList().getByUsername(ChannelsList.PART_SNIPPET, youtubeId, "");
                            }
                            if(cl.hasItems()) {
                                item = cl.items[0];
                                gitem = new GroupItem(item.getId(), GroupItem.CHANNEL, item.snippet.title, item.snippet.title,
                                        item.snippet.thumbnails.medium.url.toString(), item.snippet.publishedAt.getTime(), 0);
                                checkChannel(item.getId());
                                System.out.println("Channel GroupItem - "+gitem);
                            }
                        } else if(link.matches("http[s]://www\\.youtube\\.com/playlist\\?list=[\\w_-]+")) {
                            youtubeId = link.substring(link.lastIndexOf("=")+1, link.length());
                            PlaylistsList pl = CommentSuite.youtube().playlistsList().getByPlaylist(PlaylistsList.PART_SNIPPET, youtubeId, "");
                            if(pl.hasItems()) {
                                PlaylistsList.Item item = pl.items[0];
                                gitem = new GroupItem(youtubeId, GroupItem.PLAYLIST, item.snippet.title, item.snippet.channelTitle,
                                        item.snippet.thumbnails.medium.url.toString(), item.snippet.publishedAt.getTime(), 0);
                                checkChannel(item.snippet.channelId);
                                System.out.println("Playlist GroupItem - "+gitem);
                            }
                        } else {
                            throw new IOException("Could not parse, check formats below.");
                        }
                        if(gitem != null) {
                            List<GroupItem> list = new ArrayList<>();
                            list.add(gitem);
                            CommentSuite.db().insertGroupItems(group, list);
                            CommentSuite.db().commit();
                            group.reloadGroupItems();
                            success = true;
                        } else {
                            throw new IOException("Could not find link.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            error.setText(e.getLocalizedMessage());
                            error.setManaged(true);
                            error.setVisible(true);
                        });
                    }
                    setDisable(false);
                    if(success) {
                        cancel.fire();
                    }
                }).start();
            });
        });

        remove.setOnAction(ae -> {
            List<GroupItem> selected = groupItem.getSelectionModel().getSelectedItems()
                    .stream().map(GroupItemView::getItem).collect(Collectors.toList());

            Label title1 = new Label("Remove Selected");
            title1.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

            Label subtitle = new Label(selected.size()+" item(s) selected");
            subtitle.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 13));

            Label error = new Label("");
            error.setTextFill(Color.RED);
            error.setManaged(false);

            Button doAction = new Button("Yes, remove.");
            doAction.setStyle("-fx-base: firebrick");

            Button cancel = new Button("No, dont.");

            HBox hbox0 = new HBox(10);
            hbox0.setAlignment(Pos.CENTER_RIGHT);
            hbox0.getChildren().addAll(cancel, doAction);

            VBox vbox1 = new VBox(10);
            vbox1.setAlignment(Pos.CENTER);
            vbox1.setMaxWidth(300);
            vbox1.setMaxHeight(0);
            vbox1.setId("overlayMenu");
            vbox1.setPadding(new Insets(25));
            vbox1.getChildren().addAll(title1, subtitle, error, hbox0);

            StackPane overlay = new StackPane(vbox1);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");
            getChildren().add(overlay);

            cancel.setOnAction(ae0 -> Platform.runLater(() -> getChildren().remove(overlay)));

            doAction.setOnAction(ae0 -> {
                setDisable(true);
                new Thread(() -> {
                    boolean success = false;
                    try {
                        CommentSuite.db().deleteGroupItemLinks(group, selected);
                        CommentSuite.db().cleanUp();
                        CommentSuite.db().commit();
                        group.reloadGroupItems();
                        success = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    setDisable(false);
                    if(success) { cancel.fire(); }
                }).start();
            });
        });

        removeAll.setOnAction(ae -> {
            List<GroupItem> items = groupItem.getItems()
                    .stream().map(GroupItemView::getItem).collect(Collectors.toList());

            Label title1 = new Label("Remove All Items");
            title1.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

            Label subtitle = new Label(items.size()+" items total");

            Label error = new Label("");
            error.setTextFill(Color.RED);
            error.setManaged(false);

            Button doAction = new Button("Yes, remove.");
            doAction.setStyle("-fx-base: firebrick");

            Button cancel = new Button("No, dont.");

            HBox hbox0 = new HBox(10);
            hbox0.setAlignment(Pos.CENTER_RIGHT);
            hbox0.getChildren().addAll(cancel, doAction);

            VBox vbox1 = new VBox(10);
            vbox1.setAlignment(Pos.CENTER);
            vbox1.setMaxWidth(300);
            vbox1.setMaxHeight(0);
            vbox1.setId("overlayMenu");
            vbox1.setPadding(new Insets(25));
            vbox1.getChildren().addAll(title1, subtitle, error, hbox0);

            StackPane overlay = new StackPane(vbox1);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");
            getChildren().add(overlay);

            cancel.setOnAction(ae0 -> Platform.runLater(() -> getChildren().remove(overlay)));

            doAction.setOnAction(ae0 -> {
                setDisable(true);
                new Thread(() -> {
                    boolean success = false;
                    try {
                        CommentSuite.db().deleteGroupItemLinks(group, items);
                        CommentSuite.db().cleanUp();
                        CommentSuite.db().commit();
                        group.reloadGroupItems();
                        success = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    setDisable(false);
                    if(success) { cancel.fire(); }
                }).start();
            });
        });

        setStyle(String.format("-fx-background-color: linear-gradient(to right, rgba(%s,%s,%s,0.2), transparent);", 255 * color.getRed(), 255 * color.getGreen(), 255 * color.getBlue()));
        getChildren().addAll(vbox);

        if(CommentSuite.config().autoLoadStats()) {
            reload.fire();
        }

        new Thread(() -> {
            updateLastChecked();
            while(true) {
                if(timeSince() >= 0) updateLastChecked();
                try { Thread.sleep(200); } catch (Exception e) { break; }
            }
        }).start();
    }

    private void checkChannel(String channelId) throws SQLException, IOException, YouTubeErrorException {
        if(!CommentSuite.db().channelExists(channelId)) {
            ChannelsList cl = CommentSuite.youtube().channelsList().getByChannel(ChannelsList.PART_SNIPPET, channelId, "");
            if(cl.hasItems()) {
                YouTubeChannel channel = new YouTubeChannel(cl.items[0], true);
                List<YouTubeChannel> list = new ArrayList<>();
                list.add(channel);
                CommentSuite.db().insertChannels(list);
                CommentSuite.db().commit();
            }
        }
    }

    private void updateLastChecked() {
        Platform.runLater(() -> lblChecked.setText("Last refreshed: "+sinceLastChecked()));
    }

    private long timeSince() {
        return System.currentTimeMillis() - lastChecked;
    }

    private String sinceLastChecked() {
        long time = timeSince();
        if(time > 0) {
            long m, h, d, w, y;

            time /= 1000;
            time /= 60;
            m = time % 60; time /= 60;
            h = time;
            d = h / 24;
            w = d / 7;
            y = d / 360;

            if(y > 0) {
                return y+" years ago";
            } else if(w > 0) {
                return w+" weeks ago";
            } else if(d > 0) {
                return d+" days ago";
            } else if(h > 0 || m > 0) {
                return (h > 0 ? h+" hrs ":"")+(m > 0 ? m+" mins ":"")+"ago";
            } else {
                return "just now";
            }
        }
        return "never";
    }

    private void loadAnalytics() {
        Platform.runLater(() -> {
            statsPane.setDisable(true);
            videosPane.setDisable(true);
            viewerPane.setDisable(true);
            prog.setVisible(true);
            prog.setManaged(true);
            reload.setDisable(true);
            commentSeries.getData().clear();
            videoSeries.getData().clear();
            commentCount.setText("...");
            totalLikes.setText("...");
            videoCount.setText("...");
            totalLikes.setText("...");
            totalViews.setText("...");
            popularVideos.getItems().clear();
            dislikedVideos.getItems().clear();
            commentedVideos.getItems().clear();
            disabledVideos.getItems().clear();
            popularViewers.getItems().clear();
            activeViewers.getItems().clear();
        });
        new Thread(() -> {
            try { // General Stats
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yy");
                Map<Long,Long> commentHist = CommentSuite.db().getWeekByWeekCommentHistogram(group);
                List<XYChart.Data<String,Number>> cdata = commentHist.keySet().stream()
                        .map(time -> new XYChart.Data<String,Number>(sdf.format(time), commentHist.get(time)))
                        .collect(Collectors.toList());
                Platform.runLater(() -> commentSeries.getData().addAll(cdata));

                long totalC = CommentSuite.db().getTotalComments(group);
                long totalL = CommentSuite.db().getTotalLikes(group);
                long totalVd = CommentSuite.db().getTotalVideos(group);
                long totalVw = CommentSuite.db().getTotalViews(group);
                Platform.runLater(() -> {
                    commentCount.setText(String.format("%,d", totalC));
                    totalLikes.setText("+"+String.format("%,d", totalL));
                    videoCount.setText(String.format("%,d", totalVd));
                    totalViews.setText(String.format("%,d", totalVw));
                });

                Map<Long,Long> videoHist = CommentSuite.db().getWeekByWeekVideoHistogram(group);
                List<XYChart.Data<String,Number>> vdata = videoHist.keySet().stream()
                        .map(time -> new XYChart.Data<String,Number>(sdf.format(time), videoHist.get(time)))
                        .collect(Collectors.toList());
                Platform.runLater(() -> videoSeries.getData().addAll(vdata));
            } catch (SQLException ignored) {}
            Platform.runLater(() -> statsPane.setDisable(false));

            try { // About Viewers
                List<YouTubeObjectView> mostPopular = CommentSuite.db().getMostPopularVideos(group, 10)
                        .stream().map(v -> new YouTubeObjectView(v, String.format("%,d views", v.getViews()))).collect(Collectors.toList());
                List<YouTubeObjectView> mostDisliked = CommentSuite.db().getMostDislikedVideos(group, 10)
                        .stream().map(v -> new YouTubeObjectView(v, String.format("%,d dislikes", v.getDislikes()))).collect(Collectors.toList());
                List<YouTubeObjectView> mostCommented = CommentSuite.db().getMostCommentedVideos(group, 10)
                        .stream().map(v -> new YouTubeObjectView(v, String.format("%,d comments", v.getCommentCount()))).collect(Collectors.toList());
                List<YouTubeObjectView> disabled = CommentSuite.db().getDisabledVideos(group)
                        .stream().map(v -> new YouTubeObjectView(v, "Comments Disabled")).collect(Collectors.toList());

                Platform.runLater(() -> {
                    popularVideos.getItems().addAll(mostPopular);
                    dislikedVideos.getItems().addAll(mostDisliked);
                    commentedVideos.getItems().addAll(mostCommented);
                    disabledVideos.getItems().addAll(disabled);
                });
            } catch (SQLException ignored) {ignored.printStackTrace();}
            Platform.runLater(() -> videosPane.setDisable(false));

            try { // About Videos
                LinkedHashMap<YouTubeChannel,Long> mostActive = CommentSuite.db().getMostActiveViewers(group, 25);
                List<YouTubeObjectView> ma = mostActive.keySet()
                        .stream().map(channel -> new YouTubeObjectView(channel, String.format("%,d comments", mostActive.get(channel)))).collect(Collectors.toList());
                LinkedHashMap<YouTubeChannel,Long> mostPopular = CommentSuite.db().getMostPopularViewers(group, 25);
                List<YouTubeObjectView> mp = mostPopular.keySet()
                        .stream().map(channel -> new YouTubeObjectView(channel, String.format("%,d likes", mostPopular.get(channel)))).collect(Collectors.toList());
                Platform.runLater(() -> {
                    activeViewers.getItems().addAll(ma);
                    popularViewers.getItems().addAll(mp);
                });
            } catch (SQLException ignored) {ignored.printStackTrace();}
            Platform.runLater(() -> {
                prog.setVisible(false);
                prog.setManaged(false);
                viewerPane.setDisable(false);
                reload.setDisable(false);
            });
        }).start();
    }

    private void beginGroupRefresh() {
        GroupRefresh refreshThread = new GroupRefresh(group, CommentSuite.db(), CommentSuite.youtube());
        lastChecked = System.currentTimeMillis();

        ProgressIndicator activity = new ProgressIndicator();
        activity.setMaxWidth(25);
        activity.setMaxHeight(25);
        activity.visibleProperty().bind(refreshThread.refreshingProperty());

        Label title = new Label();
        title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 16));
        title.textProperty().bind(refreshThread.refreshStatusProperty());

        Label subtitle = new Label();
        subtitle.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 13));
        subtitle.textProperty().bind(refreshThread.elapsedTimeValueProperty());

        ProgressBar progress = new ProgressBar();
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.progressProperty().bind(refreshThread.progressProperty());

        Label errorLabel = new Label();
        errorLabel.setTooltip(new Tooltip("Most often HTTP 400 errors, commonly a result of slowed or interrupted connections."));
        errorLabel.textProperty().bind(refreshThread.errorCountProperty().asString().concat(" errors"));
        errorLabel.managedProperty().bind(refreshThread.errorCountProperty().isNotEqualTo(0));
        errorLabel.visibleProperty().bind(errorLabel.managedProperty());
        errorLabel.setStyle("-fx-text-fill: #BC3F3C");
        errorLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 13));

        Label videoLabel = new Label("0 new videos");
        videoLabel.textProperty().bind(refreshThread.videosNewProperty().asString().concat(" new videos"));
        videoLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 13));

        Label commentLabel = new Label("0 new comments");
        commentLabel.textProperty().bind(refreshThread.commentsNewProperty().asString().concat(" new comments"));
        commentLabel.setFont(Font.font("Tahoma", FontWeight.BOLD, 13));

        HBox header = new HBox(10);
        header.getChildren().addAll(activity, title);

        Button finish = new Button("Finish");
        finish.disableProperty().bind(refreshThread.completedProperty().not());

        HBox hbox1 = new HBox(10);
        hbox1.setAlignment(Pos.CENTER_RIGHT);
        hbox1.getChildren().add(finish);

        VBox vbox0 = new VBox(10);
        vbox0.setPadding(new Insets(0, 0, 0, 35));
        vbox0.setAlignment(Pos.TOP_LEFT);
        vbox0.setFillWidth(true);
        vbox0.getChildren().addAll(subtitle, progress, errorLabel, videoLabel, commentLabel, hbox1);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(25));
        vbox.setMaxWidth(350);
        vbox.setMaxHeight(0);
        vbox.setAlignment(Pos.CENTER);
        vbox.setId("overlayMenu");
        vbox.getChildren().addAll(header, vbox0);

        StackPane overlay = new StackPane(vbox);
        overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");
        getChildren().addAll(overlay);

        finish.setOnAction(ae -> {
            lastChecked = CommentSuite.db().getLastChecked(group);
            finish.disableProperty().unbind();
            title.textProperty().unbind();
            progress.progressProperty().unbind();
            errorLabel.textProperty().unbind();
            errorLabel.managedProperty().unbind();
            errorLabel.visibleProperty().unbind();
            videoLabel.textProperty().unbind();
            commentLabel.textProperty().unbind();
            subtitle.textProperty().unbind();
            activity.visibleProperty().unbind();
            reload.fire();
            Platform.runLater(() -> getChildren().remove(overlay));
        });

        refreshThread.start();
    }
}
