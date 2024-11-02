package io.mattw.youtube.commentsuite.fxml;

import com.google.common.eventbus.Subscribe;
import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ImageCache;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.db.*;
import io.mattw.youtube.commentsuite.events.*;
import io.mattw.youtube.commentsuite.util.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;

public class SearchComments implements Initializable, ImageCache {

    private static final Logger logger = LogManager.getLogger();

    @FXML private VBox contextPane, resultsPane, queryPane;
    @FXML private ImageView videoThumb, authorThumb, toggleContextIcon, toggleQueryIcon;
    @FXML private ImageView firstPageIcon, prevPageIcon, nextPageIcon, lastPageIcon;
    @FXML private TextField videoTitle, author;
    @FXML private Label toggleContext, toggleQuery;
    @FXML private Label videoViews, videoLikes;
    @FXML private TextArea videoDescription;

    @FXML private ImageView browserIcon;
    @FXML private ImageView tagsIcon;
    @FXML private MenuItem openInBrowser, manageTags, copyNames, copyComments, copyChannelLinks, copyVideoLinks, copyCommentLinks, copyCommentIds, copyChannelIds;
    @FXML private ListView<SearchCommentsListItem> resultsList;
    @FXML private TextField pageValue;
    @FXML private Label displayCount, lblMaxPage;
    @FXML private Button btnFirst, btnPrev, btnNext, btnLast;
    @FXML private Button btnBackToResults;
    @FXML private HBox paginationPane, backToResultsPane;

    @FXML private ComboBox<Group> comboGroupSelect;
    @FXML private ComboBox<GroupItem> comboGroupItemSelect;
    @FXML private Hyperlink videoSelect;
    @FXML private ComboBox<CommentQuery.CommentsType> comboCommentType;
    @FXML private ComboBox<CommentQuery.Order> comboOrderBy;
    @FXML private TextField nameLike, commentLike, hasTags;
    @FXML private DatePicker dateFrom, dateTo;
    @FXML private Button btnSearch, btnClear, btnSelectTags;

    @FXML private OverlayModal<SCVideoSelectModal> videoSelectModal;
    @FXML private OverlayModal<SCShowMoreModal> showMoreModal;
    @FXML private OverlayModal<SCManageTagsModal> tagsModal;
    @FXML private OverlayModal<SCSelectTagsModal> selectTagsModal;

    private ChangeListener<Font> fontListener;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private SimpleBooleanProperty searchingProperty = new SimpleBooleanProperty(false);
    private SimpleIntegerProperty pageProperty = new SimpleIntegerProperty();
    private SimpleIntegerProperty maxPageProperty = new SimpleIntegerProperty();
    private ElapsedTime elapsedTime = new ElapsedTime();

    private CommentDatabase database;
    private CommentQuery commentQuery;
    private List<YouTubeComment> lastResultsList;
    private SearchCommentsListItem originalTreeComment;
    private ClipboardUtil clipboardUtil = new ClipboardUtil();
    private BrowserUtil browserUtil = new BrowserUtil();
    private ConfigData configData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        database = CommentSuite.getDatabase();
        commentQuery = database.commentQuery();
        configData = CommentSuite.getConfig().getDataObject();

        CommentSuite.getEventBus().register(this);

