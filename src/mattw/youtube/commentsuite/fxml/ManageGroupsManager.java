package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import mattw.youtube.commentsuite.Cleanable;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.db.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages a specific group; refreshing, stats, renaming, deletion, adding group items, etc.
 *
 * Loads template FXML and displays info from database.
 *
 * @since 2019-01-06
 * @author mattwright324
 */
public class ManageGroupsManager extends StackPane implements ImageCache, Cleanable {

    private Logger logger = LogManager.getLogger(this.toString());
    private Image edit = ImageLoader.PENCIL.getImage();
    private Image close = ImageLoader.CLOSE.getImage();

    private ChangeListener<Font> fontListener;
    private Group group;

    private CommentDatabase database;

    private @FXML OverlayModal<MGMVRefreshModal> refreshModal;
    private @FXML OverlayModal<MGMVDeleteGroupModal> deleteModal;
    private @FXML OverlayModal<MGMVAddItemModal> addItemModal;
    private @FXML OverlayModal<MGMVRemoveSelectedModal> removeItemModal;
    private @FXML OverlayModal<MGMVRemoveAllModal> removeAllModal;
    private @FXML Button btnAddItem;
    private @FXML Button btnRemoveItems;
    private @FXML Button btnRemoveAll;
    private @FXML ListView<MGMVGroupItemView> groupItemList;

    private @FXML TextField groupTitle;
    private @FXML ImageView editIcon;
    private @FXML Hyperlink rename;
    private @FXML Button btnRefresh;
    private @FXML Button btnReload;
    private @FXML Button btnDelete;
    private @FXML Label refreshStatus;

    private @FXML LineChart<String,Number> commentsLineChart, videosLineChart;
    private LineChart.Series<String,Number> commentsLineChartData, videosLineChartData;
    private @FXML Label totalComments, totalLikes, totalVideos, totalViews, totalVideoLikes, totalVideoDislikes,
            likeDislikeRatio, normalizedRatio;
    private @FXML ListView<MGMVYouTubeObjectItem> popularVideosList, dislikedVideosList, commentedVideosList,
            disabledVideosList, popularViewersList, activeViewersList;

    private @FXML Accordion accordion;
    private @FXML TitledPane generalPane, videoPane, viewerPane;

