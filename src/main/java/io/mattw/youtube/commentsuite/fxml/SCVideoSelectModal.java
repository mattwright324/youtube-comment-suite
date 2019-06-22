package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.Cleanable;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.db.GroupItem;
import io.mattw.youtube.commentsuite.db.YouTubeVideo;
import io.mattw.youtube.commentsuite.util.DateUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to select a specific video for comment searching that are within the currently
 * selected Group and GroupItem prior to opening the modal.
 *
 * @see SearchComments
 * @since 2018-12-30
 * @author mattwright324
 */
public class SCVideoSelectModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(SCVideoSelectModal.class.getSimpleName());

    private static final String ALL_VIDEOS = "All Videos";

    private CommentDatabase database;

    private @FXML Label lblSelection, errorMsg;
    private @FXML Button btnSearch;
    private @FXML TextField keywords;
    private @FXML ComboBox<String> orderBy;
    private @FXML ListView<MGMVYouTubeObjectItem> videoList;
    private @FXML ImageView btnReset;
    private @FXML Button btnClose, btnSubmit;

    private StringProperty valueProperty = new SimpleStringProperty();

    private Group group;
    private GroupItem groupItem;
    private YouTubeVideo selectedVideo;
    private Map<String,String> orderTypes = new LinkedHashMap<>();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public SCVideoSelectModal() {
        logger.debug("Initialize SCVideoSelectModal");

        database = FXMLSuite.getDatabase();

        orderTypes.put("By Date", "publish_date DESC");
        orderTypes.put("By Title", "video_title ASC");
        orderTypes.put("By Views", "total_views DESC");
        orderTypes.put("By Comments", "total_comments DESC");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCVideoSelectModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            btnClose.setVisible(false);
            btnClose.setManaged(false);

            btnReset.setImage(ImageLoader.CLOSE.getImage());
            btnReset.setDisable(true);
            btnReset.setOnMouseClicked(me -> {
                reset();
                updateSelectionLabel();
            });

            btnSearch.setOnAction(ae -> updateVideoList());

            orderBy.getItems().addAll(orderTypes.keySet());
            orderBy.getSelectionModel().select(0);

            setValueProperty(ALL_VIDEOS);

            videoList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if(nv != null) {
                    selectedVideo = (YouTubeVideo) nv.getObject();

                    runLater(() -> {
                        setValueProperty(selectedVideo == null ? ALL_VIDEOS : selectedVideo.getTitle());

                        updateSelectionLabel();
                    });
                }

            });
        } catch (IOException e) { logger.error("An error occurred loading the SCVideoSelectModal", e); }
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    YouTubeVideo getSelectedVideo() {
        return selectedVideo;
    }

    void loadWith(Group group, GroupItem groupItem) {
        if(this.group != group || this.groupItem != groupItem) {
            this.group = group;
            this.groupItem = groupItem;
            reset();
        }

        updateSelectionLabel();

        updateVideoList();
    }

    void updateSelectionLabel() {
        btnReset.setDisable(selectedVideo == null);
        lblSelection.setText(String.format("%s > %s > %s",
                group != null ? group.getName() : "$group",
                groupItem != null ? groupItem.getTitle() : "$groupItem",
                selectedVideo != null ? selectedVideo.getTitle() : ALL_VIDEOS));
    }

    void updateVideoList() {
        new Thread(() -> {
            runLater(() -> btnSearch.setDisable(true));
            try {
                String keywordText = keywords.getText();
                String order = orderTypes.get(orderBy.getValue());

                List<YouTubeVideo> videos;
                if(groupItem.getYoutubeId().equals(GroupItem.ALL_ITEMS)) {
                    videos = database.getVideos(group, keywordText, order, 25);
                } else {
                    videos = database.getVideos(groupItem, keywordText, order, 25);
                }

                runLater(() -> {
                    videoList.getItems().clear();
                    videoList.getItems().addAll(videos.stream()
                            .map(video -> {
                                String subtitle = String.format("Published %s • %,d views • %,d comments",
                                        formatter.format(DateUtils.epochMillisToDateTime(video.getPublishedDate())),
                                        video.getViews(),
                                        video.getCommentCount());

                                return new MGMVYouTubeObjectItem(video, subtitle);
                            })
                            .collect(Collectors.toList()));
                });
            } catch (SQLException e) {
                logger.error("Failed to load videos from database.", e);
            }
            runLater(() -> btnSearch.setDisable(false));
        }).start();
    }

    void setValueProperty(String value) {
        valueProperty.setValue(String.format("Selected: (%s)", value));
    }

    public StringProperty valueProperty() {
        return valueProperty;
    }

    public void reset() {
        this.selectedVideo = null;

        runLater(() -> {
            setValueProperty(ALL_VIDEOS);

            updateSelectionLabel();
        });
    }

    @Override
    public void cleanUp() {
        updateSelectionLabel();
    }
}
