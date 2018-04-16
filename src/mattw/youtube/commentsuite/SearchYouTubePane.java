package mattw.youtube.commentsuite;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.commentsuite.db.GroupItem;
import mattw.youtube.commentsuite.io.Clipboards;
import mattw.youtube.commentsuite.io.Geolocation;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.SearchList;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchYouTubePane extends StackPane {

    private boolean doNewSearch = true;

    public SearchYouTubePane(YouTubeData3 data) {
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

        ImageView imgSearch = new ImageView(CommentSuite.IMG_SEARCH);
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
        youtubeList.setCellFactory(cf -> new ListViewEmptyCellFactory<>(143));
        youtubeList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox.setVgrow(youtubeList, Priority.ALWAYS);

        SimpleStringProperty pageTokenProperty = new SimpleStringProperty("12345");

        openBrowser.setOnAction(ae -> {
            List<SearchListView> items = youtubeList.getSelectionModel().getSelectedItems();
            for(SearchListView item : items) {
                CommentSuite.openInBrowser(item.getYouTubeLink());
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

        getChildren().add(vbox);
        setPadding(new Insets(10));

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
            groups.setItems(CommentSuite.db().globalGroupList);
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
            getChildren().add(overlay);

            cancel.setOnAction(ae0 -> getChildren().remove(overlay));
            finish.setOnAction(ae0 -> {
                try {
                    Group group = null;
                    if(existing.isSelected()) {
                        group = groups.getValue();
                    } else if(newgroup.isSelected()) {
                        group = CommentSuite.db().createGroup(groupName.getText());
                    }
                    if(group != null) {
                        List<GroupItem> insertItems = items.stream().map(GroupItem::new).collect(Collectors.toList());
                        CommentSuite.db().insertGroupItems(group, insertItems);
                        CommentSuite.db().commit();
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
    }
}
