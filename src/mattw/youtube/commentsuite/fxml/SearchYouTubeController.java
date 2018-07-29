package mattw.youtube.commentsuite.fxml;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.io.BrowserUtil;
import mattw.youtube.commentsuite.io.ClipboardUtil;
import mattw.youtube.commentsuite.io.Geolocation;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.SearchList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SearchYouTubeController implements Initializable {

    private static Logger logger = LogManager.getLogger(SearchYouTubeController.class.getSimpleName());

    private final Image IMG_SEARCH = new Image("/mattw/youtube/commentsuite/img/search.png");
    private final Image IMG_LOCATION = new Image("/mattw/youtube/commentsuite/img/location.png");

    private Geolocation geolocation;
    private YouTubeData3 youtubeApi;
    private ClipboardUtil clipboardUtil = new ClipboardUtil();
    private BrowserUtil browserUtil = new BrowserUtil();

    @FXML Pane form;
    @FXML ImageView searchIcon;
    @FXML ImageView geoIcon;
    @FXML ComboBox<String> searchType;
    @FXML TextField searchText;
    @FXML Button submit;
    @FXML HBox locationBox;
    @FXML TextField searchLocation;
    @FXML Button geolocate;
    @FXML ComboBox<String> searchRadius;
    @FXML ComboBox<String> searchOrder;
    @FXML ComboBox<String> resultType;
    @FXML ListView<SearchYouTubeListItemView> resultsList;
    @FXML HBox bottom;
    @FXML Button btnAddToGroup;
    @FXML Button btnClear;
    @FXML Button btnNextPage;
    @FXML Label searchInfo;

    @FXML MenuItem menuCopyId;
    @FXML MenuItem menuOpenBrowser;
    @FXML MenuItem menuAddToGroup;
    @FXML MenuItem menuDeselectAll;

    private int total = 0;
    private int number = 0;
    private String emptyToken = "emptyToken";
    private String pageToken = emptyToken;

    private SearchList searchList;
    private SimpleBooleanProperty searching = new SimpleBooleanProperty(false);
    private String[] types = {SearchList.TYPE_ALL, SearchList.TYPE_VIDEO, SearchList.TYPE_PLAYLIST, SearchList.TYPE_CHANNEL};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialize SearchYouTubeController");

        youtubeApi = FXMLSuite.getYoutubeApi();
        geolocation = FXMLSuite.getGeolocation();

        SelectionModel selectionModel = resultsList.getSelectionModel();

        searchIcon.setImage(IMG_SEARCH);
        geoIcon.setImage(IMG_LOCATION);

        BooleanBinding isLocation = searchType.valueProperty().isEqualTo("Location");
        locationBox.managedProperty().bind(isLocation);
        locationBox.visibleProperty().bind(isLocation);
        searchRadius.managedProperty().bind(isLocation);
        searchRadius.visibleProperty().bind(isLocation);

        resultsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        menuCopyId.setOnAction(ae -> {
            List<SearchYouTubeListItemView> list =  ((MultipleSelectionModel) selectionModel).getSelectedItems();
            List<String> ids = list.stream().map(view -> view.getObjectId()).collect(Collectors.toList());
            clipboardUtil.setClipboard(ids);
        });
        menuOpenBrowser.setOnAction(ae -> {
            List<SearchYouTubeListItemView> list =  ((MultipleSelectionModel) selectionModel).getSelectedItems();
            for(SearchYouTubeListItemView view : list) {
                browserUtil.open(view.getYoutubeURL());
            }
        });
        menuAddToGroup.setOnAction(ae -> btnAddToGroup.fire());
        menuDeselectAll.setOnAction(ae -> selectionModel.clearSelection());

        btnAddToGroup.disableProperty().bind(selectionModel.selectedIndexProperty().isEqualTo(-1));
        ((MultipleSelectionModel) selectionModel).getSelectedItems().addListener((ListChangeListener)(c -> {
            int items = ((MultipleSelectionModel) selectionModel).getSelectedItems().size();
            Platform.runLater(() -> btnAddToGroup.setText(String.format("Add to Group (%s)", items)));
        }));
        resultsList.itemsProperty().addListener((o, ov, nv) -> Platform.runLater(() -> {
            int selectedCount = ((MultipleSelectionModel) selectionModel).getSelectedItems().size();
            btnAddToGroup.setText(String.format("Add to Group (%s)", selectedCount));
        }));

        btnClear.setOnAction(ae -> Platform.runLater(() -> resultsList.getItems().clear()));

        geolocate.setOnAction(ae -> new Thread(() -> {
            geolocate.setDisable(true);
            try {
                Geolocation.Location myLocation = geolocation.getMyLocation();

                String coordinates = myLocation.geolocation_data.latitude+","+myLocation.geolocation_data.longitude;

                Platform.runLater(() -> searchLocation.setText(coordinates));
            } catch (IOException e) {
                logger.error(e);
            }
            geolocate.setDisable(false);
        }).start());

        bottom.disableProperty().bind(searching);
        form.disableProperty().bind(searching);

        form.setOnKeyPressed(ke -> {
            if(ke.getCode() == KeyCode.ENTER) {
                Platform.runLater(() -> {
                    submit.fire();
                });
            }
        });
        submit.setOnAction(ae -> {
            total = 0;
            number = 0;
            pageToken = emptyToken;
            resultsList.getItems().clear();
            Platform.runLater(() -> searchInfo.setText(String.format("Showing %s out of %s", resultsList.getItems().size(), total)));
            logger.debug(String.format("Submit New Search [pageToken=%s,type=%s,text=%s,locText=%s,locRadius=%s,order=%s,result=%s]",
                    pageToken, searchType.getValue(), searchText.getText(), searchLocation.getText(),
                    searchRadius.getValue(), searchOrder.getValue(), resultType.getValue()));
            new Thread(() -> {
                search(pageToken, searchType.getValue(), searchText.getText(), searchLocation.getText(),
                        searchRadius.getValue(), searchOrder.getValue(),
                        resultType.getSelectionModel().getSelectedIndex());
            }).start();
        });
        btnNextPage.setOnAction(ae -> {
            new Thread(() -> {
                search(pageToken, searchType.getValue(), searchText.getText(), searchLocation.getText(),
                        searchRadius.getValue(), searchOrder.getValue(),
                        resultType.getSelectionModel().getSelectedIndex());
            }).start();
        });
    }

    public void search(String pageToken, String type, String text, String locText, String locRadius, String order, int resultType) {
        Platform.runLater(() -> searching.setValue(true));
        try {
            searchList = youtubeApi.searchList().order(order.toLowerCase());

            if(pageToken.equals(emptyToken)) {
                pageToken = "";
            }

            String encodedText = URLEncoder.encode(text, "UTF-8");
            String searchType = types[resultType];

            if(type.equals("Normal")) {
                logger.debug(String.format("Normal Search [key=%s,part=snippet,text=%s,type=%s,order=%s,token=%s]",
                        youtubeApi.getDataApiKey(),encodedText,searchType,order.toLowerCase(),pageToken));
                searchList = searchList.get(SearchList.PART_SNIPPET, encodedText, searchType, pageToken);
            } else {
                logger.debug(String.format("Location Search [key=%s,part=snippet,text=%s,loc=%s,radius=%s,type=%s,order=%s,token=%s]",
                        youtubeApi.getDataApiKey(),encodedText,locText,locRadius,searchType,order.toLowerCase(),pageToken));
                searchList = searchList.getByLocation(SearchList.PART_SNIPPET, encodedText, pageToken, locText, locRadius);
            }

            this.pageToken = searchList.nextPageToken.equals(null) ? emptyToken : searchList.nextPageToken;
            this.total = searchList.pageInfo.totalResults;

            logger.debug(String.format("Search [videos=%s]", searchList.items.length));
            for(SearchList.Item item : searchList.items) {
                logger.debug(String.format("Video [id=%s,author=%s,title=%s]", item.id.getId(), item.snippet.channelTitle, item.snippet.title));
                SearchYouTubeListItemView view = new SearchYouTubeListItemView(item, number++);
                Platform.runLater(() -> {
                    resultsList.getItems().add(view);
                    searchInfo.setText(String.format("Showing %s out of %s", resultsList.getItems().size(), total));
                });
            }
        } catch (IOException | YouTubeErrorException e) {
            logger.error(e);
            e.printStackTrace();
        }
        Platform.runLater(() -> {
            if(this.pageToken != null && !this.pageToken.equals(emptyToken)) {
                btnNextPage.setDisable(false);
            }
            searching.setValue(false);
        });
    }
}
