package io.mattw.youtube.commentsuite.fxml;

import com.google.common.eventbus.Subscribe;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.ImageCache;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.db.GroupItem;
import io.mattw.youtube.commentsuite.db.GroupStats;
import io.mattw.youtube.commentsuite.events.GroupItemAddEvent;
import io.mattw.youtube.commentsuite.events.GroupItemDeleteEvent;
import io.mattw.youtube.commentsuite.util.DateUtils;
import io.mattw.youtube.commentsuite.util.FXUtils;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;
import static javafx.application.Platform.runLater;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * Manages a specific group; refreshing, stats, renaming, deletion, adding group items, etc.
 * <p>
 * Loads template FXML and displays info from database.
 */
public class ManageGroupsManager extends StackPane implements ImageCache, Cleanable {

    private static final Logger logger = LogManager.getLogger();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yy");

    private final Image edit = ImageLoader.PENCIL.getImage();
    private final Image close = ImageLoader.CLOSE.getImage();
    private final Image save = ImageLoader.SAVE.getImage();

    private static final String STAT_TOTAL_COMMENTS = "STAT_TOTAL_COMMENTS";
    private static final String STAT_TOTAL_LIKES = "STAT_TOTAL_LIKES";
    private static final String STAT_UNIQUE_VIEWERS = "STAT_UNIQUE_VIEWERS";
    private static final String STAT_TOTAL_VIDEOS = "STAT_TOTAL_VIDEOS";
    private static final String STAT_TOTAL_VIEWS = "STAT_TOTAL_VIEWS";
    private static final String STAT_VIDEO_LIKES = "STAT_VIDEO_LIKES";

    private final Group group;
    private final CommentDatabase database;
    private final ConfigData configData;

    private final Map<String, Label> statLabel = new HashMap<>();
    private ChangeListener<Font> fontListener;

    private Duration latestDiff = Duration.ZERO;

    @FXML
    private OverlayModal<MGMVRefreshModal> refreshModal;
    @FXML
    private OverlayModal<MGMVDeleteGroupModal> deleteModal;
    @FXML
    private OverlayModal<MGMVAddItemModal> addItemModal;
    @FXML
    private OverlayModal<MGMVRemoveSelectedModal> removeItemModal;
    @FXML
    private OverlayModal<MGMVRemoveAllModal> removeAllModal;
    @FXML
    private Button btnAddItem;
    @FXML
    private Button btnRemoveItems;
    @FXML
    private Button btnRemoveAll;
    @FXML
    private ListView<MGMVGroupItemView> groupItemList;

    @FXML
    private TextField groupTitle;
    @FXML
    private ImageView editIcon, closeIcon;
    @FXML
    private Hyperlink rename, renameCancel;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnReload;
    @FXML
    private Button btnDelete;
    @FXML
    private Label refreshStatus;

    @FXML
    private LineChart<String, Number> commentsLineChart, videosLineChart;
    private LineChart.Series<String, Number> commentsLineChartData, videosLineChartData;
    @FXML
    private ListView<MGMVStatItem> popularVideosList, commentedVideosList,
            disabledVideosList, popularViewersList, activeViewersList;
    @FXML
    private GridPane commentStatPane, videoStatPane;

    @FXML
    private Accordion accordion;
    @FXML
    private TitledPane generalPane, videoPane, viewerPane;

