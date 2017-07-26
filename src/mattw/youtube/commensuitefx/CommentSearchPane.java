package mattw.youtube.commensuitefx;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

public class CommentSearchPane extends HBox implements EventHandler<ActionEvent> {

    private DatabaseManager.CommentQuery query;
    private Button nextPage;
    private Button prevPage;
    private Button firstPage;
    private Button lastPage;
    private TextField pageNum;
    private int page = 1;

    private VBox commentResults;
    private ScrollPane scroll;
    private double vValue = 0.0;
    private ImageView thumbnail;
    private ImageView authorThumb;
    private TextField title;
    private TextField author;
    private Label views;
    private Label likes;
    private Label dislikes;
    private Label resultCount;
    private TextArea description;
    private ChoiceBox<Group> group;
    private ChoiceBox<GitemType> groupItem;
    private ToggleButton videoContext;
    private TextField userLike;
    private TextField textLike;
    private Button find;
    private Button backToResults;
    private ComboBox<String> type;
    private ComboBox<String> orderBy;
    private List<CommentResult> results;
    private List<CommentResult> tree;

    public void handle(ActionEvent ae) {

    }

    public CommentSearchPane() {
        setAlignment(Pos.TOP_LEFT);

        thumbnail = new ImageView(CommentSuiteFX.PLACEHOLDER);
        thumbnail.setFitHeight(180);
        thumbnail.setFitWidth(320);

        title = new TextField("YouTube Comment Suite");
        title.setId("context");
        title.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        title.setEditable(false);

        authorThumb = new ImageView(CommentResult.BLANK_PROFILE);
        authorThumb.setFitHeight(24);
        authorThumb.setFitWidth(24);

        author = new TextField("Guest");
        author.setId("context");
        author.setEditable(false);
        HBox.setHgrow(author, Priority.ALWAYS);

        views = new Label("8675309 views");
        views.setAlignment(Pos.CENTER_RIGHT);

        likes = new Label("+ 8675");
        likes.setAlignment(Pos.CENTER_RIGHT);
        likes.setStyle("-fx-text-fill: green");
        dislikes = new Label("- 309");
        dislikes.setAlignment(Pos.CENTER_RIGHT);
        dislikes.setStyle("-fx-text-fill: red");

        description = new TextArea("Published Nov 18, 1918  This is an example description. You may select this text, the title, and author's nameProperty. Right click to copy or select all."
                + "\n\nThe thumbnail and author's picture are clickable to open either the video or channel in your browser."
                + "\n\nComments may be replied to if you are signed in. Commentor names may be clicked to open their channel in browser."
                + "\n\nNote that grabbed comment numbers may be slightly off due to YouTube spam detection and the channel's user and phrase filters.");
        description.setEditable(false);
        description.setWrapText(true);
        description.setMaxWidth(320);
        description.setMaxHeight(Double.MAX_VALUE);
        description.setPrefWidth(320);
        VBox.setVgrow(description, Priority.ALWAYS);

        HBox likeDislike = new HBox(5);
        likeDislike.getChildren().addAll(likes, dislikes);

        VBox stats = new VBox();
        stats.setFillWidth(true);
        stats.getChildren().addAll(views, likeDislike);

        HBox publisher = new HBox(5);
        publisher.setAlignment(Pos.CENTER_LEFT);
        publisher.getChildren().addAll(authorThumb, author, stats);

        VBox context = new VBox(5);
        context.setPadding(new Insets(0,10,5,5));
        context.setFillWidth(true);
        context.setAlignment(Pos.TOP_CENTER);
        context.setMinWidth(330);
        context.setMaxWidth(330);
        context.setPrefWidth(330);
        context.getChildren().addAll(thumbnail, title, publisher, description);

        commentResults = new VBox(5);
        commentResults.setAlignment(Pos.TOP_CENTER);
        commentResults.setFillWidth(true);

        scroll = new ScrollPane(commentResults);
        scroll.setMaxWidth(Double.MAX_VALUE);
        scroll.setFitToWidth(true);

        Button clearComments = new Button("Clear");
        clearComments.setOnAction(e -> {
            Platform.runLater(()->{
                commentResults.getChildren().clear();
                backToResults.setDisable(true);
                resultCount.setText("Showing 0 out of 0 results.");
            });
            results.clear();
        });

        nextPage = new Button(">");
        nextPage.setDisable(true);
        nextPage.setOnAction(e -> {
            if(query != null && page+1 <= query.getPageCount()) {
                try {
                    loadQueryPage(++page);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });
        prevPage = new Button("<");
        prevPage.setDisable(true);
        prevPage.setOnAction(e -> {
            if(query != null && page-1 >= 1) {
                try {
                    loadQueryPage(--page);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });
        firstPage = new Button("<<");
        firstPage.setDisable(true);
        firstPage.setOnAction(e -> {
            if(query != null && page-1 >= 1) {
                try {
                    loadQueryPage(1);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });
        lastPage = new Button(">>");
        lastPage.setDisable(true);
        lastPage.setOnAction(e -> {
            if(query != null && page-1 >= 1) {
                try {
                    loadQueryPage(query.getPageCount());
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });
        pageNum = new TextField();
        pageNum.setEditable(false);
        pageNum.textProperty().addListener((observable, oldValue, newValue) -> pageNum.setPrefWidth(pageNum.getText().length() * 7.5));
        pageNum.setText(" Page 1 of 0 ");

        HBox box = new HBox();
        box.getChildren().addAll(firstPage, prevPage, pageNum, nextPage, lastPage);

        resultCount = new Label("Showing 0 out of 0 results.");

        backToResults = new Button("Return to Results");
        backToResults.setStyle("-fx-base: seagreen");
        backToResults.setDisable(true);
        backToResults.setOnAction(e -> returnToResults());

        HBox resultControls = new HBox(5);
        resultControls.setPadding(new Insets(5,0,0,0));
        resultControls.setAlignment(Pos.CENTER);
        resultControls.getChildren().addAll(clearComments, backToResults, box);

        VBox resultBox = new VBox(5);
        resultBox.setFillWidth(true);
        resultBox.setAlignment(Pos.TOP_CENTER);
        resultBox.setPadding(new Insets(0,0,5,0));
        resultBox.getChildren().addAll(scroll, resultControls, resultCount);

        Label label1 = new Label("Select Group");
        label1.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 16));
        label1.setAlignment(Pos.CENTER);

        group = new ChoiceBox<>(CommentSuiteFX.getApp().groupsList);
        group.setMaxWidth(300);
        group.setPrefWidth(300);
        group.setOnAction(e -> {
            Task<Void> task = new Task<Void>() {
                protected Void call() throws Exception {
                    try {
                        group.setDisable(true);
                        groupItem.setDisable(true);
                        loadCGroup(group.getValue());
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    group.setDisable(false);
                    groupItem.setDisable(false);
                    return null;
                }
            };
            new Thread(task).start();
        });
        groupItem = new ChoiceBox<>();
        groupItem.setMaxWidth(300);
        groupItem.setPrefWidth(300);

        videoContext = new ToggleButton("Show Video Context");
        videoContext.setTooltip(new Tooltip("Context appears when you select a comment."));
        videoContext.setOnAction(e -> {
            if(!videoContext.isSelected()) {
                context.setVisible(false);
                context.setMinWidth(0);
                context.setMaxWidth(0);
                context.setPrefWidth(0);
            } else {
                context.setVisible(true);
                context.setMinWidth(330);
                context.setMaxWidth(330);
                context.setPrefWidth(330);
            }
        });
        videoContext.setMaxWidth(175);
        videoContext.fire();

        Label label2 = new Label("Restrict Results");
        label2.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 16));
        label2.setAlignment(Pos.CENTER);

        type = new ComboBox<>();
        type.setMaxWidth(Double.MAX_VALUE);
        type.getItems().addAll("Comments and Replies", "Comments Only", "Replies Only");
        type.getSelectionModel().select(0);

        orderBy = new ComboBox<>();
        orderBy.setMaxWidth(Double.MAX_VALUE);
        orderBy.getItems().addAll("Most Recent", "Least Recent", "Most Likes", "Most Replies", "Longest Comment", "Names (A to Z)", "Comments (A to Z)");
        orderBy.getSelectionModel().select(0);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setVgap(10);
        grid.setHgap(10);
        ColumnConstraints cc1 = new ColumnConstraints();
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setFillWidth(true);
        grid.getColumnConstraints().addAll(cc1, cc2);

        grid.addRow(0, new Label("Sort by: "), orderBy);

        userLike = new TextField();
        userLike.setPromptText("Username contains...");
        GridPane.setHgrow(userLike, Priority.ALWAYS);
        grid.addRow(1, new Label("Name like: "), userLike);

        textLike = new TextField();
        textLike.setPromptText("Comment contains...");
        GridPane.setHgrow(textLike, Priority.ALWAYS);
        grid.addRow(2, new Label("Text like: "), textLike);

        DatePicker afterDate = new DatePicker();
        setDatePickerTime(afterDate, 0);
        grid.addRow(3, new Label("Date from: "), afterDate);

        DatePicker beforeDate = new DatePicker();
        setDatePickerTime(beforeDate, System.currentTimeMillis());
        grid.addRow(4, new Label("Date To: "), beforeDate);

        find = new Button("Find Comments");
        find.setMaxWidth(175);
        find.setOnAction(e -> {
            CommentSuiteFX.setNodesDisabled(true, find, beforeDate, afterDate, textLike, userLike, orderBy, type, group, groupItem);
            Task<Void> task = new Task<Void>() {
                protected Void call() throws Exception {
                    try {
                        if(group.getValue() != null && orderBy.getValue() != null) {
                            int groupId = group.getValue().group_id;
                            int order = orderBy.getSelectionModel().getSelectedIndex();
                            String user = userLike.getText();
                            String text = textLike.getText();
                            int limit = 250;
                            GitemType gitem = groupItem.getValue().getGitemId() != -1 ? groupItem.getValue() : null;
                            int comment_type =  type.getSelectionModel().getSelectedIndex();
                            query = CommentSuiteFX.getDatabase().newCommentQuery()
                                    .group(groupId)
                                    .groupItem(gitem)
                                    .orderBy(order)
                                    .nameLike(user)
                                    .textLike(text)
                                    .limit(limit)
                                    .after(getDatePickerDate(afterDate, false))
                                    .before(getDatePickerDate(beforeDate, true))
                                    .cType(comment_type);
                            loadQueryPage(1);
                        }
                        CommentSuiteFX.setNodesDisabled(false, find, beforeDate, afterDate, textLike, userLike, orderBy, type, group, groupItem);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            new Thread(task).start();
        });

        VBox searchBox = new VBox(10);
        searchBox.setMinWidth(320);
        searchBox.setMaxWidth(320);
        searchBox.setPrefWidth(320);
        searchBox.setPadding(new Insets(0,10,5,10));
        searchBox.setAlignment(Pos.TOP_CENTER);
        searchBox.setFillWidth(true);
        searchBox.getChildren().addAll(label1, group, groupItem, videoContext, label2, type, grid, find);
        searchBox.setOnKeyPressed(ke -> {
            if(ke.getCode().equals(KeyCode.ENTER)) find.fire();
        });

        getChildren().addAll(context, resultBox, searchBox);
        HBox.setHgrow(resultBox, Priority.ALWAYS);
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

    private void loadQueryPage(int page) throws SQLException {
        this.page = page;
        Platform.runLater(() -> CommentSuiteFX.setNodesDisabled(true, find, prevPage, nextPage, firstPage, lastPage));
        final List<CommentResult> list = query.get(page).stream()
                .map(c -> new CommentResult(c, true))
                .collect(Collectors.toList());
        results = list;
        Platform.runLater(() -> {
            prevPage.setDisable(page == 1);
            nextPage.setDisable(page == query.getPageCount());
            firstPage.setDisable(page == 1);
            lastPage.setDisable(page == query.getPageCount());
            pageNum.setText(" Page "+page+" of "+query.getPageCount()+" ");
            resultCount.setText("Showing "+list.size()+" out of "+query.getTotalResults()+" results.");
            commentResults.getChildren().clear();
            commentResults.getChildren().addAll(list);
            find.setDisable(false);
            backToResults.setDisable(true);
            vValue = 0;
            scroll.layout();
            scroll.setVvalue(vValue);
            CommentSuiteFX.setNodesDisabled(false, find);
        });
    }

    private void returnToResults() {
        backToResults.setDisable(true);
        commentResults.getChildren().clear();
        commentResults.getChildren().addAll(results);
        scroll.layout();
        scroll.setVvalue(vValue);
    }

    public void viewTree(CommentType comment) throws SQLException {
        vValue = scroll.getVvalue();
        tree = CommentSuiteFX.getDatabase().getCommentTree(comment.isReply() ? comment.getParentId() : comment.getId()).stream()
                .map(c -> new CommentResult(c, false))
                .collect(Collectors.toList());
        commentResults.getChildren().clear();
        commentResults.getChildren().addAll(tree);
        find.setDisable(false);
        backToResults.setDisable(false);
        scroll.layout();
        scroll.setVvalue(0);
    }

    public void loadContext(String videoId) throws SQLException {
        VideoType video = CommentSuiteFX.getDatabase().getVideo(videoId, true);
        thumbnail.setImage(video.fetchThumb());
        thumbnail.setCursor(Cursor.HAND);
        thumbnail.setOnMouseClicked(e -> {
            if(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 1) {
                CommentSuiteFX.openInBrowser(video.getYoutubeLink());
            }
        });
        ChannelType channel = DatabaseManager.getChannel(video.getChannelId());
        authorThumb.setImage(channel.fetchThumb() != null ? channel.fetchThumb() : CommentResult.BLANK_PROFILE);
        authorThumb.setCursor(Cursor.HAND);
        authorThumb.setOnMouseClicked(e -> {
            if(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 1) {
                CommentSuiteFX.openInBrowser(video.getYoutubeLink());
            }
        });
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
        description.setText("Published on "+sdf.format(video.getPublishDate())+"  "+video.getDescription());
        title.setText(video.getTitle());
        author.setText(channel.getTitle());
        likes.setText("+"+video.getLikes());
        dislikes.setText("-"+video.getDislikes());
        views.setText(video.getViews()+" views");
    }

    private void loadCGroup(Group g) throws SQLException {
        final List<GitemType> items = CommentSuiteFX.getDatabase().getGitems(g.group_id, false);
        Platform.runLater(() -> {
            groupItem.getItems().clear();
            groupItem.getItems().add(new GitemType(-1, "All Items ("+items.size()+")"));
            groupItem.getItems().addAll(items);
            groupItem.getSelectionModel().select(0);
            // Not smart to load all relevant videos into another ChoiceBox, too slow and list has potential to be gigantic.
        });
    }

    public void refreshResultProfiles() {
        if(results != null) {
            for(CommentResult comment : results) {
                comment.refreshImage();
            }
        }
        if(tree != null) {
            for(CommentResult comment : tree) {
                comment.refreshImage();
            }
        }
    }

}
