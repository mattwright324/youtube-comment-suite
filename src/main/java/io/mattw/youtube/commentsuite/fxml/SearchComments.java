package io.mattw.youtube.commentsuite.fxml;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.ImageCache;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.db.*;
import io.mattw.youtube.commentsuite.util.BrowserUtil;
import io.mattw.youtube.commentsuite.util.ClipboardUtil;
import io.mattw.youtube.commentsuite.util.DateUtils;
import io.mattw.youtube.commentsuite.util.ElapsedTime;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
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
import javafx.scene.layout.VBox;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

/**
 * @author mattwright324
 */
public class SearchComments implements Initializable, ImageCache {

    private static final Logger logger = LogManager.getLogger();

    private Cache<Object, YouTubeVideo> videoCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    @FXML private VBox contextPane, resultsPane, queryPane;
    @FXML private ImageView videoThumb, authorThumb, toggleContextIcon, toggleQueryIcon;
    @FXML private ImageView firstPageIcon, prevPageIcon, nextPageIcon, lastPageIcon;
    @FXML private ImageView likesIcon, dislikesIcon;
    @FXML private TextField videoTitle, author;
    @FXML private Label toggleContext, toggleQuery;
    @FXML private Label videoViews, videoLikes, videoDislikes;
    @FXML private TextArea videoDescription;

    @FXML private ImageView browserIcon;
    @FXML private MenuItem openInBrowser, copyNames, copyComments, copyChannelLinks, copyVideoLinks, copyCommentLinks;
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
    @FXML private TextField nameLike, commentLike;
    @FXML private DatePicker dateFrom, dateTo;
    @FXML private Button btnSearch, btnClear, btnExport;

    @FXML private OverlayModal<SCVideoSelectModal> videoSelectModal;
    @FXML private OverlayModal<SCShowMoreModal> showMoreModal;
    @FXML private OverlayModal<SCExportModal> exportModal;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private SimpleBooleanProperty searchingProperty = new SimpleBooleanProperty(false);
    private SimpleIntegerProperty pageProperty = new SimpleIntegerProperty();
    private SimpleIntegerProperty maxPageProperty = new SimpleIntegerProperty();
    private ElapsedTime elapsedTime = new ElapsedTime();
    private ChangeListener<Number> cl;

    private CommentDatabase database;
    private CommentQuery commentQuery;
    private List<YouTubeComment> lastResultsList;
    private SearchCommentsListItem actionComment;
    private ClipboardUtil clipboardUtil = new ClipboardUtil();
    private BrowserUtil browserUtil = new BrowserUtil();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        database = FXMLSuite.getDatabase();
        commentQuery = database.commentQuery();

        SelectionModel<Group> selectionModel = comboGroupSelect.getSelectionModel();
        comboGroupSelect.setItems(database.getGlobalGroupList());
        comboGroupSelect.getItems().addListener((ListChangeListener<Group>) (c -> {
            if (!comboGroupSelect.getItems().isEmpty() && selectionModel.getSelectedIndex() == -1) {
                runLater(() -> selectionModel.select(0));
            }
        }));
        selectionModel.selectedItemProperty().addListener((o, ov, nv) -> {
            if (ov != null) {
                ov.itemsUpdatedProperty().removeListener(cl);
                ov.itemsUpdatedProperty().unbind();
                ov.nameProperty().unbind();
            }
            if (nv != null) {
                nv.itemsUpdatedProperty().addListener(cl = (o1, ov2, nv3) -> {
                    List<GroupItem> groupItems = database.getGroupItems(nv);
                    GroupItem all = new GroupItem(GroupItem.ALL_ITEMS, String.format("All Items (%s)", groupItems.size()));
                    runLater(() -> {
                        comboGroupItemSelect.getItems().clear();
                        comboGroupItemSelect.getItems().add(all);
                        comboGroupItemSelect.getItems().addAll(groupItems);
                        comboGroupItemSelect.setDisable(false);
                        comboGroupItemSelect.getSelectionModel().select(0);
                        videoSelect.setDisable(false);
                    });
                });
                runLater(nv::reloadGroupItems);
            } else {
                runLater(() -> {
                    comboGroupItemSelect.setDisable(true);
                    videoSelect.setDisable(true);
                });
            }
        });

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

