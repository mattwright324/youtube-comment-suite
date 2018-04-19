package mattw.youtube.commentsuite;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.commentsuite.db.*;
import mattw.youtube.commentsuite.io.Browser;
import mattw.youtube.commentsuite.io.Clipboards;
import mattw.youtube.datav3.resources.CommentsList;

import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class SearchCommentsPane extends StackPane {

    public static final Image IMG_BLANK_PROFILE = new Image("/mattw/youtube/commentsuite/img/blankProfile.png");
    public static final Image IMG_VID_PLACEHOLDER = new Image("/mattw/youtube/commentsuite/img/videoPlaceholder.png");

    private ComboBox<Group> group;
    private ComboBox<GroupItem> groupItem;
    private ChangeListener<Number> cl;
    private CommentDatabase.CommentQuery query;
    private Hyperlink select = new Hyperlink();
    private StackPane videoSelect = buildVideoSelectPane();
    private TextField videoKeyword;
    private ComboBox<String> videoOrder;
    private String[] vOrderBy = new String[]{"publish_date DESC","video_title ASC","total_views DESC"};
    private YouTubeVideo filterVideo = null;
    private ListView<YouTubeVideoView> videoBox;
    private ObservableList<YouTubeCommentView> originalComments = FXCollections.observableArrayList();
    private ObservableList<YouTubeCommentView> treeComments = FXCollections.observableArrayList();
    private ListView<YouTubeCommentView> commentsList = new ListView<>();
    private YouTubeCommentView actionComment = null;
    private Button showMore = new Button(), reply = new Button(), viewTree = new Button();
    private SimpleStringProperty commentsGroupId = new SimpleStringProperty(Group.NO_GROUP);
    private SimpleStringProperty selectedVideoId = new SimpleStringProperty("");
    private YouTubeVideo selectedVideo = null;

    private class UpdateVideoSelect {
        UpdateVideoSelect(GroupItem nv) {
            new Thread(() -> {
                List<YouTubeVideoView> videos = new ArrayList<>();
                if(nv.getYouTubeId().equals(GroupItem.ALL_ITEMS)) {
                    System.out.println("Update videos list; Group: "+videos.size());
                    try {
                        videos.addAll(CommentSuite.db().getVideos(group.getValue(),videoKeyword.getText(),vOrderBy[videoOrder.getSelectionModel().getSelectedIndex()],200)
                                .stream().map(v -> new YouTubeVideoView(v)).collect(Collectors.toList()));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else if(!nv.getYouTubeId().equals(GroupItem.NO_ITEMS)) {
                    System.out.println("Update videos list; Gitem: "+videos.size());
                    try {
                        videos.addAll(CommentSuite.db().getVideos(groupItem.getValue(),videoKeyword.getText(),vOrderBy[videoOrder.getSelectionModel().getSelectedIndex()],200)
                                .stream().map(v -> new YouTubeVideoView(v)).collect(Collectors.toList()));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                Platform.runLater(() -> {
                    videoBox.getItems().clear();
                    videoBox.getItems().addAll(videos);
                });
            }).start();
        }
    }

    private StackPane buildVideoSelectPane() {
        Button search = new Button("Search");
        search.setOnAction(ae -> new UpdateVideoSelect(groupItem.getValue()));

        Label title = new Label("Select a Video");
        title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

        videoKeyword = new TextField();
        videoKeyword.setPromptText("Keywords...");
        HBox.setHgrow(videoKeyword, Priority.ALWAYS);

        videoOrder = new ComboBox<>();
        videoOrder.getItems().addAll("By Date", "By Title", "By Views");
        videoOrder.getSelectionModel().select(0);

        HBox hbox0 = new HBox(10);
        hbox0.getChildren().addAll(search, videoKeyword, videoOrder);
        hbox0.setOnKeyReleased(ke -> {
            if(ke.getCode().equals(KeyCode.ENTER)) {
                search.fire();
            }
        });

        Label selected = new Label("Selected: (All Videos)");
        select.textProperty().bind(selected.textProperty());
        selected.setMinWidth(0);
        selected.setMaxWidth(Double.MAX_VALUE);
        selected.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 14));
        HBox.setHgrow(selected, Priority.ALWAYS);

        int wh = 25;
        ColorAdjust monochrome = new ColorAdjust();
        monochrome.setBrightness(1.0);
        Blend blush = new Blend(BlendMode.MULTIPLY, monochrome, new ColorInput(0, 0, wh, wh, Color.CORNFLOWERBLUE));

        ImageView clip = new ImageView(new Image("/mattw/youtube/commentsuite/img/close.png"));
        clip.setFitHeight(wh);
        clip.setFitWidth(wh);

        ImageView iv = new ImageView(new Image("/mattw/youtube/commentsuite/img/close.png"));
        iv.setFitHeight(wh);
        iv.setFitWidth(wh);
        iv.setEffect(blush);
        iv.setCache(true);
        iv.setCacheHint(CacheHint.SPEED);
        iv.setClip(clip);

        Hyperlink deselect = new Hyperlink();
        deselect.setGraphic(iv);
        deselect.setTooltip(new Tooltip("Deselect this video."));
        deselect.setDisable(true);
        deselect.setOnAction(ae -> {
            Platform.runLater(() -> {
                filterVideo = null;
                videoBox.getSelectionModel().clearSelection();
                deselect.setDisable(true);
                selected.setText("Selected: (All Videos)");
            });
        });

        HBox hbox2 = new HBox(5);
        hbox2.setPadding(new Insets(0,0,0,10));
        hbox2.setAlignment(Pos.CENTER_LEFT);
        hbox2.setStyle("-fx-background-color: lightgray; -fx-background-radius: 5px;");
        hbox2.getChildren().addAll(selected, deselect);

        videoBox = new ListView<>();
        videoBox.getSelectionModel().selectedIndexProperty().addListener((o, ov, nv) -> {
            if(nv.intValue() != -1) {
                Platform.runLater(() -> {
                    filterVideo = videoBox.getSelectionModel().getSelectedItem().getVideo();
                    selected.setText("Selected: ("+filterVideo.getTitle()+")");
                    deselect.setDisable(false);
                });
            }
        });
        VBox.setVgrow(videoBox, Priority.ALWAYS);

        Button close = new Button("Save & Close");
        close.setOnAction(ae -> Platform.runLater(() -> getChildren().remove(videoSelect)));

        HBox hbox1 = new HBox(close);
        hbox1.setAlignment(Pos.CENTER_RIGHT);

        VBox vbox = new VBox(10);
        vbox.setMinWidth(450);
        vbox.setPadding(new Insets(25));
        vbox.setId("overlayMenu");
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().addAll(title, hbox0, hbox2, videoBox, hbox1);

        StackPane overlay = new StackPane(vbox);
        overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");
        overlay.setPadding(new Insets(25));
        return overlay;
    }

    public SearchCommentsPane(OAuth2Handler oauth2) {
        Config config = CommentSuite.config();

        ImageView videoThumb = new ImageView(IMG_VID_PLACEHOLDER);
        videoThumb.setFitWidth(300);
        videoThumb.setFitHeight(168);
        videoThumb.setCursor(Cursor.HAND);
        videoThumb.setOnMouseClicked(me -> {
            if(selectedVideo != null) {
                Browser.open(selectedVideo.getYouTubeLink());
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
                Browser.open(selectedVideo.getChannel().getYouTubeLink());
            }
        });

        TextField videoAuthor = new TextField("mattwright324");
        videoAuthor.setMinWidth(0);
        videoAuthor.setMaxWidth(Double.MAX_VALUE);
        videoAuthor.setEditable(false);
        videoAuthor.setId("context");
        videoAuthor.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 15));

        Label views = new Label("8.7M views");

        ImageView ivLikes = new ImageView(new Image("/mattw/youtube/commentsuite/img/thumbs-up.png"));
        ivLikes.setFitHeight(20);
        ivLikes.setFitWidth(20);

        Label likes = new Label("8.6K");

        ImageView ivDislikes = new ImageView(new Image("/mattw/youtube/commentsuite/img/thumbs-down.png"));
        ivDislikes.setFitHeight(20);
        ivDislikes.setFitWidth(20);

        Label dislikes = new Label("309");

        HBox likeBox = new HBox(5);
        likeBox.getChildren().addAll(ivLikes, likes, ivDislikes, dislikes);

        VBox stats = new VBox();
        stats.setMinWidth(120);
        stats.setPrefWidth(120);
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
        menu.getItems().addAll(openBrowser, copyName, copyText, copyChannelLink, copyVideoLink, copyCommentLink);

        commentsList.setItems(originalComments);
        commentsList.setId("listView");
        commentsList.setCellFactory(cf -> new ListViewEmptyCellFactory<>(96));
        commentsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        commentsList.setContextMenu(menu);
        commentsList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if(nv != null) {
                loadThumb.fire();
                Platform.runLater(() -> selectedVideoId.setValue(nv.getComment().getVideoId()));
            }
        });
        VBox.setVgrow(commentsList, Priority.ALWAYS);

        selectedVideoId.addListener((o, ov, nv) -> new Thread(() -> {
            try {
                this.selectedVideo = CommentSuite.db().getVideo(nv);
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
                        views.setText(String.format("%s views", formatNumber(selectedVideo.getViews(),1)));
                        likes.setText(formatNumber(selectedVideo.getLikes(),1));
                        dislikes.setText(formatNumber(selectedVideo.getDislikes(),1));
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
                Browser.open(link);
            }
        });

        loadThumb.setOnAction(ae -> {
            loadThumb.setDisable(true);
            new Thread(() -> {
                for(YouTubeCommentView yc : commentsList.getSelectionModel().getSelectedItems()) {
                    if(yc != null) yc.updateProfileThumb();
                }
                for(YouTubeCommentView yc : commentsList.getItems()) {
                    if(yc != null) yc.checkProfileThumb();
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
            commentsList.scrollTo(actionComment);
            backToResults.setVisible(false);
            backToResults.setManaged(false);
        }));

        SimpleIntegerProperty queryUpdate = new SimpleIntegerProperty(0);
        SimpleIntegerProperty pageNum = new SimpleIntegerProperty(1);
        SimpleIntegerProperty lastPageNum = new SimpleIntegerProperty(1);

        Button firstPage = new Button();
        firstPage.disableProperty().bind(backToResults.managedProperty().or(pageNum.isEqualTo(1)));
        firstPage.setTooltip(new Tooltip("First Page"));
        ImageView fpView = new ImageView(new Image("/mattw/youtube/commentsuite/img/angle-double-left.png"));
        fpView.setFitHeight(22);
        fpView.setFitWidth(22);
        firstPage.setGraphic(fpView);

        Button prevPage = new Button();
        prevPage.disableProperty().bind(backToResults.managedProperty().or(pageNum.isEqualTo(1)));
        prevPage.setTooltip(new Tooltip("Previous Page"));
        ImageView ppView = new ImageView(new Image("/mattw/youtube/commentsuite/img/angle-left.png"));
        ppView.setFitHeight(22);
        ppView.setFitWidth(22);
        prevPage.setGraphic(ppView);

        Button nextPage = new Button();
        nextPage.disableProperty().bind(backToResults.managedProperty().or(pageNum.greaterThanOrEqualTo(lastPageNum)));
        nextPage.setTooltip(new Tooltip("Next Page"));
        ImageView npView = new ImageView(new Image("/mattw/youtube/commentsuite/img/angle-right.png"));
        npView.setFitHeight(22);
        npView.setFitWidth(22);
        nextPage.setGraphic(npView);

        Button lastPage = new Button();
        lastPage.disableProperty().bind(backToResults.managedProperty().or(pageNum.greaterThanOrEqualTo(lastPageNum)));
        lastPage.setTooltip(new Tooltip("Last Page"));
        ImageView lpView = new ImageView(new Image("/mattw/youtube/commentsuite/img/angle-double-right.png"));
        lpView.setFitHeight(22);
        lpView.setFitWidth(22);
        lastPage.setGraphic(lpView);

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

        groupItem = new ComboBox<>();
        groupItem.setId("control");
        groupItem.setDisable(true);
        groupItem.setMaxWidth(Double.MAX_VALUE);

        group = new ComboBox<>();
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
                    group.setItems(CommentSuite.db().globalGroupList);
                    group.setValue(nv);
                });
            }
            if(!group.isDisabled() && nv != null) {
                nv.itemsUpdatedProperty().addListener(cl = (o1, ov1, nv1) -> {
                    GroupItem allItems = new GroupItem(GroupItem.ALL_ITEMS, "All items ("+nv.getGroupItems().size()+")");
                    Platform.runLater(() -> {
                        videoSelect = buildVideoSelectPane();
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
                nv.reloadGroupItems();
            }
        });
        groupItem.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            groupItem.setDisable(nv == null || nv.getYouTubeId().equals(GroupItem.NO_ITEMS));
            videoSelect = buildVideoSelectPane();
        });
        group.setItems(CommentSuite.db().globalGroupList);
        group.getItems().addListener((ListChangeListener<Group>) c -> {
            if(group.getSelectionModel().getSelectedIndex() == -1 && group.getItems() != null && group.getItems().size() > 0) {
                group.getSelectionModel().select(0);
            }
        });
        if(group.getSelectionModel().getSelectedIndex() == -1 && group.getItems() != null && group.getItems().size() > 0) {
            group.getSelectionModel().select(0);
        }

        select.setOnAction(ae -> {
            if(!getChildren().contains(videoSelect)) {
                new UpdateVideoSelect(groupItem.getValue());
                Platform.runLater(() -> getChildren().add(videoSelect));
            }
        });

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
        searchBox.getChildren().addAll(label1, group, groupItem, select, label2, grid, hbox2);
        searchBox.setOnKeyPressed(ke -> {
            if(ke.getCode().equals(KeyCode.ENTER)) {
                search.fire();
            }
        });

        HBox hbox = new HBox();
        hbox.setFillHeight(true);
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().addAll(contextBox, contextToggle, commentsBox, searchBox);

        class RunQuery {
            RunQuery(int toPage) {
                new Thread(() -> {
                    searchBox.setDisable(true);
                    hbox0.setDisable(true);
                    try {
                        Group g = group.getValue();
                        GroupItem gi = groupItem.getValue().getYouTubeId().equals(GroupItem.ALL_ITEMS) ? null : groupItem.getValue();
                        List<YouTubeVideo> videos = null;
                        if(filterVideo != null) {
                            videos = new ArrayList<>();
                            videos.add(filterVideo);
                        }
                        List<YouTubeCommentView> commentViews = query.get(toPage, g, gi, videos)
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
                    hbox0.setDisable(false);
                }).start();
            }
        }

        firstPage.setOnAction(ae -> new RunQuery(1));
        nextPage.setOnAction(ae -> new RunQuery(query.getPage()+1));
        prevPage.setOnAction(ae -> new RunQuery(query.getPage()-1));
        lastPage.setOnAction(ae -> new RunQuery(query.getPageCount()));
        search.setOnAction(ae -> new Thread(() -> {
            searchBox.setDisable(true);
            hbox0.setDisable(true);
            query = CommentSuite.db().commentQuery()
                    .limit(500)
                    .ctype(type.getSelectionModel().getSelectedIndex())
                    .after(getDatePickerDate(dateFrom, false).getTime())
                    .before(getDatePickerDate(dateTo, true).getTime())
                    .orderBy(orderBy.getSelectionModel().getSelectedIndex())
                    .textLike(textLike.getText())
                    .nameLike(nameLike.getText());
            new RunQuery(1);
        }).start());

        getChildren().add(hbox);
        setPadding(new Insets(0));

        showMore.setOnAction(ae -> {
            commentsList.getSelectionModel().select(actionComment);
            loadThumb.fire();

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
            getChildren().addAll(overflow);

            close.setOnAction(ae2 -> getChildren().remove(overflow));
        });

        reply.setOnAction(ae -> {
            commentsList.getSelectionModel().select(actionComment);
            loadThumb.fire();

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
            if(config.prefixReplies()) {
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
            getChildren().addAll(overflow);

            close.setOnAction(ae2 -> getChildren().remove(overflow));

            reply.setOnAction(ae2 -> {
                try {
                    oauth2.setTokens(replyAs.getValue().getTokens());
                    CommentsList.Item item = oauth2.postReply(actionComment.getComment().getYouTubeId(), textArea.getText());
                    YouTubeComment replyMade = new YouTubeComment(item, actionComment.getComment().getVideoId());
                    List<YouTubeComment> list = new ArrayList<>();
                    list.add(replyMade);
                    CommentSuite.db().insertComments(list);
                    CommentSuite.db().commit();
                    commentsList.getItems().add(new YouTubeCommentView(replyMade, commentsList.getItems().equals(treeComments)));
                    commentsList.scrollTo(actionComment);
                    close.fire();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        viewTree.setOnAction(ae -> {
            actionComment.updateProfileThumb();
            loadThumb.fire();
            try {
                String parentId = actionComment.getComment().isReply() ? actionComment.getComment().getParentId() : actionComment.getComment().getYouTubeId();
                treeComments.addAll(CommentSuite.db().getCommentTree(parentId)
                        .stream().map(c -> new YouTubeCommentView(c, false)).collect(Collectors.toList()));
                Platform.runLater(() -> {
                    commentsList.setItems(treeComments);
                    backToResults.setManaged(true);
                    backToResults.setVisible(true);
                });
            } catch (Exception ignored) {}
        });
    }

    public void runQuery(int page) {

    }

    private String formatNumber(double number, int precision) {
        if(number < 1000) {
            return Integer.toString((int) number);
        }
        char[] c = new char[]{'\0','K','M','B','T'};
        int level = 0;
        while(number > 1000) {
            level++;
            number /= 1000.0;
        }
        return String.format("%."+precision+"f%s", number, c[level]);
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
}
