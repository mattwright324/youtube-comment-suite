package io.mattw.youtube.commentsuite.fxml;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.util.BrowserUtil;
import io.mattw.youtube.commentsuite.util.ClipboardUtil;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

public class SearchYouTube implements Initializable {

    private static final Logger logger = LogManager.getLogger();
    private static final String TOKEN_FOO = "TOKEN_FOO";
    private final ClipboardUtil clipboardUtil = new ClipboardUtil();
    private final BrowserUtil browserUtil = new BrowserUtil();

    @FXML
    private Pane form;
    @FXML
    private ImageView searchIcon;
    @FXML
    private TextField searchText;
    @FXML
    private Button submit;
    @FXML
    private ComboBox<String> searchOrder;
    @FXML
    private ComboBox<String> resultType;
    @FXML
    private ListView<SearchYouTubeListItem> resultsList;
    @FXML
    private HBox bottom;
    @FXML
    private Button btnAddToGroup;
    @FXML
    private Button btnClear;
    @FXML
    private Button btnNextPage;
    @FXML
    private Label searchInfo;

    @FXML
    private MenuItem menuCopyId;
    @FXML
    private MenuItem menuOpenBrowser;
    @FXML
    private MenuItem menuAddToGroup;
    @FXML
    private MenuItem menuDeselectAll;

    @FXML
    private OverlayModal<SYAddToGroupModal> addToGroupModal;

    private int total = 0;
    private int number = 0;
    private String pageToken = TOKEN_FOO;

    private YouTube.Search.List searchList;
    private String[] types = {"all", "video", "playlist", "channel"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialize SearchYouTube");

        final MultipleSelectionModel<SearchYouTubeListItem> selectionModel = resultsList.getSelectionModel();

        searchIcon.setImage(ImageLoader.SEARCH.getImage());

        resultsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        menuCopyId.setOnAction(ae -> {
            final List<String> ids = selectionModel.getSelectedItems().stream()
                    .map(SearchYouTubeListItem::getObjectId)
                    .collect(Collectors.toList());

            clipboardUtil.setClipboard(ids);
        });
        menuOpenBrowser.setOnAction(ae -> {
            for (final SearchYouTubeListItem view : selectionModel.getSelectedItems()) {
                browserUtil.open(view.getYoutubeURL());
            }
        });
        menuAddToGroup.setOnAction(ae -> btnAddToGroup.fire());
        menuDeselectAll.setOnAction(ae -> runLater(selectionModel::clearSelection));

        btnAddToGroup.disableProperty().bind(selectionModel.selectedIndexProperty().isEqualTo(-1));
        selectionModel.getSelectedItems().addListener((ListChangeListener<SearchYouTubeListItem>) (c ->
                runLater(() -> btnAddToGroup.setText(String.format("Add to Group (%s)", selectionModel.getSelectedItems().size())))
        ));
        resultsList.itemsProperty().addListener((o, ov, nv) ->
                runLater(() -> btnAddToGroup.setText(String.format("Add to Group (%s)", selectionModel.getSelectedItems().size()))));

        btnClear.setOnAction(ae -> runLater(() -> resultsList.getItems().clear()));

        form.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ENTER) {
                runLater(submit::fire);
            }
        });
        submit.setOnAction(ae -> {
            total = 0;
            number = 0;
            pageToken = TOKEN_FOO;
            runLater(() -> {
                resultsList.getItems().clear();
                searchInfo.setText(String.format("Showing %s out of %s", resultsList.getItems().size(), total));
            });
            logger.debug("Submit New Search [pageToken={},text={},order={},result={}]",
                    pageToken, searchText.getText(), searchOrder.getValue(), resultType.getValue());
            new Thread(() ->
                    search(pageToken, searchText.getText(), searchOrder.getValue(),
                            resultType.getSelectionModel().getSelectedIndex())
            ).start();
        });
        btnNextPage.setOnAction(ae -> new Thread(() ->
                search(pageToken, searchText.getText(), searchOrder.getValue(),
                        resultType.getSelectionModel().getSelectedIndex())
        ).start());

        final SYAddToGroupModal syAddToGroupModal = new SYAddToGroupModal(resultsList);
        addToGroupModal.setContent(syAddToGroupModal);
        syAddToGroupModal.getBtnClose().setOnAction(ae -> runLater(() -> {
            addToGroupModal.setVisible(false);
            addToGroupModal.setManaged(false);
        }));
        addToGroupModal.visibleProperty().addListener((cl) -> runLater(() -> {
            syAddToGroupModal.getBtnClose().setCancelButton(addToGroupModal.isVisible());
            syAddToGroupModal.getBtnSubmit().setDefaultButton(addToGroupModal.isVisible());
        }));
        btnAddToGroup.setOnAction(ae -> runLater(() -> {
            syAddToGroupModal.cleanUp();
            addToGroupModal.setVisible(true);
            addToGroupModal.setManaged(true);
        }));
    }

    public void search(
            String pageToken,
            final String text,
            String order,
            final int resultType
    ) {
        runLater(() -> {
            bottom.setDisable(true);
            form.setDisable(true);
        });
        try {
            final String encodedText = text.replaceAll("\\|", "%7C");
            final String searchType = types[resultType];

            if (pageToken.equals(TOKEN_FOO)) {
                pageToken = "";
            }

            if (order.equals("Video Count")) {
                order = "videoCount";
            } else if (order.equals("View Count")) {
                order = "viewCount";
            }

            searchList = CommentSuite.getYouTube().search().list("snippet")
                    .setKey(CommentSuite.getYouTubeApiKey())
                    .setMaxResults(50L)
                    .setPageToken(pageToken)
                    .setQ(encodedText)
                    .setType(searchType)
                    .setOrder(order.toLowerCase());

            logger.debug("Normal Search [key={},part=snippet,text={},type={},order={},token={}]",
                    CommentSuite.getYouTubeApiKey(), encodedText, searchType, order.toLowerCase(), pageToken);

            final SearchListResponse sl = searchList.execute();;

            this.pageToken = sl.getNextPageToken() == null ? TOKEN_FOO : sl.getNextPageToken();
            this.total = sl.getPageInfo().getTotalResults();

            logger.debug("Search [videos={}]", sl.getItems().size());
            for (final SearchResult item : sl.getItems()) {
                logger.debug("Video [id={},author={},title={}]",
                        item.getId(),
                        item.getSnippet().getChannelTitle(),
                        item.getSnippet().getTitle());
                final SearchYouTubeListItem view = new SearchYouTubeListItem(item, number++);
                runLater(() -> {
                    resultsList.getItems().add(view);

                    if (TOKEN_FOO.equals(this.pageToken)) {
                        this.total = resultsList.getItems().size();
                    }

                    searchInfo.setText(String.format("Showing %s out of %s", resultsList.getItems().size(), total));
                });
            }
        } catch (final IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
        runLater(() -> {
            if (this.pageToken != null && !TOKEN_FOO.equals(this.pageToken)) {
                btnNextPage.setDisable(false);
            }
            if (TOKEN_FOO.equals(this.pageToken)) {
                btnNextPage.setDisable(true);
                btnNextPage.setText("Out of pages");
            } else {
                btnNextPage.setText("Next Page >");
            }
            bottom.setDisable(false);
            form.setDisable(false);
        });
    }
}