    public ManageGroupsManager(final Group group) throws IOException {
        logger.debug("Initialize for Group [id={},name={}]", group.getGroupId(), group.getName());

        database = CommentSuite.getDatabase();
        configData = CommentSuite.getConfig().getDataObject();

        CommentSuite.getEventBus().register(this);

        this.group = group;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("ManageGroupsManager.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        Random random = new Random();
        this.setStyle(String.format("-fx-background-color: linear-gradient(to top, rgba(%s,%s,%s,%s), transparent);",
                220 - random.nextInt(60), 220 - random.nextInt(60), 220 - random.nextInt(60), 0.4));

        reloadGroupItems("init");

        accordion.setExpandedPane(generalPane);

        commentsLineChartData = new LineChart.Series<>();
        commentsLineChart.getData().add(commentsLineChartData);

        videosLineChartData = new LineChart.Series<>();
        videosLineChart.getData().add(videosLineChartData);

        int cIndex = 1, vIndex = 1;
        createGridRow(commentStatPane, STAT_TOTAL_COMMENTS, cIndex++, "Total Comments");
        createGridRow(commentStatPane, STAT_TOTAL_LIKES, cIndex++, "Total Likes");
        statLabel.get(STAT_TOTAL_LIKES).setStyle("-fx-text-fill:cornflowerblue");
        createGridRow(commentStatPane, STAT_UNIQUE_VIEWERS, cIndex++, "Unique Viewers");
        createGridRow(videoStatPane, STAT_TOTAL_VIDEOS, vIndex++, "Total Videos");
        createGridRow(videoStatPane, STAT_TOTAL_VIEWS, vIndex++, "Total Views");
        createGridRow(videoStatPane, STAT_VIDEO_LIKES, vIndex++, "Total Video Likes");
        statLabel.get(STAT_VIDEO_LIKES).setStyle("-fx-text-fill:cornflowerblue");

        editIcon.setImage(edit);
        closeIcon.setImage(close);

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        updateLastRefreshed();
                    }
                }, 0, Duration.ofSeconds(30).toMillis()
        );

        FXUtils.registerToSize(groupTitle, 18);
        FXUtils.registerToPadding(groupTitle, 10.5);

        groupTitle.setMinWidth(Region.USE_PREF_SIZE);
        groupTitle.setMaxWidth(Region.USE_PREF_SIZE);
        groupTitle.textProperty().addListener((ov, prevText, currText) -> {
            runLater(() -> rename.setDisable(isBlank(groupTitle.getText())));

            FXUtils.adjustTextFieldWidthByContent(groupTitle);
        });
        groupTitle.fontProperty().addListener(fontListener = (o, ov, nv) -> {
            FXUtils.adjustTextFieldWidthByContent(groupTitle);

            // One-time font listener resize.
            // Will match content after font set on label from styleClass.
            // If not removed, when clicking the 'Rename' button, the label will
            // flicker once between Font size 15 (default) and back to the styleClass font size
            // every time the edit button is clicked.
            groupTitle.fontProperty().removeListener(fontListener);
        });
        groupTitle.setText(group.getName());
        groupTitle.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ENTER) {
                rename.fire();
            }
        });

        rename.setOnAction(ae -> new Thread(() -> {
            if (editIcon.getImage().equals(save)) {
                try {
                    final String newTitle = trimToEmpty(groupTitle.getText());
                    database.groups().rename(group, newTitle);
                    runLater(() -> groupTitle.setText(newTitle));
                } catch (SQLException e) {
                    runLater(() -> groupTitle.setText(group.getName()));
                    logger.error(e);
                }
            }
            runLater(() -> {
                if (editIcon.getImage().equals(edit)) {
                    editIcon.setImage(save);
                    groupTitle.getStyleClass().remove("clearTextField");
                    groupTitle.setEditable(true);
                    rename.setTooltip(new Tooltip("Save Changes"));

                    renameCancel.setVisible(true);
                    renameCancel.setManaged(true);
                    renameCancel.setDisable(false);
                } else {
                    editIcon.setImage(edit);
                    groupTitle.getStyleClass().add("clearTextField");
                    groupTitle.setEditable(false);
                    rename.setTooltip(new Tooltip("Rename"));

                    renameCancel.setVisible(false);
                    renameCancel.setManaged(false);
                    renameCancel.setDisable(true);
                }
            });
        }).start());

        renameCancel.setOnAction(ae -> new Thread(() -> runLater(() -> {
            editIcon.setImage(edit);
            groupTitle.getStyleClass().add("clearTextField");
            groupTitle.setEditable(false);
            rename.setTooltip(new Tooltip("Rename"));

            renameCancel.setVisible(false);
            renameCancel.setManaged(false);
            renameCancel.setDisable(true);

            groupTitle.setText(group.getName());

            FXUtils.adjustTextFieldWidthByContent(groupTitle);
        })).start());
        renameCancel.setTooltip(new Tooltip("Cancel"));

        SelectionModel selectionModel = groupItemList.getSelectionModel();
        ((MultipleSelectionModel) selectionModel).setSelectionMode(SelectionMode.MULTIPLE);
        ((MultipleSelectionModel) selectionModel).getSelectedItems().addListener((ListChangeListener) (lcl) ->
                runLater(() -> {
                    int items = lcl.getList().size();
                    btnRemoveItems.setText(String.format("Remove (%s)", items));
                    btnRemoveItems.setDisable(items <= 0);
                })
        );
        groupItemList.getItems().addListener((ListChangeListener<MGMVGroupItemView>) (lcl) ->
                runLater(() -> {
                    int items = lcl.getList().size();
                    btnRemoveAll.setText(String.format("Remove All (%s)", items));
                    btnRemoveAll.setDisable(items <= 0);
                })
        );

        btnReload.setOnAction(ae -> new Thread(() -> {
            reloadGroupItems("btnReload");
            try {
                reloadStats();
            } catch (SQLException e) {
                logger.error("An error occured during group reload", e);
            }
        }).start());

        if (configData.isAutoLoadStats()) {
            btnReload.fire();
        }

        /*
          Refresh Modal
         */
        MGMVRefreshModal mgmvRefresh = new MGMVRefreshModal(this, group);
        refreshModal.setContent(mgmvRefresh);
        btnRefresh.setOnAction(ae -> runLater(() -> {
            mgmvRefresh.reset();
            refreshModal.setVisible(true);
        }));
        refreshModal.visibleProperty().addListener((cl) -> {
            mgmvRefresh.getBtnClose().setCancelButton(refreshModal.isVisible());
            mgmvRefresh.getBtnStart().setDefaultButton(refreshModal.isVisible());
        });
        mgmvRefresh.getBtnClose().setOnAction(ae -> {
            refreshModal.setVisible(false);
            updateLastRefreshed();
            if (mgmvRefresh.isHasBeenStarted()) {
                btnReload.fire();
            }
        });
        mgmvRefresh.getErrorList().managedProperty().addListener((o, ov, nv) -> {
            if (nv) {
                runLater(() -> refreshModal.getModalContainer().setMaxWidth(MGMVRefreshModal.WIDTH + 250));
            } else {
                runLater(() -> refreshModal.getModalContainer().setMaxWidth(MGMVRefreshModal.WIDTH));
            }
        });

        /*
          Delete Group Modal
         */
        MGMVDeleteGroupModal mgmvDelete = new MGMVDeleteGroupModal();
        deleteModal.setContent(mgmvDelete);
        deleteModal.setDividerClass("dividerDanger");
        btnDelete.setOnAction(ae -> runLater(() -> deleteModal.setVisible(true)));
        deleteModal.visibleProperty().addListener((cl) -> {
            mgmvDelete.getBtnClose().setCancelButton(deleteModal.isVisible());
            mgmvDelete.getBtnDelete().setDefaultButton(deleteModal.isVisible());
        });
        mgmvDelete.getBtnDelete().setOnAction(ae -> {
            deleteModal.setVisible(false);
            btnReload.fire();
        });
        mgmvDelete.getBtnDelete().setOnAction(ae -> new Thread(() -> {
            runLater(() -> {
                mgmvDelete.getBtnDelete().setDisable(true);
                mgmvDelete.getBtnClose().setDisable(true);
            });

            try {
                logger.warn("Deleting Group[id={},name={}]", group.getGroupId(), group.getName());
                database.groups().delete(this.group);
            } catch (SQLException e) {
                logger.error("Failed to delete group.", e);
            }

            runLater(() -> {
                mgmvDelete.getBtnDelete().setDisable(false);
                mgmvDelete.getBtnClose().setDisable(false);
                deleteModal.setVisible(false);
                btnReload.fire();
            });
        }).start());
        mgmvDelete.getBtnClose().setOnAction(ae -> deleteModal.setVisible(false));

        /*
          Add Item Modal
         */
        MGMVAddItemModal mgmvAddItem = new MGMVAddItemModal(group);
        addItemModal.setContent(mgmvAddItem);
        btnAddItem.setOnAction(ae -> runLater(() -> {
            mgmvAddItem.cleanUp();
            addItemModal.setVisible(true);
        }));
        addItemModal.visibleProperty().addListener((cl) -> {
            mgmvAddItem.getBtnClose().setCancelButton(addItemModal.isVisible());
            mgmvAddItem.getBtnSubmit().setDefaultButton(addItemModal.isVisible());
        });
        mgmvAddItem.getBtnClose().setOnAction(ae -> addItemModal.setVisible(false));

        /*
          Remove Selected GroupItems Modal
         */
        MGMVRemoveSelectedModal mgmvRemoveSelected = new MGMVRemoveSelectedModal(group, groupItemList.getSelectionModel());
        removeItemModal.setContent(mgmvRemoveSelected);
        removeItemModal.setDividerClass("dividerWarning");
        btnRemoveItems.setOnAction(ae -> runLater(() -> {
            mgmvRemoveSelected.cleanUp();
            removeItemModal.setVisible(true);
        }));
        removeItemModal.visibleProperty().addListener((cl) -> {
            mgmvRemoveSelected.getBtnClose().setCancelButton(removeItemModal.isVisible());
            mgmvRemoveSelected.getBtnSubmit().setDefaultButton(removeItemModal.isVisible());
        });
        mgmvRemoveSelected.getBtnClose().setOnAction(ae -> removeItemModal.setVisible(false));

        /*
          Remove All GroupItems Modal
         */
        MGMVRemoveAllModal mgmvRemoveAll = new MGMVRemoveAllModal(group, groupItemList.getItems());
        removeAllModal.setContent(mgmvRemoveAll);
        removeAllModal.setDividerClass("dividerDanger");
        btnRemoveAll.setOnAction(ae -> runLater(() -> {
            mgmvRemoveAll.cleanUp();
            removeAllModal.setVisible(true);
        }));
        removeAllModal.visibleProperty().addListener((cl) -> {
            mgmvRemoveAll.getBtnClose().setCancelButton(removeAllModal.isVisible());
            mgmvRemoveAll.getBtnSubmit().setDefaultButton(removeAllModal.isVisible());
        });
        mgmvRemoveAll.getBtnClose().setOnAction(ae -> removeAllModal.setVisible(false));
    }

    private void createGridRow(final GridPane gridPane, final String statKey, final int rowNum, final String displayName) {
        final Label name = new Label(displayName);
        name.getStyleClass().addAll("bold");
        name.setAlignment(Pos.TOP_RIGHT);

        final Label value = new Label("...");
        statLabel.put(statKey, value);

        gridPane.add(name, 1, rowNum);
        gridPane.add(value, 2, rowNum);
    }

    /**
     * Reloads displayed timestamp and group stats information. Information displayed is queried from the database
     * and formatted and processed before being added to the labels, lists, and charts.
     *
     * @throws SQLException the group stats operation failed
     */
    private void reloadStats() throws SQLException {
        logger.debug("Reload {}", group);
        runLater(() -> btnReload.setDisable(true));

        cleanUp();

        final String previousName = group.getName();
        final String previousId = group.getGroupId();
        final Group newGroup = database.groups().get(group.getGroupId());

        if (newGroup == null) {
            ManageGroups.getManagerCache().invalidate(previousId);
            return;
        } else if (!StringUtils.equals(previousName, newGroup.getName())) {
            group.setName(newGroup.getName());
            runLater(() -> groupTitle.setText(group.getName()));
        }

        updateLastRefreshed();

        final GroupStats groupStats = database.getGroupStats(this.group);

        final List<LineChart.Data<String, Number>> commentChartData = groupStats.getWeeklyCommentHistogram().entrySet().stream()
                .map(entry -> {
                    final LocalDateTime beginningOfWeek = DateUtils.epochMillisToDateTime(entry.getKey());
                    final String beginningOfWeekStr = formatter.format(beginningOfWeek);

                    final LocalDateTime endOfWeek = beginningOfWeek.plusDays(7);
                    final String endOfWeekStr = formatter.format(endOfWeek);

                    final LineChart.Data<String, Number> dataPoint =
                            new LineChart.Data<>(beginningOfWeekStr, entry.getValue());

                    final Tooltip tooltip = new Tooltip(String.format("%s - %s\r\n%,d new comment(s)",
                            beginningOfWeekStr, endOfWeekStr, entry.getValue()));

                    final StackPane node = new StackPane();

                    installDataTooltip(tooltip, node, dataPoint);

                    return dataPoint;
                })
                .collect(Collectors.toList());

        final List<LineChart.Data<String, Number>> videoChartData = groupStats.getWeeklyUploadHistogram().entrySet().stream()
                .map(entry -> {
                    final LocalDateTime beginningOfWeek = DateUtils.epochMillisToDateTime(entry.getKey());
                    final String beginningOfWeekStr = formatter.format(beginningOfWeek);

                    final LocalDateTime endOfWeek = beginningOfWeek.plusDays(7);
                    final String endOfWeekStr = formatter.format(endOfWeek);

                    final LineChart.Data<String, Number> dataPoint =
                            new LineChart.Data<>(beginningOfWeekStr, entry.getValue());

                    final Tooltip tooltip = new Tooltip(String.format("%s - %s\r\n%,d new video(s)",
                            beginningOfWeekStr, endOfWeekStr, entry.getValue()));

                    final StackPane node = new StackPane();

                    installDataTooltip(tooltip, node, dataPoint);

                    return dataPoint;
                })
                .collect(Collectors.toList());

        final List<MGMVStatItem> popularVideos = groupStats.getMostViewed().stream()
                .map(video -> new MGMVStatItem(video, video.getViewCount(), "views"))
                .collect(Collectors.toList());
        final List<MGMVStatItem> commentedVideos = groupStats.getMostCommented().stream()
                .map(video -> new MGMVStatItem(video, video.getComments(), "comments"))
                .collect(Collectors.toList());
        final List<MGMVStatItem> disabledVideos = groupStats.getCommentsDisabled().stream()
                .map(video -> new MGMVStatItem(video, 0L, "Comments Disabled", true))
                .collect(Collectors.toList());

        final List<MGMVStatItem> mostLikedViewers = groupStats.getMostLikedViewers().entrySet().stream()
                .map(entry -> new MGMVStatItem(entry.getKey(), entry.getValue(), "likes"))
                .collect(Collectors.toList());
        final List<MGMVStatItem> mostActiveViewers = groupStats.getMostActiveViewers().entrySet().stream()
                .map(entry -> new MGMVStatItem(entry.getKey(), entry.getValue(), "comments"))
                .collect(Collectors.toList());

        final long comments = groupStats.getTotalComments();

        runLater(() -> {
            commentsLineChartData.getData().addAll(commentChartData);
            statLabel.get(STAT_TOTAL_COMMENTS).setText(String.format("%,d", comments));
            statLabel.get(STAT_TOTAL_LIKES).setText(String.format("+%,d", groupStats.getTotalCommentLikes()));
            statLabel.get(STAT_UNIQUE_VIEWERS).setText(String.format("%,d", groupStats.getUniqueViewers()));

            videosLineChartData.getData().addAll(videoChartData);
            statLabel.get(STAT_TOTAL_VIDEOS).setText(String.format("%,d", groupStats.getTotalVideos()));
            statLabel.get(STAT_TOTAL_VIEWS).setText(String.format("%,d", groupStats.getTotalViews()));
            statLabel.get(STAT_VIDEO_LIKES).setText(String.format("+%,d", groupStats.getTotalLikes()));
            generalPane.setDisable(false);

            popularVideosList.getItems().addAll(popularVideos);
            commentedVideosList.getItems().addAll(commentedVideos);
            disabledVideosList.getItems().addAll(disabledVideos);
            videoPane.setDisable(false);

            popularViewersList.getItems().addAll(mostLikedViewers);
            activeViewersList.getItems().addAll(mostActiveViewers);
            viewerPane.setDisable(false);

            btnReload.setDisable(false);
        });
    }

    @Subscribe
    public void groupItemAddEvent(final GroupItemAddEvent groupItemAddEvent) {
        if (this.group.equals(groupItemAddEvent.getGroup())) {
            logger.debug("Group Item Add Event");
            reloadGroupItems("groupItemAddEvent");
        }
    }

    @Subscribe
    public void groupItemDeleteEvent(final GroupItemDeleteEvent groupItemDeleteEvent) {
        if (this.group.equals(groupItemDeleteEvent.getGroup())) {
            logger.debug("Group Item Delete Event");
            reloadGroupItems("groupItemDeleteEvent");
            try {
                reloadStats();
            } catch (SQLException e) {
                logger.error("Failed to reload stats", e);
            }
        }
    }

    /**
     * Starts a thread to reload the GroupItems in the ListView.
     */
    private void reloadGroupItems(final String caller) {
        logger.debug("[Load] Grabbing GroupItems {}", caller);
        final List<GroupItem> groupItems = database.groupItems().byGroup(this.group);
        logger.debug("[Load] Found " + groupItems.size() + " GroupItem(s)");

        runLater(() -> groupItemList.getItems().clear());

        final Map<Integer, List<GroupItem>> partitioned = partition(groupItems, 1000);
        for (int key : partitioned.keySet()) {
            final List<MGMVGroupItemView> groupItemViews = partitioned.get(key).stream()
                    .map(MGMVGroupItemView::new)
                    .collect(Collectors.toList());

            runLater(() -> groupItemList.getItems().addAll(groupItemViews));
        }
    }

    private void installDataTooltip(Tooltip tooltip, Node node, LineChart.Data<?, ?> dataPoint) {
        node.setStyle("-fx-background-color: transparent;");
        node.setOnMouseEntered(e -> node.setStyle("-fx-background-color: orangered;"));
        node.setOnMouseExited(e -> node.setStyle("-fx-background-color: transparent;"));

        Tooltip.install(node, tooltip);

        dataPoint.setNode(node);
    }

    private void updateLastRefreshed() {
        long timestamp = database.getLastChecked(this.group);
        final LocalDateTime dateTime = DateUtils.epochMillisToDateTime(timestamp);
        latestDiff = Duration.between(dateTime, LocalDateTime.now());
        final String formattedTimestamp = timestamp == 0 ? "never refreshed" : timeSince(latestDiff);

        runLater(() -> {
            refreshStatus.setText(formattedTimestamp);

            MGMVRefreshModal mgmvRefresh = refreshModal.getContent();
            mgmvRefresh.getBtnDelete().setOnAction(e -> btnDelete.fire());

            if (latestDiff.toDays() >= 30 && !groupItemList.getItems().isEmpty() && !refreshModal.isVisible()) {
                btnRefresh.fire();
            }
        });
    }

    private String timeSince(final Duration diff) {
        if (diff.minusSeconds(60).isNegative()) {
            return "just now";
        } else if (diff.minusMinutes(60).isNegative()) {
            return String.format("%s minute(s) ago", diff.toMinutes());
        } else if (diff.minusHours(24).isNegative()) {
            return String.format("%s hour(s) ago", diff.toHours());
        } else if (diff.minusDays(7).isNegative()) {
            return String.format("%s day(s) ago", diff.toDays());
        } else {
            return String.format("%s week(s) ago", diff.toDays() / 7);
        }
    }

    private <T> Map<Integer, List<T>> partition(final List<T> list, int pageSize) {
        return IntStream.iterate(0, i -> i + pageSize)
                .limit((list.size() + pageSize - 1) / pageSize)
                .boxed()
                .collect(toMap(i -> i / pageSize,
                        i -> list.subList(i, min(i + pageSize, list.size()))));
    }

    /**
     * Cleans up the statistics area, removing all data, disables the accordion panes.
     */
    @Override
    public void cleanUp() {
        runLater(() -> {
            commentsLineChartData.getData().clear();
            videosLineChartData.getData().clear();

            statLabel.values().forEach(label -> label.setText("..."));

            Stream.of(generalPane, videoPane, viewerPane)
                    .forEach(pane -> pane.setDisable(true));
            Stream.of(popularVideosList, commentedVideosList,
                            disabledVideosList, popularViewersList, activeViewersList)
                    .forEach(list -> list.getItems().clear());
        });
    }

    public Duration getLatestDiff() {
        return latestDiff;
    }

    public ListView<MGMVGroupItemView> getGroupItemList() {
        return groupItemList;
    }
}