    public ManageGroupsManager(Group group) throws IOException {
        logger.debug(String.format("Initialize for Group [id=%s,name=%s]", group.getId(), group.getName()));

        database = FXMLSuite.getDatabase();

        this.group = group;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("ManageGroupsManager.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        Random random = new Random();
        this.setStyle(String.format("-fx-background-color: linear-gradient(to top, rgba(%s,%s,%s,%s), transparent);",
                220-random.nextInt(60), 220-random.nextInt(60), 220-random.nextInt(60), 0.4));

        accordion.setExpandedPane(generalPane);

        commentsLineChartData = new LineChart.Series<>();
        commentsLineChart.getData().add(commentsLineChartData);

        videosLineChartData = new LineChart.Series<>();
        videosLineChart.getData().add(videosLineChartData);

        editIcon.setImage(edit);

        groupTitle.setMinWidth(Region.USE_PREF_SIZE);
        groupTitle.setMaxWidth(Region.USE_PREF_SIZE);
        groupTitle.textProperty().addListener((ov, prevText, currText) -> resizeTextField(groupTitle));
        groupTitle.fontProperty().addListener(fontListener = (o, ov, nv) -> {
            resizeTextField(groupTitle);
            // One-time font listener resize.
            // Will match content after font set on label from styleClass.
            // If not removed, when clicking the 'Rename' button, the label will
            // flicker once between Font size 15 (default) and back to the styleClass font size
            // every time the edit button is clicked.
            groupTitle.fontProperty().removeListener(fontListener);
        });
        groupTitle.setText(group.getName());
        groupTitle.setOnKeyPressed(ke -> {
            if(ke.getCode() == KeyCode.ENTER) {
                rename.fire();
            }
        });

        rename.setOnAction(ae -> new Thread(() -> {
            if(editIcon.getImage().equals(close)) {
                try {
                    database.renameGroup(group, groupTitle.getText());
                } catch (SQLException e){
                    logger.error(e);
                }
            }
            runLater(() -> {
                if(editIcon.getImage().equals(edit)) {
                    editIcon.setImage(close);
                    groupTitle.getStyleClass().remove("clearTextField");
                    groupTitle.setEditable(true);
                    rename.setTooltip(new Tooltip("Save Changes"));
                } else {
                    editIcon.setImage(edit);
                    groupTitle.getStyleClass().add("clearTextField");
                    groupTitle.setEditable(false);
                    rename.setTooltip(new Tooltip("Rename"));
                }
            });
        }).start());

        SelectionModel selectionModel = groupItemList.getSelectionModel();
        ((MultipleSelectionModel) selectionModel).setSelectionMode(SelectionMode.MULTIPLE);
        ((MultipleSelectionModel) selectionModel).getSelectedItems().addListener((ListChangeListener)(lcl) -> {
            runLater(() -> {
                int items = lcl.getList().size();
                btnRemoveItems.setText(String.format("Remove (%s)", items));
                btnRemoveItems.setDisable(items <= 0);
            });
        });
        groupItemList.getItems().addListener((ListChangeListener<MGMVGroupItemView>)(lcl) -> {
           runLater(() -> {
               int items = lcl.getList().size();
               btnRemoveAll.setText(String.format("Remove All (%s)", items));
               btnRemoveAll.setDisable(items <= 0);
           });
        });

        btnReload.setOnAction(ae -> new Thread(() -> {
            reloadGroupItems();
            try {
                reload();
            } catch (SQLException e) {
                logger.error("An error occured during group reload", e);
            }
        }).start());

        btnReload.fire();

        /**
         * Refresh Modal
         */
        MGMVRefreshModal mgmvRefresh = new MGMVRefreshModal(group);
        refreshModal.setContent(mgmvRefresh);
        btnRefresh.setOnAction(ae -> runLater(() -> {
            mgmvRefresh.reset();
            refreshModal.setVisible(true);
        }));
        mgmvRefresh.getBtnClose().setOnAction(ae -> {
            refreshModal.setVisible(false);
            updateLastRefreshed();
        });
        mgmvRefresh.getErrorList().managedProperty().addListener((o, ov, nv) -> {
            if(nv) {
                runLater(() -> refreshModal.getModalContainer().setMaxWidth(420+250));
            } else {
                runLater(() -> refreshModal.getModalContainer().setMaxWidth(420));
            }
        });

        /**
         * Delete Group Modal
         */
        MGMVDeleteGroupModal mgmvDelete = new MGMVDeleteGroupModal(group);
        deleteModal.setContent(mgmvDelete);
        deleteModal.setDividerClass("dividerDanger");
        btnDelete.setOnAction(ae -> runLater(() -> deleteModal.setVisible(true)));
        mgmvDelete.getBtnClose().setOnAction(ae -> deleteModal.setVisible(false));

        /**
         * Add Item Modal
         */
        MGMVAddItemModal mgmvAddItem = new MGMVAddItemModal(group);
        addItemModal.setContent(mgmvAddItem);
        btnAddItem.setOnAction(ae -> runLater(() -> {
            mgmvAddItem.cleanUp();
            addItemModal.setVisible(true);
        }));
        mgmvAddItem.getBtnClose().setOnAction(ae -> addItemModal.setVisible(false));
        mgmvAddItem.itemAddedProperty().addListener((o, ov, nv) -> reloadGroupItems());

        /**
         * Remove Selected GroupItems Modal
         */
        MGMVRemoveSelectedModal mgmvRemoveSelected = new MGMVRemoveSelectedModal(group, groupItemList.getSelectionModel());
        removeItemModal.setContent(mgmvRemoveSelected);
        removeItemModal.setDividerClass("dividerWarning");
        btnRemoveItems.setOnAction(ae -> runLater(() -> {
            mgmvRemoveSelected.cleanUp();
            removeItemModal.setVisible(true);
        }));
        mgmvRemoveSelected.getBtnClose().setOnAction(ae -> removeItemModal.setVisible(false));
        mgmvRemoveSelected.itemsRemovedProperty().addListener((o, ov, nv) -> reloadGroupItems());

        /**
         * Remove All GroupItems Modal
         */
        MGMVRemoveAllModal mgmvRemoveAll = new MGMVRemoveAllModal(group, groupItemList.getItems());
        removeAllModal.setContent(mgmvRemoveAll);
        removeAllModal.setDividerClass("dividerDanger");
        btnRemoveAll.setOnAction(ae -> runLater(() -> {
            mgmvRemoveAll.cleanUp();
            removeAllModal.setVisible(true);
        }));
        mgmvRemoveAll.getBtnClose().setOnAction(ae -> removeAllModal.setVisible(false));
        mgmvRemoveAll.itemsRemovedProperty().addListener((o, ov, nv) -> reloadGroupItems());
    }

    /**
     * Reloads displayed timestamp and group stats information. Information displayed is queried from the database
     * and formatted and processed before being added to the labels, lists, and charts.
     *
     * @throws SQLException the group stats operation failed
     */
    private void reload() throws SQLException {
        cleanUp();
        updateLastRefreshed();

        GroupStats groupStats = database.getGroupStats(this.group);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yy");

        List<LineChart.Data<String,Number>> commentChartData = groupStats.getWeeklyCommentHistogram().entrySet().stream()
                .map(entry -> {
                    Date date = new Date(entry.getKey());
                    Date endOfWeek = new Date(entry.getKey()+604800000);

                    LineChart.Data<String,Number> dataPoint = new LineChart.Data<>(sdf.format(date), entry.getValue());

                    Tooltip tooltip = new Tooltip(String.format("%s - %s\r\n%,d new comments",
                            sdf.format(date), sdf.format(endOfWeek),
                            entry.getValue()));

                    StackPane node = new StackPane();

                    installDataTooltip(tooltip, node, dataPoint);

                    return dataPoint;
                })
                .collect(Collectors.toList());

        List<LineChart.Data<String,Number>> videoChartData = groupStats.getWeeklyUploadHistogram().entrySet().stream()
                .map(entry -> {
                    Date date = new Date(entry.getKey());
                    Date endOfWeek = new Date(entry.getKey()+604800000);

                    LineChart.Data<String,Number> dataPoint = new LineChart.Data<>(sdf.format(date), entry.getValue());

                    Tooltip tooltip = new Tooltip(String.format("%s - %s\r\n%,d new videos",
                            sdf.format(date), sdf.format(endOfWeek),
                            entry.getValue()));

                    StackPane node = new StackPane();

                    installDataTooltip(tooltip, node, dataPoint);

                    return dataPoint;
                })
                .collect(Collectors.toList());

        long gcd = gcd(groupStats.getTotalLikes(), groupStats.getTotalDislikes());
        long gcdLikes = groupStats.getTotalLikes() / gcd;
        long gcdDislikes = groupStats.getTotalDislikes() / gcd;

        long nLikes, nDislikes;
        if(gcdLikes > gcdDislikes) {
            nLikes = gcdLikes / gcdDislikes;
            nDislikes = 1;
        } else {
            nLikes = 1;
            nDislikes = gcdDislikes / gcdLikes;
        }

        List<MGMVYouTubeObjectItem> popularVideos = groupStats.getMostViewed().stream()
                .map(video -> new MGMVYouTubeObjectItem(video, video.getViews(), "views"))
                .collect(Collectors.toList());
        List<MGMVYouTubeObjectItem> dislikedVideos = groupStats.getMostDisliked().stream()
                .map(video -> new MGMVYouTubeObjectItem(video, video.getDislikes(), "dislikes"))
                .collect(Collectors.toList());
        List<MGMVYouTubeObjectItem> commentedVideos = groupStats.getMostCommented().stream()
                .map(video -> new MGMVYouTubeObjectItem(video, video.getCommentCount(), "comments"))
                .collect(Collectors.toList());
        List<MGMVYouTubeObjectItem> disabledVideos = groupStats.getCommentsDisabled().stream()
                .map(video -> new MGMVYouTubeObjectItem(video, 0L, "Comments Disabled", true))
                .collect(Collectors.toList());

        List<MGMVYouTubeObjectItem> mostLikedViewers = groupStats.getMostLikedViewers().entrySet().stream()
                .map(entry -> new MGMVYouTubeObjectItem(entry.getKey(), entry.getValue(), "likes"))
                .collect(Collectors.toList());
        List<MGMVYouTubeObjectItem> mostActiveViewers = groupStats.getMostActiveViewers().entrySet().stream()
                .map(entry -> new MGMVYouTubeObjectItem(entry.getKey(), entry.getValue(), "comments"))
                .collect(Collectors.toList());

        runLater(() -> {
            commentsLineChartData.getData().addAll(commentChartData);
            totalComments.setText(String.format("%,d", groupStats.getTotalComments()));
            totalLikes.setText(String.format("+%,d", groupStats.getTotalCommentLikes()));

            videosLineChartData.getData().addAll(videoChartData);
            totalVideos.setText(String.format("%,d", groupStats.getTotalVideos()));
            totalViews.setText(String.format("%,d", groupStats.getTotalViews()));
            totalVideoLikes.setText(String.format("+%,d", groupStats.getTotalLikes()));
            totalVideoDislikes.setText(String.format("-%,d", groupStats.getTotalDislikes()));
            likeDislikeRatio.setText(String.format("+%,d : -%,d", gcdLikes, gcdDislikes));
            likeDislikeRatio.setStyle(String.format("-fx-text-fill:%s", gcdLikes > gcdDislikes ? "cornflowerblue" : "orangered"));
            normalizedRatio.setText(String.format("+%,d : -%,d", nLikes, nDislikes));
            normalizedRatio.setStyle(String.format("-fx-text-fill:%s", gcdLikes > gcdDislikes ? "cornflowerblue" : "orangered"));
            generalPane.setDisable(false);

            popularVideosList.getItems().addAll(popularVideos);
            dislikedVideosList.getItems().addAll(dislikedVideos);
            commentedVideosList.getItems().addAll(commentedVideos);
            disabledVideosList.getItems().addAll(disabledVideos);
            videoPane.setDisable(false);

            popularViewersList.getItems().addAll(mostLikedViewers);
            activeViewersList.getItems().addAll(mostActiveViewers);
            viewerPane.setDisable(false);
        });
    }

    private void installDataTooltip(Tooltip tooltip, Node node, LineChart.Data<?, ?> dataPoint) {
        node.setStyle("-fx-background-color: transparent;");
        node.setOnMouseEntered(e -> node.setStyle("-fx-background-color: orangered;"));
        node.setOnMouseExited(e -> node.setStyle("-fx-background-color: transparent;"));

        Tooltip.install(node, tooltip);

        dataPoint.setNode(node);
    }

    private long gcd(long p, long q) {
        if (q == 0) {
            return p;
        } else {
            return gcd(q, p % q);
        }
    }

    private void updateLastRefreshed() {
        long timestamp = database.getLastChecked(this.group);
        runLater(() ->  refreshStatus.setText(timestamp == Long.MAX_VALUE ? "Never refreshed." : timeSince(timestamp)));
    }

    private String timeSince(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long temp = diff;
        long ms   = temp % 1000; temp /= 1000;
        long s    = temp % 60; temp /= 60;
        long m    = temp % 60; temp /= 60;
        long h    = temp % 24; temp /= 24;
        long d    = temp; temp /= 7;
        long w    = temp;

        if(diff < 1000 * 60) {
            return "just now";
        } else if(diff < 1000 * 60 * 60) {
            return String.format("%s minute(s) ago", m);
        } else if(diff < 1000 * 60 * 60 * 24) {
            return String.format("%s hour(s) ago", h);
        } else if(diff < 1000 * 60 * 60 * 24 * 7) {
            return String.format("%s day(s)ago", d);
        } else {
            return String.format("%s week(s) ago", w);
        }
    }

    /**
     * Starts a thread to reload the GroupItems in the ListView.
     */
    private void reloadGroupItems() {
        new Thread(() -> {
            logger.debug("[Load] Grabbing GroupItems");
            List<GroupItem> groupItems = database.getGroupItems(this.group);
            List<MGMVGroupItemView> groupItemViews = groupItems.stream()
                    .map(MGMVGroupItemView::new).collect(Collectors.toList());
            logger.debug("[Load] Found "+groupItems.size()+" GroupItem(s)");
            runLater(() -> {
                groupItemList.getItems().clear();
                groupItemList.getItems().addAll(groupItemViews);
            });
        }).start();
    }

    /**
     * Source: https://stackoverflow.com/a/25643696/2650847
     * Modifies a TextField's preferred width based on it's text content.
     * @param field
     */
    private void resizeTextField(TextField field) {
        runLater(() -> {
            Text text = new Text(field.getText());
            text.setFont(field.getFont());
            double width = text.getLayoutBounds().getWidth()
                    + field.getPadding().getLeft() + field.getPadding().getRight()
                    + 3d;
            field.setPrefWidth(width);
            field.positionCaret(field.getCaretPosition());
        });
    }

    /**
     * Cleans up the statistics area, removing all data, disables the accordion panes.
     */
    @Override
    public void cleanUp() {
        runLater(() -> {
            commentsLineChartData.getData().clear();
            videosLineChartData.getData().clear();
            Stream.of(totalComments, totalLikes, totalVideos, totalViews, totalVideoLikes, totalVideoDislikes,
                    likeDislikeRatio, normalizedRatio)
                    .forEach(label -> label.setText("..."));
            Stream.of(generalPane, videoPane, viewerPane)
                    .forEach(pane -> pane.setDisable(true));
            Stream.of(popularVideosList, dislikedVideosList, commentedVideosList,
                    disabledVideosList, popularViewersList, activeViewersList)
                    .forEach(list -> list.getItems().clear());
        });
    }
}
