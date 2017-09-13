package mattw.youtube.commentsuitefx;

import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.SearchList;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class YoutubeSearchPane extends GridPane implements EventHandler<ActionEvent> {

    private StackPane addGroup;
    private String pageToken = "";
    private Label resultStatus;
    private VBox searchResults;
    private TextField locField, searchField;
    private Button search, selectAll, clearResults, addToGroup, nextPage;
    private ComboBox<String> locDistance, searchMethod, searchOrder, searchType;

    public void handle(ActionEvent ae) {
        Object o = ae.getSource();
        if(o.equals(search) || o.equals(nextPage)) {
            Task<Void> task = new Task<Void>() {
                protected Void call() {
                    CommentSuiteFX.setNodesDisabled(true, search, nextPage, searchField, searchMethod, locField, locDistance, searchOrder, searchType);
                    try {
                        String escaped_search = URLEncoder.encode(searchField.getText(), "UTF-8");
                        String order = searchOrder.getSelectionModel().getSelectedItem().toLowerCase();
                        String type = "";
                        if(searchType.getSelectionModel().getSelectedIndex() != 0) type = searchType.getSelectionModel().getSelectedItem().toLowerCase();
                        int method = searchMethod.getSelectionModel().getSelectedIndex();
                        String location = locField.getText();
                        String distance = locDistance.getSelectionModel().getSelectedItem();
                        String token = "";
                        if(o.equals(search)) {
                            Platform.runLater(() -> searchResults.getChildren().clear());
                        } else {
                            token = pageToken;
                        }
                        SearchList sl;
                        if(method == 0) {
                            sl = CommentSuiteFX.getYoutube().searchList()
                                    .maxResults(25)
                                    .order(order)
                                    .get(SearchList.PART_SNIPPET, escaped_search, type, token);
                        } else {
                            sl = CommentSuiteFX.getYoutube().searchList()
                                    .maxResults(25)
                                    .order(order)
                                    .getByLocation(SearchList.PART_SNIPPET, escaped_search, token, location, distance);
                        }
                        for(SearchList.Item item : sl.items) {
                            final SearchResult result = new SearchResult(item);
                            Platform.runLater(() -> {
                                searchResults.getChildren().add(result);
                                CommentSuiteFX.setNodesDisabled(false, selectAll, clearResults, addToGroup);
                                resultStatus.setText("Showing "+searchResults.getChildren().size()+" out of "+sl.pageInfo.totalResults+" results.");
                            });
                        }
                        if(sl.nextPageToken != null) {
                            CommentSuiteFX.setNodesDisabled(false, nextPage);
                            pageToken = sl.nextPageToken;
                        }
                    } catch (JsonSyntaxException | IOException | YouTubeErrorException e) {
                        Platform.runLater(() -> resultStatus.setText(e.getMessage()));
                        e.printStackTrace();
                    }
                    CommentSuiteFX.setNodesDisabled(false, search, searchField, searchMethod, locField, locDistance, searchOrder);
                    if(searchMethod.getSelectionModel().getSelectedIndex() == 0) searchType.setDisable(false);
                    return null;
                }
            };
            Thread thread = new Thread(task);
            thread.start();
        } else if(o.equals(selectAll)) {
            ObservableList<Node> list = searchResults.getChildren();
            boolean allSelected = true;
            for(Node n : list) {
                if(n instanceof SearchResult) {
                    SearchResult r = (SearchResult) n;
                    if(!r.isSelected()) {
                        allSelected = false;
                        break;
                    }
                }
            }
            for(Node n : list) {
                if(n instanceof SearchResult) {
                    SearchResult r = (SearchResult) n;
                    r.setSelected(!allSelected);
                }
            }
        } else if(o.equals(clearResults)) {
            CommentSuiteFX.setNodesDisabled(true, selectAll, clearResults, addToGroup, nextPage, resultStatus);
            searchResults.getChildren().clear();
        } else if(o.equals(addToGroup)) {
            if(!CommentSuiteFX.getMainPane().getChildren().contains(addGroup)) {
                addGroup = createAddToGroupPane();
                CommentSuiteFX.getMainPane().getChildren().add(addGroup);
            }
        }
    }

    public YoutubeSearchPane() {
        setPadding(new Insets(5,5,5,5));
        setId("videoPane");
        setAlignment(Pos.TOP_CENTER);
        setHgap(5);
        setVgap(10);

        GridPane btnGrid = new GridPane();
        btnGrid.setHgap(5);
        btnGrid.setVgap(10);

        locField = new TextField();
        locField.setPromptText("40.7058253,-74.1180864");

        locDistance = new ComboBox<>();
        locDistance.getItems().addAll("1km", "2km", "5km", "10km", "15km", "20km", "25km", "30km", "50km", "75km", "100km", "200km", "500km", "1000km");
        locDistance.getSelectionModel().select(3);

        searchMethod = new ComboBox<>();
        searchMethod.getItems().addAll("Normal", "Location");
        searchMethod.setOnAction(e -> {
            String method = searchMethod.getSelectionModel().getSelectedItem();
            ObservableList<Node> list = getChildren();
            if(method.equals("Normal")) {
                if(list.contains(locField)) {
                    list.removeAll(locField, locDistance, search, searchOrder, searchType);
                    add(search, 2, 0);
                    add(searchOrder, 3, 0);
                    add(searchType, 4, 0);
                    searchType.setDisable(false);
                    GridPane.setColumnSpan(btnGrid, 5);
                }
            } else if(method.equals("Location")) {
                if(!list.contains(locField)) {
                    list.removeAll(locField, locDistance, search, searchOrder, searchType);
                    add(locField, 2, 0);
                    add(locDistance, 3, 0);
                    add(search, 4, 0);
                    add(searchOrder, 5, 0);
                    add(searchType, 6, 0);
                    searchType.getSelectionModel().select(1);
                    searchType.setDisable(true);
                    GridPane.setColumnSpan(btnGrid, 7);
                }
            }
        });
        searchMethod.getSelectionModel().select(0);
        add(searchMethod, 0, 0);

        searchField = new TextField();
        searchField.setPromptText("Search");
        add(searchField, 1, 0);
        GridPane.setHgrow(searchField, Priority.ALWAYS);

        search = new Button("Search");
        search.setOnAction(this);
        add(search, 2, 0);

        searchOrder = new ComboBox<>();
        searchOrder.getItems().addAll("Relevance", "Date", "Title", "Rating", "Views");
        searchOrder.getSelectionModel().select(0);
        add(searchOrder, 3, 0);

        searchType = new ComboBox<>();
        searchType.getItems().addAll("All Types", "Video", "Channel", "Playlist");
        searchType.getSelectionModel().select(0);
        add(searchType, 4, 0);

        add(btnGrid, 0, 1, 5, 1);

        searchResults = new VBox();
        searchResults.setPadding(new Insets(10,10,10,10));
        searchResults.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(searchResults);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        btnGrid.add(scroll, 0, 0);
        GridPane.setHgrow(scroll, Priority.ALWAYS);

        selectAll = new Button("Select All");
        selectAll.setTooltip(new Tooltip("Select or deselect all visible items."));
        selectAll.setDisable(true);
        selectAll.setOnAction(this);

        clearResults = new Button("Clear Results");
        clearResults.setTooltip(new Tooltip("Clear all visible items."));
        clearResults.setDisable(true);
        clearResults.setOnAction(this);

        addToGroup = new Button("Add To Group");
        addToGroup.setTooltip(new Tooltip("Add selected results to a group."));
        addToGroup.setId("completeForm");
        addToGroup.setDisable(true);
        addToGroup.setOnAction(this);

        nextPage = new Button("Next Page");
        nextPage.setTooltip(new Tooltip("Get the next page of results."));
        nextPage.setDisable(true);
        nextPage.setOnAction(this);

        resultStatus = new Label();

        HBox hbox = new HBox(5);
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().addAll(selectAll, clearResults, addToGroup, nextPage, resultStatus);
        hbox.setPadding(new Insets(0,0,5,0));

        setOnKeyPressed(ke -> {
            if(ke.getCode().equals(KeyCode.ENTER)) {
                search.fire();
            }
        });

        btnGrid.add(hbox, 0, 1);
    }

    private StackPane createAddToGroupPane() {
        ToggleGroup toggle = new ToggleGroup();
        RadioButton existing = new RadioButton("Existing group.");
        existing.setToggleGroup(toggle);
        RadioButton newGroup = new RadioButton("Make new group.");
        newGroup.setToggleGroup(toggle);

        ChoiceBox<Group> groupList = new ChoiceBox<>(CommentSuiteFX.getApp().groupsList);

        TextField field = new TextField();
        field.setMinWidth(200);
        field.setPromptText("Choose a unique nameProperty.");


        existing.setOnAction(e -> {
            if(existing.isSelected()) {
                groupList.setDisable(false);
                field.setDisable(true);
            }
        });
        newGroup.setOnAction(e -> {
            if(newGroup.isSelected()) {
                groupList.setDisable(true);
                field.setDisable(false);
            }
        });

        if(groupList.getItems().isEmpty()) {
            existing.setDisable(true);
            newGroup.fire();
        } else {
            existing.fire();
            groupList.getSelectionModel().select(0);
        }

        List<GitemType> items = new ArrayList<>();
        searchResults.getChildren().stream()
                .filter(object -> object instanceof SearchResult && ((SearchResult) object).isSelected())
                .forEach(result -> items.add(((SearchResult) result).gitem));

        Button ok = new Button("Finish");
        Button cancel = new Button("Cancel");
        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(cancel, ok);
        hbox.setAlignment(Pos.CENTER_RIGHT);

        Label title = new Label("Add "+items.size()+" items to group: ");
        title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 16));

        VBox vbox = new VBox(10);
        vbox.setId("stackMenu");
        vbox.setMaxHeight(0);
        vbox.setMaxWidth(0);
        vbox.setFillWidth(true);
        vbox.setPadding(new Insets(25,25,25,25));
        vbox.setAlignment(Pos.CENTER_LEFT);
        vbox.getChildren().addAll(title, existing, groupList, newGroup, field, hbox);

        StackPane glass = new StackPane();
        glass.setStyle("-fx-background-color: rgba(127,127,127,0.5);");
        glass.setMaxHeight(Double.MAX_VALUE);
        glass.setMaxWidth(Double.MAX_VALUE);
        glass.setAlignment(Pos.CENTER);
        glass.getChildren().add(vbox);
        cancel.setOnAction(ae -> CommentSuiteFX.getMainPane().getChildren().remove(glass));
        ok.setOnAction(ae -> {
            String group_name;
            try {
                Group group;
                if(newGroup.isSelected()) {
                    boolean unique = true;
                    group_name = field.getText();
                    for(Group g : groupList.getItems()) if(g.group_name.equals(group_name)) {
                        unique = false;
                        break;
                    }
                    if(unique) {
                        CommentSuiteFX.getDatabase().insertGroup(group_name);
                        CommentSuiteFX.reloadGroups();
                    }
                    group = CommentSuiteFX.getDatabase().getGroup(group_name);
                } else {
                    group = groupList.getSelectionModel().getSelectedItem();
                }
                List<String> currentItems = CommentSuiteFX.getDatabase().getGitems(group.group_id, false).stream().map(YoutubeObject::getId).collect(Collectors.toList());
                CommentSuiteFX.getDatabase().insertGitems(group.group_id, items.stream().filter(gitem -> !currentItems.contains(gitem.getId())).collect(Collectors.toList()));
                CommentSuiteFX.getMainPane().getChildren().remove(glass);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return glass;
    }
}