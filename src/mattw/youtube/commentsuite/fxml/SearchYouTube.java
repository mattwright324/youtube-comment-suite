package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.io.BrowserUtil;
import mattw.youtube.commentsuite.io.ClipboardUtil;
import mattw.youtube.commentsuite.io.EurekaProvider;
import mattw.youtube.commentsuite.io.Location;
import mattw.youtube.datav3.Parts;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.entrypoints.SearchList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class SearchYouTube implements Initializable {

    private static Logger logger = LogManager.getLogger(SearchYouTube.class.getSimpleName());

    private Location<EurekaProvider, EurekaProvider.Location> location;
    private YouTubeData3 youtubeApi;
    private ClipboardUtil clipboardUtil = new ClipboardUtil();
    private BrowserUtil browserUtil = new BrowserUtil();

    private @FXML Pane form;
    private @FXML ImageView searchIcon;
    private @FXML ImageView geoIcon;
    private @FXML ComboBox<String> searchType;
    private @FXML TextField searchText;
    private @FXML Button submit;
    private @FXML HBox locationBox;
    private @FXML TextField searchLocation;
    private @FXML Button geolocate;
    private @FXML ComboBox<String> searchRadius;
    private @FXML ComboBox<String> searchOrder;
    private @FXML ComboBox<String> resultType;
    private @FXML ListView<SearchYouTubeListItem> resultsList;
    private @FXML HBox bottom;
    private @FXML Button btnAddToGroup;
    private @FXML Button btnClear;
    private @FXML Button btnNextPage;
    private @FXML Label searchInfo;

    private @FXML MenuItem menuCopyId;
    private @FXML MenuItem menuOpenBrowser;
    private @FXML MenuItem menuAddToGroup;
    private @FXML MenuItem menuDeselectAll;

    private @FXML OverlayModal<SYAddToGroupModal> addToGroupModal;

    private int total = 0;
    private int number = 0;
    private String emptyToken = "emptyToken";
    private String pageToken = emptyToken;

    private SearchList searchList;
    private SimpleBooleanProperty searching = new SimpleBooleanProperty(false);
    private String[] types = {SearchList.TYPE_ALL, SearchList.TYPE_VIDEO, SearchList.TYPE_PLAYLIST, SearchList.TYPE_CHANNEL};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialize SearchYouTube");

        youtubeApi = FXMLSuite.getYoutubeApi();
        this.location = FXMLSuite.getLocation();

        MultipleSelectionModel selectionModel = resultsList.getSelectionModel();

        searchIcon.setImage(ImageLoader.SEARCH.getImage());
        geoIcon.setImage(ImageLoader.LOCATION.getImage());

        BooleanBinding isLocation = searchType.valueProperty().isEqualTo("Location");
        locationBox.managedProperty().bind(isLocation);
        locationBox.visibleProperty().bind(isLocation);
        searchRadius.managedProperty().bind(isLocation);
        searchRadius.visibleProperty().bind(isLocation);

        resultsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        menuCopyId.setOnAction(ae -> {
            List<SearchYouTubeListItem> list = selectionModel.getSelectedItems();
            List<String> ids = list.stream().map(SearchYouTubeListItem::getObjectId).collect(Collectors.toList());
            clipboardUtil.setClipboard(ids);
        });
        menuOpenBrowser.setOnAction(ae -> {
            List<SearchYouTubeListItem> list = selectionModel.getSelectedItems();
            for(SearchYouTubeListItem view : list) {
                browserUtil.open(view.getYoutubeURL());
            }
        });
        menuAddToGroup.setOnAction(ae -> btnAddToGroup.fire());
        menuDeselectAll.setOnAction(ae -> selectionModel.clearSelection());

        btnAddToGroup.disableProperty().bind(selectionModel.selectedIndexProperty().isEqualTo(-1));
        selectionModel.getSelectedItems().addListener((ListChangeListener)(c -> {
            int items = selectionModel.getSelectedItems().size();
            runLater(() -> btnAddToGroup.setText(String.format("Add to Group (%s)", items)));
        }));
        resultsList.itemsProperty().addListener((o, ov, nv) -> runLater(() -> {
            int selectedCount = selectionModel.getSelectedItems().size();
            btnAddToGroup.setText(String.format("Add to Group (%s)", selectedCount));
        }));

        btnClear.setOnAction(ae -> runLater(() -> resultsList.getItems().clear()));

        geolocate.setOnAction(ae -> new Thread(() -> {
            geolocate.setDisable(true);
            try {
                EurekaProvider.Location myLocation = this.location.getMyLocation();

                String coordinates = myLocation.geolocation_data.latitude+","+myLocation.geolocation_data.longitude;

                runLater(() -> searchLocation.setText(coordinates));
            } catch (IOException e) {
                logger.error(e);
            }
            geolocate.setDisable(false);
        }).start());

        bottom.disableProperty().bind(searching);
        form.disableProperty().bind(searching);

        form.setOnKeyPressed(ke -> {
            if(ke.getCode() == KeyCode.ENTER) {
                runLater(() ->
                    submit.fire()
                );
            }
        });
        submit.setOnAction(ae -> {
            total = 0;
            number = 0;
            pageToken = emptyToken;
            resultsList.getItems().clear();
            runLater(() -> searchInfo.setText(String.format("Showing %s out of %s", resultsList.getItems().size(), total)));
            logger.debug(String.format("Submit New Search [pageToken=%s,type=%s,text=%s,locText=%s,locRadius=%s,order=%s,result=%s]",
                    pageToken, searchType.getValue(), searchText.getText(), searchLocation.getText(),
                    searchRadius.getValue(), searchOrder.getValue(), resultType.getValue()));
            new Thread(() ->
                search(pageToken, searchType.getValue(), searchText.getText(), searchLocation.getText(),
                        searchRadius.getValue(), searchOrder.getValue(),
                        resultType.getSelectionModel().getSelectedIndex())
            ).start();
        });
        btnNextPage.setOnAction(ae ->
            new Thread(() ->
                search(pageToken, searchType.getValue(), searchText.getText(), searchLocation.getText(),
                        searchRadius.getValue(), searchOrder.getValue(),
                        resultType.getSelectionModel().getSelectedIndex())
            ).start()
        );

        SYAddToGroupModal syAddToGroupModal = new SYAddToGroupModal(resultsList);
        addToGroupModal.setContent(syAddToGroupModal);
        syAddToGroupModal.getBtnClose().setOnAction(ae -> {
            addToGroupModal.setVisible(false);
            addToGroupModal.setManaged(false);
        });
        btnAddToGroup.setOnAction(ae -> {
            syAddToGroupModal.cleanUp();
            addToGroupModal.setVisible(true);
            addToGroupModal.setManaged(true);
        });
    }

    public void search(String pageToken, String type, String text, String locText, String locRadius, String order, int resultType) {
        runLater(() -> searching.setValue(true));
        try {
            searchList = ((SearchList) youtubeApi.searchList().part(Parts.SNIPPET)).order(order.toLowerCase());

            if(pageToken.equals(emptyToken)) {
                pageToken = "";
            }

            String encodedText = URLEncoder.encode(text, "UTF-8");
            String searchType = types[resultType];

            if(type.equals("Normal")) {
                logger.debug(String.format("Normal Search [key=%s,part=snippet,text=%s,type=%s,order=%s,token=%s]",
                        youtubeApi.getDataApiKey(),encodedText,searchType,order.toLowerCase(),pageToken));
                searchList = searchList.get(encodedText, searchType, pageToken);
            } else {
                logger.debug(String.format("Location Search [key=%s,part=snippet,text=%s,loc=%s,radius=%s,type=%s,order=%s,token=%s]",
                        youtubeApi.getDataApiKey(),encodedText,locText,locRadius,searchType,order.toLowerCase(),pageToken));
                searchList = searchList.getByLocation(encodedText, pageToken, locText, locRadius);
            }

            this.pageToken = searchList.getNextPageToken() == null ? emptyToken : searchList.getNextPageToken();
            this.total = searchList.getPageInfo().getTotalResults();

            logger.debug(String.format("Search [videos=%s]", searchList.getItems().length));
            for(SearchList.Item item : searchList.getItems()) {
                logger.debug(String.format("Video [id=%s,author=%s,title=%s]",
                        item.getId().getId(),
                        item.getSnippet().getChannelTitle(),
                        item.getSnippet().getTitle()));
                SearchYouTubeListItem view = new SearchYouTubeListItem(item, number++);
                runLater(() -> {
                    resultsList.getItems().add(view);
                    searchInfo.setText(String.format("Showing %s out of %s", resultsList.getItems().size(), total));
                });
            }
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
        runLater(() -> {
            if(this.pageToken != null && !this.pageToken.equals(emptyToken)) {
                btnNextPage.setDisable(false);
            }
            searching.setValue(false);
        });
    }
}
