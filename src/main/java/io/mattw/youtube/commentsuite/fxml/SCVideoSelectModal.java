package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.CommentSuite;
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
import javafx.scene.input.KeyCode;
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
 */
public class SCVideoSelectModal extends VBox implements Cleanable {

    private static final Logger logger = LogManager.getLogger();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final String ALL_VIDEOS = "All Videos";

    private CommentDatabase database;

    @FXML private Label lblSelection, errorMsg;
    @FXML private Button btnSearch;
    @FXML private TextField keywords;
    @FXML private ComboBox<String> orderBy;
    @FXML private ListView<MGMVStatItem> videoList;
    @FXML private ImageView btnReset;
    @FXML private Button btnClose, btnSubmit;

    private StringProperty valueProperty = new SimpleStringProperty();
    private final Map<String, String> orderTypes = new LinkedHashMap<>();

    private Group group;
    private GroupItem groupItem;
    private YouTubeVideo selectedVideo;

    public SCVideoSelectModal() {
        logger.debug("Initialize SCVideoSelectModal");

        database = CommentSuite.getDatabase();

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

            keywords.setOnKeyReleased(ke -> {
                if (ke.getCode() == KeyCode.ENTER) {
                    updateVideoList();
                }
            });

            orderBy.getItems().addAll(orderTypes.keySet());
            orderBy.getSelectionModel().select(0);

            setValueProperty(ALL_VIDEOS);

            videoList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (nv != null) {
                    selectedVideo = nv.getVideo();

                    runLater(() -> {
                        setValueProperty(selectedVideo == null ? ALL_VIDEOS : selectedVideo.getTitle());

                        updateSelectionLabel();
                    });
                }

            });
        } catch (IOException e) {
            logger.error("An error occurred loading the SCVideoSelectModal", e);
        }
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    public YouTubeVideo getSelectedVideo() {
        return selectedVideo;
    }

    public void loadWith(Group group, GroupItem groupItem) {
        if (this.group != group || this.groupItem != groupItem) {
            this.group = group;
            this.groupItem = groupItem;
            reset();
        }

        updateSelectionLabel();

        updateVideoList();
    }

    public void updateSelectionLabel() {
        btnReset.setDisable(selectedVideo == null);
        lblSelection.setText(String.format("%s > %s > %s",
                group != null ? group.getName() : "$group",
                groupItem != null ? groupItem.getDisplayName() : "$groupItem",
                selectedVideo != null ? selectedVideo.getTitle() : ALL_VIDEOS));
    }

    public void updateVideoList() {
        new Thread(() -> {
            runLater(() -> btnSearch.setDisable(true));
            try {
                String keywordText = keywords.getText();
                String order = orderTypes.get(orderBy.getValue());

                List<YouTubeVideo> videos;
                if (groupItem.getId().equals(GroupItem.ALL_ITEMS)) {
                    videos = database.videos().byGroupCriteria(group, keywordText, order, 50);
                } else {
                    videos = database.videos().byGroupItemCriteria(groupItem, keywordText, order, 50);
                }

                runLater(() -> {
                    videoList.getItems().clear();
                    videoList.getItems().addAll(videos.stream()
                            .map(video -> {
                                String subtitle = String.format("Published %s • %,d views • %,d comments",
                                        formatter.format(DateUtils.epochMillisToDateTime(video.getPublished())),
                                        video.getViewCount(),
                                        video.getComments());

                                return new MGMVStatItem(video, subtitle);
                            })
                            .collect(Collectors.toList()));
                });
            } catch (SQLException e) {
                logger.error("Failed to load videos from database.", e);
            }
            runLater(() -> btnSearch.setDisable(false));
        }).start();
    }

    public void setValueProperty(String value) {
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