        SelectionModel<Group> selectionModel = comboGroupSelect.getSelectionModel();
        selectionModel.selectedItemProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                reloadGroupItems();
            } else {
                runLater(() -> {
                    comboGroupItemSelect.setDisable(true);
                    videoSelect.setDisable(true);
                });
            }
        });
        runLater(this::rebuildGroupSelect);
        runLater(this::reloadGroupItems);

        resultsPane.disableProperty().bind(searchingProperty);
        queryPane.disableProperty().bind(searchingProperty);

        comboCommentType.getItems().addAll(CommentQuery.CommentsType.values());
        comboCommentType.getSelectionModel().select(0);
        comboOrderBy.getItems().addAll(CommentQuery.Order.values());
        comboOrderBy.getSelectionModel().select(0);

        dateFrom.setValue(LocalDate.of(1970, 1, 1));
        dateTo.setValue(LocalDate.now()); // midnight = .atTime(LocalDate.MAX);

        firstPageIcon.setImage(ImageLoader.ANGLE_DOUBLE_LEFT.getImage());
        prevPageIcon.setImage(ImageLoader.ANGLE_LEFT.getImage());
        nextPageIcon.setImage(ImageLoader.ANGLE_RIGHT.getImage());
        lastPageIcon.setImage(ImageLoader.ANGLE_DOUBLE_RIGHT.getImage());

        videoThumb.setImage(ImageLoader.VIDEO_PLACEHOLDER.getImage());
        authorThumb.setImage(ImageCache.toLetterAvatar('m'));

        toggleContextIcon.setImage(ImageLoader.TOGGLE_CONTEXT.getImage());
        contextPane.visibleProperty().bind(contextPane.managedProperty());
        toggleContext.setOnMouseClicked(me -> runLater(() -> contextPane.setManaged(!contextPane.isManaged())));

        toggleQueryIcon.setImage(ImageLoader.TOGGLE_QUERY.getImage());
        queryPane.visibleProperty().bind(queryPane.managedProperty());
        toggleQuery.setOnMouseClicked(me -> runLater(() -> queryPane.setManaged(!queryPane.isManaged())));

        pageValue.setOnMouseClicked(me -> runLater(() -> {
            pageValue.setEditable(true);
            pageValue.getStyleClass().remove("clearTextField");
        }));
        pageValue.focusedProperty().addListener((o, ov, nv) -> {
            if (!nv) {
                runLater(() -> pageValue.getStyleClass().remove("clearTextField"));
                int page = interpretPageValue(pageValue.getText());
                submitPageValue(page);
            }
        });
        pageValue.onMouseClickedProperty().addListener((cl) -> {
            runLater(() -> pageValue.getStyleClass().remove("clearTextField"));
            int page = interpretPageValue(pageValue.getText());
            submitPageValue(page);
        });
        pageValue.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ENTER || ke.getCode() == KeyCode.SPACE) {
                int page = interpretPageValue(pageValue.getText());
                submitPageValue(page);
            }
        });
        pageValue.setMinWidth(Region.USE_PREF_SIZE);
        pageValue.setMaxWidth(Region.USE_PREF_SIZE);
        pageValue.textProperty().addListener((ov, prevText, currText) -> FXUtils.adjustTextFieldWidthByContent(pageValue));
        pageValue.fontProperty().addListener(fontListener = (o, ov, nv) -> {
            FXUtils.adjustTextFieldWidthByContent(pageValue);

            // One-time font listener resize.
            // Will match content after font set on label from styleClass.
            // If not removed, when clicking the 'Rename' button, the label will
            // flicker once between Font size 15 (default) and back to the styleClass font size
            // every time the edit button is clicked.
            pageValue.fontProperty().removeListener(fontListener);
        });

        maxPageProperty.addListener((o, ov, nv) ->
                runLater(() -> lblMaxPage.setText(String.format(" of %s", nv)))
        );

        btnFirst.disableProperty().bind(pageProperty.lessThanOrEqualTo(1));
        btnFirst.setOnAction(ae -> submitPageValue(1));

        btnPrev.disableProperty().bind(pageProperty.lessThanOrEqualTo(1));
        btnPrev.setOnAction(ae -> submitPageValue(pageProperty.getValue() - 1));

        btnNext.disableProperty().bind(maxPageProperty.isEqualTo(pageProperty));
        btnNext.setOnAction(ae -> submitPageValue(pageProperty.getValue() + 1));

        btnLast.disableProperty().bind(maxPageProperty.isEqualTo(pageProperty));
        btnLast.setOnAction(ae -> submitPageValue(maxPageProperty.getValue()));

        pageProperty.setValue(1);
        maxPageProperty.setValue(1);

        btnSearch.setOnAction(ae -> submitPageValue(1, true));

        btnClear.setOnAction(ae -> runLater(() -> resultsList.getItems().clear()));

        resultsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        MultipleSelectionModel<SearchCommentsListItem> scSelection = resultsList.getSelectionModel();
        scSelection.selectedItemProperty().addListener((o, ov, nv) -> new Thread(() -> {
            if (nv != null) {
                showListItemContext(nv);
            }
        }).start());

        browserIcon.setImage(ImageLoader.BROWSER.getImage());
        openInBrowser.setOnAction(ae -> {
            String link = scSelection.getSelectedItem()
                    .getComment()
                    .toYouTubeLink();
            browserUtil.open(link);
        });
        copyNames.setOnAction(ae -> {
            Stream<String> toCopy = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getChannel)
                    .map(YouTubeChannel::getTitle);

            if (configData.isFilterDuplicatesOnCopy()) {
                toCopy = toCopy.distinct();
            }

            clipboardUtil.setClipboard(toCopy.collect(Collectors.toList()));
        });
        copyComments.setOnAction(ae -> {
            Stream<String> toCopy = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getCommentText);

            clipboardUtil.setClipboard(toCopy.collect(Collectors.toList()));
        });
        copyChannelLinks.setOnAction(ae -> {
            Stream<String> toCopy = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getChannel)
                    .map(YouTubeChannel::toYouTubeLink);

            if (configData.isFilterDuplicatesOnCopy()) {
                toCopy = toCopy.distinct();
            }

            clipboardUtil.setClipboard(toCopy.collect(Collectors.toList()));
        });
        copyCommentLinks.setOnAction(ae -> {
            Stream<String> toCopy = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::toYouTubeLink);

            if (configData.isFilterDuplicatesOnCopy()) {
                toCopy = toCopy.distinct();
            }

            clipboardUtil.setClipboard(toCopy.collect(Collectors.toList()));
        });
        copyVideoLinks.setOnAction(ae -> {
            Stream<String> toCopy = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(c -> String.format("https://youtu.be/%s", c.getVideoId()));

            if (configData.isFilterDuplicatesOnCopy()) {
                toCopy = toCopy.distinct();
            }

            clipboardUtil.setClipboard(toCopy.collect(Collectors.toList()));
        });
        copyCommentIds.setOnAction(ae -> {
            Stream<String> toCopy = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getId);

            if (configData.isFilterDuplicatesOnCopy()) {
                toCopy = toCopy.distinct();
            }

            clipboardUtil.setClipboard(toCopy.collect(Collectors.toList()));
        });
        copyChannelIds.setOnAction(ae -> {
            Stream<String> toCopy = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getChannelId);

            if (configData.isFilterDuplicatesOnCopy()) {
                toCopy = toCopy.distinct();
            }

            clipboardUtil.setClipboard(toCopy.collect(Collectors.toList()));
        });

        btnBackToResults.setOnAction(ae -> {
            setResultsList(lastResultsList, false);

            runLater(() -> {
                Optional<SearchCommentsListItem> toSelect = resultsList.getItems().stream()
                        .filter(scli -> scli.getComment().getId().equals(originalTreeComment.getComment().getId()))
                        .findFirst();

                SearchCommentsListItem scli = toSelect.orElse(null);

                resultsList.scrollTo(scli);
                resultsList.getSelectionModel().select(scli);
            });
        });

        SCVideoSelectModal scVideoSelectModal = new SCVideoSelectModal();
        videoSelectModal.setContent(scVideoSelectModal);
        videoSelectModal.getModalContainer().setMaxWidth(Double.MAX_VALUE);
        videoSelectModal.showSpacers(false);
        videoSelectModal.setPadding(new Insets(25));
        HBox.setHgrow(videoSelectModal.getModalContainer(), Priority.ALWAYS);
        videoSelect.textProperty().bind(scVideoSelectModal.valueProperty());
        videoSelect.setOnAction(ae -> runLater(() -> {
            Group group = comboGroupSelect.getValue();
            GroupItem groupItem = comboGroupItemSelect.getValue();

            scVideoSelectModal.loadWith(group, groupItem);
            scVideoSelectModal.cleanUp();
            videoSelectModal.setVisible(true);
        }));
        videoSelectModal.visibleProperty().addListener((cl) -> {
            scVideoSelectModal.getBtnSubmit().setCancelButton(videoSelectModal.isVisible());
            scVideoSelectModal.getBtnSubmit().setDefaultButton(videoSelectModal.isVisible());
        });
        scVideoSelectModal.getBtnClose().setOnAction(ae -> videoSelectModal.setVisible(false));
        scVideoSelectModal.getBtnSubmit().setOnAction(ae -> videoSelectModal.setVisible(false));
        comboGroupSelect.valueProperty().addListener((o, ov, nv) -> scVideoSelectModal.reset());
        comboGroupItemSelect.valueProperty().addListener((o, ov, nv) -> scVideoSelectModal.reset());

        SCShowMoreModal scShowMoreModal = new SCShowMoreModal();
        showMoreModal.setContent(scShowMoreModal);
        scShowMoreModal.getBtnClose().setOnAction(ae -> showMoreModal.setVisible(false));

        tagsIcon.setImage(ImageLoader.TAGS.getImage());
        ImageView tagsIcon2 = new ImageView(tagsIcon.getImage());
        tagsIcon2.setFitHeight(20);
        tagsIcon2.setFitWidth(20);
        btnSelectTags.setGraphic(tagsIcon2);

        SCManageTagsModal scManageTagsModal = new SCManageTagsModal();
        tagsModal.setContent(scManageTagsModal);
        scManageTagsModal.getBtnFinish().setOnAction(ae -> tagsModal.setVisible(false));
        manageTags.setOnAction(ae -> {
            scManageTagsModal.withComments(resultsList.getSelectionModel().getSelectedItems());
            tagsModal.setVisible(true);
        });

        SCSelectTagsModal scSelectTagsModal = new SCSelectTagsModal();
        selectTagsModal.setContent(scSelectTagsModal);
        scSelectTagsModal.getBtnClose().setOnAction(ae -> selectTagsModal.setVisible(false));
        scSelectTagsModal.getBtnSelect().setOnAction(ae -> runLater(() -> {
            hasTags.setText(scSelectTagsModal.getSelectedString());
            selectTagsModal.setVisible(false);
        }));
        btnSelectTags.setOnAction(ae -> selectTagsModal.setVisible(true));
    }

    /**
     * Load video context and comment author profiles on comment interaction: on selection, show more, reply, view thread
     */
    private void showListItemContext(final SearchCommentsListItem commentItem) {
        final YouTubeComment comment = commentItem.getComment();

        commentItem.loadProfileThumb();

        for (final SearchCommentsListItem scli : resultsList.getItems()) {
            scli.checkProfileThumb();
        }

        try {
            final String videoId = comment.getVideoId();
            final YouTubeVideo video = database.videos().get(videoId);

            final Image vThumb = video.findOrGetThumb();
            runLater(() -> {
                videoTitle.setText(video.getTitle());
                videoLikes.setText(readableNumber(video.getLikes()) + " likes");
                videoViews.setText(String.format("%s views", readableNumber(video.getViewCount())));
                videoDescription.setText(String.format("Published %s • %s",
                        formatter.format(DateUtils.epochMillisToDateTime(video.getPublished())),
                        StringEscapeUtils.unescapeHtml4(video.getDescription())));

                videoThumb.setCursor(Cursor.HAND);
                videoThumb.setOnMouseClicked(me -> browserUtil.open(video.toYouTubeLink()));
                videoThumb.setImage(vThumb);
            });

            final YouTubeChannel author = database.channels().getOrNull(video.getChannelId());
            final Image aThumb = Optional.ofNullable(author)
                    .map(HasImage::findOrGetThumb)
                    .orElse(ImageCache.toLetterAvatar(' '));
            runLater(() -> {
                if (author != null) {
                    this.author.setText(author.getTitle());
                    authorThumb.setCursor(Cursor.HAND);
                    authorThumb.setOnMouseClicked(me -> browserUtil.open(author.toYouTubeLink()));
                    authorThumb.setImage(aThumb);
                } else {
                    this.author.setText("Error: Null Channel");
                    authorThumb.setCursor(Cursor.DEFAULT);
                    authorThumb.setOnMouseClicked(null);
                    authorThumb.setImage(aThumb);

                    logger.error("Channel for video was null [id={}]", video.getChannelId());
                }
            });
        } catch (SQLException e) {
            logger.error("Failed to load YouTubeVideo", e);
        }
    }

    /**
     * Validation of page number input.
     */
    private int interpretPageValue(String value) {
        value = value.replaceAll("[^\\d]", "").trim();
        if (value.isEmpty()) {
            value = "1";
        }
        return Integer.parseInt(value);
    }

    /**
     * Submits an unforced page search
     *
     * @param page page value
     */
    private void submitPageValue(int page) {
        submitPageValue(page, false);
    }

    /**
     * Validates submitted page value, updates properties, and performs comment search.
     *
     * @param page   page value
     * @param forced perform search regardless of page value equal to current page
     */
    private void submitPageValue(int page, boolean forced) {
        if (page < 1) {
            page = 1;
        } else if (page > maxPageProperty.getValue()) {
            page = maxPageProperty.getValue();
        }

        logger.debug("Attempting comment search [submittedPage={},queryPage={}]", page, commentQuery.getPageNum());

        if (page - 1 != commentQuery.getPageNum() || forced) {
            logger.debug("Changing page {} -> {}", commentQuery.getPageNum(), page - 1);

            new Thread(this::searchComments).start();
        } else {
            logger.debug("Didn't do search");
        }

        final int newPage = page;
        runLater(() -> {
            pageValue.setEditable(false);
            pageValue.getStyleClass().add("clearTextField");
            pageValue.setText(String.valueOf(newPage));
            pageProperty.setValue(newPage);
        });

        FXUtils.adjustTextFieldWidthByContent(pageValue);
    }

    /**
     * Queries the database for comments using search parameters:
     * - group / groupItem / specific video(s)
     * - page
     * - comment type
     * - sorting order
     * - name like (SQL wildcards accepted)
     * - comment like (SQL wildcards accepted)
     * - date range (from start to end)
     * <p>
     * Displays results in the ListView
     */
    private void searchComments() {
        runLater(() -> searchingProperty.setValue(true));

        resultsList.getItems().forEach(SearchCommentsListItem::cleanUp);

        try {
            int pageNum = pageProperty.getValue();

            GroupItem selectedItem = comboGroupItemSelect.getValue();
            YouTubeVideo selectedVideo = videoSelectModal.getContent().getSelectedVideo();

            elapsedTime.setNow();
            lastResultsList = commentQuery.setGroup(comboGroupSelect.getValue())
                    .setGroupItem(Optional.ofNullable(GroupItem.ALL_ITEMS.equals(selectedItem.getId()) ?
                            null : selectedItem))
                    .setVideos(Optional.ofNullable(selectedVideo != null ?
                            Collections.singletonList(selectedVideo) : null))
                    .setCommentsType(comboCommentType.getValue())
                    .setOrder(comboOrderBy.getValue())
                    .setNameLike(nameLike.getText())
                    .setTextLike(commentLike.getText())
                    .setHasTags(hasTags.getText())
                    .setDateFrom(dateFrom.getValue())
                    .setDateTo(dateTo.getValue())
                    .getByPage(pageNum - 1, 500); // 1 in app = 0 in query

            logger.debug("Query completed [time={},comments={}]",
                    elapsedTime.humanReadableFormat(),
                    lastResultsList.size());

            setResultsList(lastResultsList, false);
        } catch (SQLException e) {
            logger.error("Failed to query comments from database", e);
        } finally {
            runLater(() -> searchingProperty.setValue(false));
        }
    }

    private void setResultsList(List<YouTubeComment> comments, boolean treeMode) {
        List<SearchCommentsListItem> commentListItems = comments.stream()
                .map(yc -> {
                    try {
                        SearchCommentsListItem item = new SearchCommentsListItem(yc);
                        if (treeMode) {
                            runLater(item::treeMode);
                        }
                        return item;
                    } catch (IOException e) {
                        logger.error("Failed to convert YouTubeComment to SearchCommentsListItem", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        runLater(() -> {
            resultsList.getItems().clear();
            resultsList.getItems().addAll(commentListItems);
            maxPageProperty.setValue(commentQuery.getPageCount());

            paginationPane.setManaged(!treeMode);
            paginationPane.setVisible(!treeMode);

            backToResultsPane.setVisible(treeMode);
            backToResultsPane.setManaged(treeMode);

            displayCount.setText(String.format("Showing %,d of %,d total",
                    comments.size(),
                    commentQuery.getTotalResults()));

            if (treeMode) {
                resultsList.scrollTo(0);

                Optional<SearchCommentsListItem> toSelect = resultsList.getItems().stream()
                        .filter(scli -> scli.getComment().getId().equals(originalTreeComment.getComment().getId()))
                        .findFirst();

                resultsList.getSelectionModel().select(toSelect.orElse(null));
            }
        });
    }

    private void selectListItem(SearchCommentsListItem item) {
        resultsList.getSelectionModel().clearSelection();
        resultsList.getSelectionModel().select(item);
    }

    @Subscribe
    public void showMoreEvent(final ShowMoreEvent showMoreEvent) {
        final SearchCommentsListItem listItem = showMoreEvent.getCommentListItem();
        final YouTubeComment comment = listItem.getComment();

        logger.debug("Showing more window for commment [videoId={},commentId={}]",
                comment.getVideoId(),
                comment.getId());

        selectListItem(listItem);
        openShowMoreModal(comment);
        showListItemContext(listItem);
    }

    private void openShowMoreModal(final YouTubeComment comment) {
        runLater(() -> {
            showMoreModal.setVisible(true);
            showMoreModal.setManaged(true);

            SCShowMoreModal modalContent = showMoreModal.getContent();
            modalContent.cleanUp();
            modalContent.loadComment(comment);
        });
    }

    @Subscribe
    public void viewTreeEvent(final ViewTreeEvent viewTreeEvent) {
        final SearchCommentsListItem listItem = originalTreeComment = viewTreeEvent.getCommentListItem();
        final YouTubeComment comment = listItem.getComment();
        final String parentId = comment.isReply() ? comment.getParentId() : comment.getId();

        logger.debug("Viewing comment reply tree [videoId={},commentId={},parentId={}]",
                comment.getVideoId(),
                comment.getId(),
                parentId);

        selectListItem(listItem);
        showListItemContext(listItem);

        runLater(() -> searchingProperty.setValue(true));

        try {
            setResultsList(database.getCommentTree(parentId, comboCommentType.getValue() == CommentQuery.CommentsType.MODERATED_ONLY), true);
        } catch (SQLException e) {
            logger.debug("Failed to view comment tree [commentId={},parentId={}]", comment.getId(),
                    parentId);
        } finally {
            runLater(() -> searchingProperty.setValue(false));
        }
    }

    @Subscribe
    public void groupDeleteEvent(final GroupDeleteEvent deleteEvent) {
        logger.debug("Group Delete Event");
        runLater(this::rebuildGroupSelect);
    }

    @Subscribe
    public void groupAddEvent(final GroupAddEvent addEvent) {
        logger.debug("Group Add Event");
        runLater(this::rebuildGroupSelect);
    }

    @Subscribe
    public void groupRenameEvent(final GroupRenameEvent renameEvent) {
        logger.debug("Group Rename Event");
        runLater(this::rebuildGroupSelect);
    }

    private void rebuildGroupSelect() {
        final Group selectedGroup = comboGroupSelect.getValue();
        final ObservableList<Group> groups = FXCollections.observableArrayList(database.groups().getAllGroups());
        comboGroupSelect.setItems(FXCollections.emptyObservableList());
        comboGroupSelect.setItems(groups);

        if (selectedGroup == null || comboGroupSelect.getValue() == null) {
            comboGroupSelect.getSelectionModel().select(0);
        } else if (groups.contains(selectedGroup)) {
            comboGroupSelect.setValue(selectedGroup);
        }
    }

    @Subscribe
    public void groupItemAddEvent(final GroupItemAddEvent groupItemAddEvent) {
        logger.debug("Group Item Add Event");
        reloadGroupItems();
    }

    @Subscribe
    public void groupItemDeleteEvent(final GroupItemDeleteEvent groupItemDeleteEvent) {
        logger.debug("Group Item Delete Event");
        reloadGroupItems();
    }

    private void reloadGroupItems() {
        final Group selectedGroup = comboGroupSelect.getValue();
        final List<GroupItem> groupItems = database.groupItems().byGroup(selectedGroup);

        final GroupItem all = new GroupItem()
                .setId(GroupItem.ALL_ITEMS)
                .setDisplayName(String.format("All Items (%s)", groupItems.size()));
        runLater(() -> {
            comboGroupItemSelect.getItems().clear();
            comboGroupItemSelect.getItems().add(all);
            comboGroupItemSelect.getItems().addAll(groupItems);
            comboGroupItemSelect.setDisable(false);
            comboGroupItemSelect.getSelectionModel().select(0);
            videoSelect.setDisable(false);
        });
    }

    public String readableNumber(double value) {
        char[] suffix = new char[]{'k', 'm', 'b', 't'};
        int pos = 0;
        while (value > 1000) {
            value /= 1000;
            pos++;
        }
        return String.format("%.1f%s", value, pos > 0 ? suffix[pos - 1] : "");
    }

}
