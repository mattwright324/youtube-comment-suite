package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.db.*;
import mattw.youtube.commentsuite.io.BrowserUtil;
import mattw.youtube.commentsuite.io.ClipboardUtil;
import mattw.youtube.commentsuite.io.ElapsedTime;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class SearchComments implements Initializable, ImageCache {

    private static Logger logger = LogManager.getLogger(SearchComments.class);

    private Cache<Object, YouTubeVideo> videoCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    private @FXML VBox contextPane, resultsPane, queryPane;
    private @FXML ImageView videoThumb, authorThumb, toggleContextIcon, toggleQueryIcon;
    private @FXML ImageView firstPageIcon, prevPageIcon, nextPageIcon, lastPageIcon;
    private @FXML ImageView likesIcon, dislikesIcon;
    private @FXML TextField videoTitle, author;
    private @FXML Label toggleContext, toggleQuery;
    private @FXML Label videoViews, videoLikes, videoDislikes;
    private @FXML TextArea videoDescription;

    private @FXML MenuItem openInBrowser, copyNames, copyComments, copyChannelLinks, copyVideoLinks, copyCommentLinks;
    private @FXML ListView<SearchCommentsListItem> resultsList;
    private @FXML TextField pageValue;
    private @FXML Label displayCount, lblMaxPage;
    private @FXML Button btnFirst, btnPrev, btnNext, btnLast;
    private @FXML Button btnBackToResults;
    private @FXML HBox paginationPane, backToResultsPane;

    private @FXML ComboBox<Group> comboGroupSelect;
    private @FXML ComboBox<GroupItem> comboGroupItemSelect;
    private @FXML Hyperlink videoSelect;
    private @FXML ComboBox<String> comboCommentType;
    private @FXML ComboBox<String> comboOrderBy;
    private @FXML TextField nameLike, commentLike;
    private @FXML DatePicker dateFrom, dateTo;
    private @FXML Button btnSearch, btnClear;

    private @FXML OverlayModal<SCVideoSelectModal> videoSelectModal;
    private @FXML OverlayModal<SCShowMoreModal> showMoreModal;

    private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
    private SimpleBooleanProperty searchingProperty = new SimpleBooleanProperty(false);
    private SimpleIntegerProperty pageProperty = new SimpleIntegerProperty();
    private SimpleIntegerProperty maxPageProperty = new SimpleIntegerProperty();
    private ElapsedTime elapsedTime = new ElapsedTime();

    private CommentDatabase database;
    private CommentQuery query;
    private List<YouTubeComment> lastResultsList;
    private ClipboardUtil clipboardUtil = new ClipboardUtil();
    private BrowserUtil browserUtil = new BrowserUtil();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        database = FXMLSuite.getDatabase();
        query = database.commentQuery();

        SelectionModel<Group> selectionModel = comboGroupSelect.getSelectionModel();
        comboGroupSelect.setItems(database.globalGroupList);
        comboGroupSelect.getItems().addListener((ListChangeListener<Group>)(c -> {
            if(!comboGroupSelect.getItems().isEmpty() && selectionModel.getSelectedIndex() == -1) {
                runLater(() -> selectionModel.select(0));
            }
        }));
        selectionModel.selectedItemProperty().addListener((o, ov, nv) -> {
            if(nv == null) {
                runLater(() -> {
                    comboGroupItemSelect.setDisable(true);
                    videoSelect.setDisable(true);
                });
            } else {
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
            }
        });

        resultsPane.disableProperty().bind(searchingProperty);
        queryPane.disableProperty().bind(searchingProperty);

        comboCommentType.getItems().addAll("Comments and Replies", "Comments Only", "Replies Only");
        comboCommentType.getSelectionModel().select(0);
        comboOrderBy.getItems().addAll("Most Recent", "Least Recent", "Most Likes", "Most Replies",
                "Longest Comment", "Names (A to Z)", "Comments (A to Z)");
        comboOrderBy.getSelectionModel().select(0);

        dateFrom.setValue(LocalDate.of(1970,1,1));
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
            if(!nv) {
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
            if(ke.getCode() == KeyCode.ENTER || ke.getCode() == KeyCode.SPACE) {
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
            if(nv != null) {
                checkUpdateThumbs(nv);
            }
        }).start());

        openInBrowser.setOnAction(ae -> {
            String link = scSelection.getSelectedItem()
                    .getComment()
                    .getYouTubeLink();
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
                    .map(YouTubeComment::getText)
                    .collect(Collectors.toList());

            clipboardUtil.setClipboard(comments);
        });
        copyChannelLinks.setOnAction(ae -> {
            List<String> uniqueChannelLinks = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getChannel)
                    .map(YouTubeChannel::getYouTubeLink)
                    .distinct()
                    .collect(Collectors.toList());

            clipboardUtil.setClipboard(uniqueChannelLinks);
        });
        copyCommentLinks.setOnAction(ae -> {
            List<String> uniqueCommentLinks = scSelection.getSelectedItems()
                    .stream()
                    .map(SearchCommentsListItem::getComment)
                    .map(YouTubeComment::getYouTubeLink)
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

        btnBackToResults.setOnAction(ae -> setResultsList(lastResultsList, false));

        SCVideoSelectModal scVideoSelectModal = new SCVideoSelectModal();
        videoSelectModal.setContent(scVideoSelectModal);
        videoSelectModal.getModalContainer().setMaxWidth(Double.MAX_VALUE);
        videoSelectModal.showSpacers(false);
        videoSelectModal.setPadding(new Insets(25));
        HBox.setHgrow(videoSelectModal.getModalContainer(), Priority.ALWAYS);
        videoSelect.setOnAction(ae -> runLater(() -> {
            Group group = comboGroupSelect.getValue();
            GroupItem groupItem = comboGroupItemSelect.getValue();

            scVideoSelectModal.loadWith(group, groupItem);
            scVideoSelectModal.cleanUp();
            videoSelectModal.setVisible(true);
        }));
        scVideoSelectModal.getBtnClose().setOnAction(ae -> videoSelectModal.setVisible(false));
        scVideoSelectModal.getBtnSubmit().setOnAction(ae -> videoSelectModal.setVisible(false));

        SCShowMoreModal scShowMoreModal = new SCShowMoreModal();
        showMoreModal.setContent(scShowMoreModal);
        scShowMoreModal.getBtnClose().setOnAction(ae -> showMoreModal.setVisible(false));
        scShowMoreModal.replyModeProperty().addListener((o, ov, nv) -> runLater(() ->
            showMoreModal.getModalContainer().setMaxWidth(420 * (nv ? 2 : 1))
        ));
    }

    private void checkUpdateThumbs(SearchCommentsListItem commentItem) {
        YouTubeComment comment = commentItem.getComment();

        commentItem.loadProfileThumb();

        for(SearchCommentsListItem scli : resultsList.getItems()) {
            scli.checkProfileThumb();
        }

        try {
            String videoId = comment.getVideoId();
            YouTubeVideo video = videoCache.getIfPresent(videoId);
            if(video == null) {
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
                videoViews.setText(String.format("%s views", trunc(v.getViews())));
                videoDescription.setText(String.format("Published %s • %s",
                        sdf.format(v.getPublishedDate()),
                        StringEscapeUtils.unescapeHtml4(v.getDescription())));

                videoThumb.setCursor(Cursor.HAND);
                videoThumb.setOnMouseClicked(me -> browserUtil.open(v.getYouTubeLink()));
                authorThumb.setCursor(Cursor.HAND);
                authorThumb.setOnMouseClicked(me -> browserUtil.open(va.getYouTubeLink()));
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

    /**
     * Converts dateTime to Pacific Time (PDT) as it is where YouTube is headquartered.
     *
     * @param date LocalDate value
     * @param midnight beginning of day (false) or end of day midnight (true)
     * @return epoch millis of LocalDate in Pacific Time
     */
    private long localDateToEpochMillis(LocalDate date, boolean midnight) {
        LocalDateTime dateTime = midnight ?
                date.atTime(23, 59, 59) : date.atTime(0, 0, 0);
        return dateTime.atZone(ZoneId.of("America/Los_Angeles"))
                .toInstant()
                .toEpochMilli();
    }

    private int interpretPageValue(String value) {
        value = value.replaceAll("[^0-9]", "").trim();
        if(value.isEmpty()) {
            value = "1";
        }
        return Integer.valueOf(value);
    }

    /**
     * Submits an unforced page search
     * @param page page value
     */
    private void submitPageValue(int page) {
        submitPageValue(page, false);
    }

    /**
     * Validates submitted page value, updates properties, and performs comment search.
     * @param page page value
     * @param forced perform search regardless of page value equal to current page
     */
    private void submitPageValue(int page, boolean forced) {
        logger.debug(String.format("Submit page value = %s", page));
        if(page < 1) {
            page = 1;
        } else if(page > maxPageProperty.getValue()) {
            page = maxPageProperty.getValue();
        }
        final int newPage = page;
        runLater(() -> {
            pageValue.setEditable(false);
            pageValue.getStyleClass().add("clearTextField");
            pageValue.setText(String.valueOf(newPage));
            pageProperty.setValue(newPage);
            if(newPage != query.getPage() || forced) {
                logger.debug(String.format("Changing page %s -> %s", query.getPage(), newPage));
                new Thread(this::searchComments).start();
            }
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
     *
     * Displays results in the ListView
     */
    private void searchComments() {
        runLater(() -> searchingProperty.setValue(true));

        resultsList.getItems().forEach(SearchCommentsListItem::cleanUp);

        try {
            int page = pageProperty.getValue();
            Group group = comboGroupSelect.getSelectionModel().getSelectedItem();
            GroupItem groupItem = comboGroupItemSelect.getSelectionModel().getSelectedItem();
            groupItem = GroupItem.ALL_ITEMS.equals(groupItem.getYoutubeId()) ? null : groupItem;

            elapsedTime.setNow();
            lastResultsList = query
                    .ctype(comboCommentType.getSelectionModel().getSelectedIndex())
                    .orderBy(comboOrderBy.getSelectionModel().getSelectedIndex())
                    .textLike(commentLike.getText())
                    .nameLike(nameLike.getText())
                    .before(localDateToEpochMillis(dateTo.getValue(), true))
                    .after(localDateToEpochMillis(dateFrom.getValue(), false))
                    .get(page, group, groupItem, null);
            logger.debug(String.format("Query completed [time=%s,comments=%s]",
                    elapsedTime.getElapsedString(),
                    lastResultsList.size()));

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
                        if(treeMode) {
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
            maxPageProperty.setValue(query.getPageCount());

            paginationPane.setManaged(!treeMode);
            paginationPane.setVisible(!treeMode);

            backToResultsPane.setVisible(treeMode);
            backToResultsPane.setManaged(treeMode);

            displayCount.setText(String.format("Showing %,d of %,d total",
                    comments.size(),
                    query.getTotalResults()));
        });
    }


    private void showMore(SearchCommentsListItem scli) {
        YouTubeComment comment = scli.getComment();

        logger.debug(String.format("Showing more window for commment [videoId=%s,commentId=%s]",
                comment.getVideoId(),
                comment.getYoutubeId()));

        checkUpdateThumbs(scli);
        commentModal(comment, false);
    }

    private void reply(SearchCommentsListItem scli) {
        YouTubeComment comment = scli.getComment();

        logger.debug(String.format("Showing reply window for commment [videoId=%s,commentId=%s]",
                comment.getVideoId(),
                comment.getYoutubeId()));

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
        runLater(() -> searchingProperty.setValue(true));

        checkUpdateThumbs(scli);

        YouTubeComment comment = scli.getComment();

        String parentId = comment.isReply() ? comment.getParentId() : comment.getYoutubeId();

        logger.debug(String.format("Viewing comment reply tree [videoId=%s,commentId=%s,parentId=%s]",
                comment.getVideoId(),
                comment.getYoutubeId(),
                parentId));

        try {
            List<YouTubeComment> comments = database.getCommentTree(parentId);

            setResultsList(comments, true);
        } catch (SQLException e) {
            logger.debug(String.format("Failed to view comment tree [commentId=%s,parentId=%s]", comment.getYoutubeId(),
                    parentId));
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
        while(value > 1000) {
            value /= 1000;
            pos++;
        }
        return String.format("%.1f%s", value, pos > 0 ? suffix[pos-1] : "");
    }
}