        likesIcon.setImage(ImageLoader.THUMBS_UP.getImage());
        dislikesIcon.setImage(ImageLoader.THUMBS_DOWN.getImage());

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
                checkUpdateThumbs(nv);
            }
        }).start());

        browserIcon.setImage(ImageLoader.BROWSER.getImage());
        openInBrowser.setOnAction(ae -> {
            String link = scSelection.getSelectedItem()
                    .getComment()
                    .buildYouTubeLink();
            browserUtil.open(link);
        });
        copyNames.setOnAction(ae -> {
            List<String> uniqueNames = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getChannel)
                    .map(YouTubeChannel::getTitle)
                    .distinct()
                    .collect(Collectors.toList());

            clipboardUtil.setClipboard(uniqueNames);
        });
        copyComments.setOnAction(ae -> {
            List<String> comments = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getCommentText)
                    .collect(Collectors.toList());

            clipboardUtil.setClipboard(comments);
        });
        copyChannelLinks.setOnAction(ae -> {
            List<String> uniqueChannelLinks = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getChannel)
                    .map(YouTubeChannel::buildYouTubeLink)
                    .distinct()
                    .collect(Collectors.toList());

            clipboardUtil.setClipboard(uniqueChannelLinks);
        });
        copyCommentLinks.setOnAction(ae -> {
            List<String> uniqueCommentLinks = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::buildYouTubeLink)
                    .distinct()
                    .collect(Collectors.toList());

            clipboardUtil.setClipboard(uniqueCommentLinks);
        });
        copyVideoLinks.setOnAction(ae -> {
            List<String> uniqueVideoLinks = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(c -> String.format("https://youtu.be/%s", c.getVideoId()))
                    .distinct()
                    .collect(Collectors.toList());

            clipboardUtil.setClipboard(uniqueVideoLinks);
        });

        btnBackToResults.setOnAction(ae -> {
            setResultsList(lastResultsList, false);

            runLater(() -> {
                Optional<SearchCommentsListItem> toSelect = resultsList.getItems().stream()
                        .filter(scli -> scli.getComment().getId().equals(actionComment.getComment().getId()))
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
        scVideoSelectModal.getBtnClose().setOnAction(ae -> videoSelectModal.setVisible(false));
        scVideoSelectModal.getBtnSubmit().setOnAction(ae -> videoSelectModal.setVisible(false));
        comboGroupSelect.valueProperty().addListener((o, ov, nv) -> scVideoSelectModal.reset());
        comboGroupItemSelect.valueProperty().addListener((o, ov, nv) -> scVideoSelectModal.reset());

        SCShowMoreModal scShowMoreModal = new SCShowMoreModal();
        showMoreModal.setContent(scShowMoreModal);
        scShowMoreModal.getBtnClose().setOnAction(ae -> showMoreModal.setVisible(false));
        scShowMoreModal.replyModeProperty().addListener((o, ov, nv) -> runLater(() ->
                showMoreModal.getModalContainer().setMaxWidth(420 * (nv ? 2 : 1))
        ));

        SCExportModal scExportModal = new SCExportModal();
        exportModal.setContent(scExportModal);
        exportModal.getModalContainer().setMaxWidth(exportModal.getModalContainer().getMaxWidth() * 1.5);
        scExportModal.getBtnClose().setOnAction(ae -> exportModal.setVisible(false));
        btnExport.setOnAction(ae -> {
            scExportModal.withQuery(commentQuery);
            scExportModal.cleanUp();
            exportModal.setVisible(true);
        });
    }

    private void checkUpdateThumbs(SearchCommentsListItem commentItem) {
        YouTubeComment comment = commentItem.getComment();

        commentItem.loadProfileThumb();

        for (SearchCommentsListItem scli : resultsList.getItems()) {
            scli.checkProfileThumb();
        }

        try {
            String videoId = comment.getVideoId();
            YouTubeVideo video = videoCache.getIfPresent(videoId);
            if (video == null) {
                video = database.getVideo(videoId);
                videoCache.put(videoId, video);
            }

            final YouTubeVideo v = video;
            final YouTubeChannel va = database.getChannel(video.getChannelId());
            runLater(() -> {
                author.setText(va.getTitle());
                videoTitle.setText(v.getTitle());
                videoLikes.setText(trunc(v.getLikes()));
                videoDislikes.setText(trunc(v.getDislikes()));
                videoViews.setText(String.format("%s views", trunc(v.getViewCount())));
                videoDescription.setText(String.format("Published %s • %s",
                        formatter.format(DateUtils.epochMillisToDateTime(v.getPublishedDate())),
                        StringEscapeUtils.unescapeHtml4(v.getDescription())));

                videoThumb.setCursor(Cursor.HAND);
                videoThumb.setOnMouseClicked(me -> browserUtil.open(v.buildYouTubeLink()));
                authorThumb.setCursor(Cursor.HAND);
                authorThumb.setOnMouseClicked(me -> browserUtil.open(va.buildYouTubeLink()));
            });

            Image vthumb = ImageCache.findOrGetImage(video);
            Image athumb = ImageCache.findOrGetImage(va);
            runLater(() -> {
                videoThumb.setImage(vthumb);
                authorThumb.setImage(athumb);
            });
        } catch (SQLException e) {
            logger.error("Failed to load YouTubeVideo", e);
        }
    }

    private int interpretPageValue(String value) {
        value = value.replaceAll("[^\\d]", "").trim();
        if (value.isEmpty()) {
            value = "1";
        }
        return Integer.valueOf(value);
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

        if (page-1 != commentQuery.getPageNum() || forced) {
            logger.debug("Changing page {} -> {}", commentQuery.getPageNum(), page-1);

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
                    .setDateFrom(dateFrom.getValue())
                    .setDateTo(dateTo.getValue())
                    .getByPage(pageNum-1, 500); // 1 in app = 0 in query

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

        commentListItems.forEach(comment -> {
            comment.getShowMore().setOnAction(ae -> showMore(comment));
            comment.getReply().setOnAction(ae -> reply(comment));
            comment.getViewTree().setOnAction(ae -> viewTree(comment));
        });

        runLater(() -> {
            resultsList.getItems().clear();
            resultsList.getItems().addAll(commentListItems);
            maxPageProperty.setValue(commentQuery.getPageCount());

            btnExport.setDisable(commentQuery.getTotalResults() == 0);

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
                        .filter(scli -> scli.getComment().getId().equals(actionComment.getComment().getId()))
                        .findFirst();

                resultsList.getSelectionModel().select(toSelect.orElse(null));
            }
        });
    }

    private void selectAndShowContext(SearchCommentsListItem item) {
        resultsList.getSelectionModel().clearSelection();
        resultsList.getSelectionModel().select(item);
    }

    private void showMore(SearchCommentsListItem scli) {
        selectAndShowContext(scli);

        YouTubeComment comment = scli.getComment();

        logger.debug("Showing more window for commment [videoId={},commentId={}]",
                comment.getVideoId(),
                comment.getId());

        checkUpdateThumbs(scli);
        commentModal(comment, false);
    }

    private void reply(SearchCommentsListItem scli) {
        selectAndShowContext(scli);

        YouTubeComment comment = scli.getComment();

        logger.debug("Showing reply window for commment [videoId={},commentId={}]",
                comment.getVideoId(),
                comment.getId());

        checkUpdateThumbs(scli);
        commentModal(comment, true);
    }

    private void commentModal(YouTubeComment comment, boolean replyMode) {
        runLater(() -> {
            showMoreModal.setVisible(true);
            showMoreModal.setManaged(true);

            SCShowMoreModal modalContent = showMoreModal.getContent();
            modalContent.cleanUp();
            modalContent.loadComment(comment, replyMode);
        });
    }

    private void viewTree(SearchCommentsListItem scli) {
        selectAndShowContext(scli);

        runLater(() -> searchingProperty.setValue(true));

        actionComment = scli;

        checkUpdateThumbs(scli);

        YouTubeComment comment = scli.getComment();

        String parentId = comment.isReply() ? comment.getParentId() : comment.getId();

        logger.debug("Viewing comment reply tree [videoId={},commentId={},parentId={}]",
                comment.getVideoId(),
                comment.getId(),
                parentId);

        try {
            List<YouTubeComment> comments = database.getCommentTree(parentId);

            setResultsList(comments, true);
        } catch (SQLException e) {
            logger.debug("Failed to view comment tree [commentId={},parentId={}]", comment.getId(),
                    parentId);
        } finally {
            runLater(() -> searchingProperty.setValue(false));
        }
    }

    /**
     * Truncates a number with shorthand:
     * 2000       -> 2.0k
     * 5422000    -> 54.2k
     * 123456789  -> 123.5m
     * 1234567890 -> 1.2b
     */
    public String trunc(double value) {
        char[] suffix = new char[]{'k', 'm', 'b', 't'};
        int pos = 0;
        while (value > 1000) {
            value /= 1000;
            pos++;
        }
        return String.format("%.1f%s", value, pos > 0 ? suffix[pos - 1] : "");
    }
}
