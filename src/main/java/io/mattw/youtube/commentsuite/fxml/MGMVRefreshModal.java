package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.ConfigFile;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.db.Group;
import io.mattw.youtube.commentsuite.refresh.*;
import io.mattw.youtube.commentsuite.util.ClipboardUtil;
import io.mattw.youtube.commentsuite.util.Threads;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.mattw.youtube.commentsuite.refresh.RefreshStyle.CUSTOM;
import static io.mattw.youtube.commentsuite.refresh.RefreshStyle.values;
import static javafx.application.Platform.runLater;

/**
 * This modal allows the user to start a group refresh. The group refresh will use the YouTube API to download
 * videos under the GroupItems of the Group in the ManageGroupsManager.
 *
 * @see RefreshInterface
 * @see GroupRefresh
 * @see ManageGroupsManager
 */
public class MGMVRefreshModal extends HBox {

    private static final Logger logger = LogManager.getLogger();

    public static final int WIDTH = 550;
    private static final String STAT_ELAPSED = "STAT_ELAPSED";
    private static final String STAT_VIDEO = "STAT_NEW_VIDEO";
    private static final String STAT_COMMENT = "STAT_NEW_COMMENT";
    private static final String STAT_MODERATED = "STAT_MODERATED";
    private static final String STAT_VIEWER = "STAT_NEW_VIEWER";

    // Enables debug stats during refresh for in-queue amounts
    public static final boolean DEBUG_MODE = false;

    @FXML private Label alert, apiTermsAlert;
    @FXML private Label statusStep;
    @FXML private Button btnClose;
    @FXML private Button btnStart;
    @FXML private Button btnDelete;
    @FXML private ProgressBar progressBar;
    @FXML private VBox statusPane;

    @FXML private VBox optionsPane;
    @FXML private ComboBox<RefreshStyle> refreshStyle;
    @FXML private ComboBox<RefreshTimeframe> refreshTimeframe;
    @FXML private ComboBox<RefreshCommentPages> refreshCommentPages;
    @FXML private ComboBox<RefreshCommentOrder> refreshCommentOrder;
    @FXML private ComboBox<RefreshCommentPages> refreshReplyPages;
    @FXML private CheckBox smartCommentPages;
    @FXML private CheckBox updateIgnore;
    @FXML private Spinner<Integer> maxRetryAttempts;

    @FXML private HBox warningsPane;
    @FXML private Label warnings;
    @FXML private GridPane refreshStatsPane;
    @FXML private GridPane debugStatsPane;
    @FXML private ImageView expandIcon;
    @FXML private ListView<String> errorList;
    @FXML private Hyperlink expand;
    @FXML private ImageView endStatus;
    @FXML private ProgressIndicator statusIndicator;

    private final ManageGroupsManager manageGroupsManager;
    private final Group group;
    private RefreshInterface refreshThread;
    private boolean running = false;
    private boolean hasBeenStarted = false;
    private boolean expanded = false;

    private final Map<String, Label> statNewValue = new HashMap<>();
    private final Map<String, Label> statTotalValue = new HashMap<>();

    private final ClipboardUtil clipboard = new ClipboardUtil();
    private final ConfigFile<ConfigData> configFile = CommentSuite.getConfig();
    private final ConfigData configData = configFile.getDataObject();

    public MGMVRefreshModal(final ManageGroupsManager manageGroupsManager, final Group group) {
        logger.debug("Initialize for Group [id={},name={}]", group.getGroupId(), group.getName());

        this.manageGroupsManager = manageGroupsManager;
        this.group = group;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVRefreshModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            debugStatsPane.setManaged(DEBUG_MODE);
            debugStatsPane.setVisible(DEBUG_MODE);

            expandIcon.setImage(ImageLoader.ANGLE_RIGHT.getImage());

            errorList.prefHeightProperty().bind(statusPane.heightProperty());
            errorList.maxHeightProperty().bind(statusPane.heightProperty());

            refreshStyle.setItems(FXCollections.observableArrayList(values()));
            refreshTimeframe.setItems(FXCollections.observableArrayList(RefreshTimeframe.values()));
            refreshCommentPages.setItems(FXCollections.observableArrayList(RefreshCommentPages.values()));
            refreshCommentOrder.setItems(FXCollections.observableArrayList(RefreshCommentOrder.values()));
            refreshReplyPages.setItems(FXCollections.observableArrayList(RefreshCommentPages.values()));
            maxRetryAttempts.focusedProperty().addListener((s, ov, nv) -> {
                if (nv) return;
                commitEditorText(maxRetryAttempts);
            });

            refreshStyle.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                logger.debug("Style {}", nv);
                if (nv != CUSTOM) {
                    runLater(() -> {
                        refreshTimeframe.setValue(nv.getTimeframe());
                        refreshCommentPages.setValue(nv.getCommentPages());
                        refreshCommentOrder.setValue(nv.getCommentOrder());
                        refreshReplyPages.setValue(nv.getReplyPages());
                    });
                }
            });

            RefreshOptions refreshOptions = configData.getRefreshOptions();
            smartCommentPages.setSelected(refreshOptions.isCommentPagesSmart());
            updateIgnore.setSelected(refreshOptions.isUpdateCommentsChannels());
            logger.debug(refreshOptions.getMaxRetryAttempts());
            maxRetryAttempts.getValueFactory().setValue(refreshOptions.getMaxRetryAttempts());
            if (refreshOptions.getStyle() == CUSTOM) {
                refreshStyle.setValue(CUSTOM);
                refreshTimeframe.setValue(refreshOptions.getTimeframe());
                refreshCommentPages.setValue(refreshOptions.getCommentPages());
                refreshCommentOrder.setValue(refreshOptions.getCommentOrder());
                refreshReplyPages.setValue(refreshOptions.getReplyPages());
            } else {
                refreshStyle.setValue(refreshOptions.getStyle());
            }

            refreshTimeframe.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> checkCustomStyle());
            refreshCommentPages.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> checkCustomStyle());
            refreshCommentOrder.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> checkCustomStyle());
            refreshReplyPages.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> checkCustomStyle());

            expand.setOnAction(ae -> {
                expanded = !expanded;
                runLater(() -> {
                    if (expanded) {
                        expandIcon.setImage(ImageLoader.ANGLE_LEFT.getImage());
                        errorList.setManaged(true);
                        errorList.setVisible(true);
                    } else {
                        expandIcon.setImage(ImageLoader.ANGLE_RIGHT.getImage());
                        errorList.setManaged(false);
                        errorList.setVisible(false);
                    }
                });
            });

            errorList.setOnKeyPressed(ke -> {
                if (ke.getCode() == KeyCode.C && ke.isControlDown()) {
                    clipboard.setClipboard(errorList.getItems());
                }
            });

            btnStart.setOnAction(ae -> new Thread(() -> {
                if (running) {
                    logger.debug("Requesting group refresh stopped for group [id={},name={}]", group.getGroupId(), group.getName());
                    runLater(() -> {
                        btnStart.setDisable(true);
                        endStatus.setImage(ImageLoader.MINUS_CIRCLE.getImage());
                    });
                    refreshThread.hardShutdown();
                    while (refreshThread.isAlive()) {
                        Threads.awaitMillis(97);
                    }
                    running = false;
                    runLater(() -> btnClose.setDisable(false));
                } else {
                    running = true;
                    logger.debug("Starting group refresh for group [id={},name={}]", group.getGroupId(), group.getName());
                    runLater(() -> {
                        statusPane.setVisible(true);
                        statusPane.setManaged(true);
                        btnClose.setVisible(true);
                        btnClose.setManaged(true);
                        btnClose.setDisable(true);
                        btnStart.setText("Stop");
                        alert.setVisible(false);
                        alert.setManaged(false);
                        optionsPane.setVisible(false);
                        optionsPane.setManaged(false);
                    });

                    RefreshOptions options = new RefreshOptions();
                    options.setStyle(refreshStyle.getValue());
                    options.setTimeframe(refreshTimeframe.getValue());
                    options.setCommentPages(refreshCommentPages.getValue());
                    options.setCommentPagesSmart(smartCommentPages.isSelected());
                    options.setReplyPages(refreshReplyPages.getValue());
                    options.setUpdateCommentsChannels(updateIgnore.isSelected());
                    options.setMaxRetryAttempts(maxRetryAttempts.getValue());

                    configData.setRefreshOptions(options);
                    configFile.save();

                    refreshThread = new GroupRefresh(group, options);

                    runLater(() -> {
                        int rowIndex = 1;
                        createGridRow(refreshStatsPane, STAT_ELAPSED, rowIndex++, "Elapsed time");
                        createGridRowNewTotal(refreshStatsPane, STAT_VIDEO, rowIndex++,"New videos");
                        createGridRowNewTotal(refreshStatsPane, STAT_COMMENT, rowIndex++,"New comments");
                        createGridRowNewTotal(refreshStatsPane, STAT_VIEWER, rowIndex++,"New viewers");

                        rowIndex = 1;
                        for (Map.Entry<String,ConsumerMultiProducer<?>> consumer : refreshThread.getConsumerProducers().entrySet()) {
                            createGridRowNewTotal(debugStatsPane, consumer.getKey(), rowIndex++, consumer.getKey());
                        }

                        refreshThread.getObservableErrorList().addListener((ListChangeListener<String>) (lcl) -> runLater(() -> {
                            int items = lcl.getList().size();
                            warningsPane.setManaged(items > 0);
                            warningsPane.setVisible(items > 0);
                            warnings.setText(items + " message(s)");
                        }));

                        errorList.setItems(refreshThread.getObservableErrorList());
                        statNewValue.get(STAT_ELAPSED).textProperty().bind(refreshThread.elapsedTimeProperty());
                        progressBar.progressProperty().bind(refreshThread.progressProperty());
                        statusStep.textProperty().bind(refreshThread.statusStepProperty());

                        statNewValue.get(STAT_VIDEO).textProperty().bind(Bindings.format("%,d", refreshThread.newVideosProperty()));
                        statTotalValue.get(STAT_VIDEO).textProperty().bind(
                                Bindings.concat("of ")
                                        .concat(Bindings.format("%,d", refreshThread.totalVideosProperty()))
                                        .concat(" total"));

                        statNewValue.get(STAT_COMMENT).textProperty().bind(Bindings.format("%,d", refreshThread.newCommentsProperty()));
                        statTotalValue.get(STAT_COMMENT).textProperty().bind(
                                Bindings.concat("of ")
                                        .concat(Bindings.format("%,d", refreshThread.totalCommentsProperty()))
                                        .concat(" total"));

                        statNewValue.get(STAT_VIEWER).textProperty().bind(Bindings.format("%,d", refreshThread.newViewersProperty()));
                        statTotalValue.get(STAT_VIEWER).textProperty().bind(
                                Bindings.concat("of ")
                                        .concat(Bindings.format("%,d", refreshThread.totalViewersProperty()))
                                        .concat(" total"));
                    });
                    refreshThread.start();
                    hasBeenStarted = true;
                    refreshThread.endedProperty().addListener((o, ov, nv) -> {
                        progressBar.progressProperty().unbind();
                        runLater(() -> {
                            btnStart.setVisible(false);
                            btnStart.setManaged(false);
                            btnClose.setDisable(false);
                            endStatus.setImage(refreshThread.isEndedOnError() ?
                                    ImageLoader.TIMES_CIRCLE.getImage() : ImageLoader.CHECK_CIRCLE.getImage());
                            endStatus.setManaged(true);
                            endStatus.setVisible(true);
                            statusIndicator.setManaged(false);
                            statusIndicator.setVisible(false);
                        });
                    });

                    final ExecutorService es = Executors.newSingleThreadExecutor();
                    es.submit(() -> {
                        logger.debug("Start Debug Progress Thread");

                        while (refreshThread.isAlive()) {
                            for (Map.Entry<String,ConsumerMultiProducer<?>> consumer : refreshThread.getConsumerProducers().entrySet()) {
                                runLater(() -> {
                                    Label newLabel = statNewValue.get(consumer.getKey());
                                    if (newLabel != null) {
                                        newLabel.setText(consumer.getValue().getBlockingQueue().size() + " iq.");
                                    }
                                    Label totalLabel = statTotalValue.get(consumer.getKey());
                                    if (totalLabel != null) {
                                        totalLabel.setText("of " + consumer.getValue().getTotalAccepted().toString() + " a.");
                                    }
                                });
                            }

                            Threads.awaitMillis(100);
                        }
                        logger.debug("End Debug Progress Thread");
                    });
                    es.shutdown();
                }
            }).start());
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void createGridRow(final GridPane gridPane, final String statKey, final int rowNum, final String displayName) {
        final Label name = new Label(displayName);
        name.getStyleClass().addAll("bold", "font14");
        name.setAlignment(Pos.TOP_RIGHT);

        final Label newValue = new Label("0");
        newValue.setMinWidth(0);
        newValue.setPrefWidth(0);
        newValue.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(newValue, Priority.ALWAYS);
        statNewValue.put(statKey, newValue);

        gridPane.addRow(rowNum, name, newValue);
    }

    private void createGridRowNewTotal(final GridPane gridPane, final String statKey, final int rowNum, final String displayName) {
        final Label name = new Label(displayName);
        name.getStyleClass().addAll("bold", "font14");
        name.setAlignment(Pos.TOP_RIGHT);

        final Label newValue = new Label("0");
        newValue.setMinWidth(0);
        newValue.setPrefWidth(0);
        newValue.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(newValue, Priority.ALWAYS);

        final Label totalValue = new Label("0 total");
        totalValue.getStyleClass().addAll("textMutedLight", "font14");
        totalValue.setAlignment(Pos.TOP_RIGHT);

        statNewValue.put(statKey, newValue);
        statTotalValue.put(statKey, totalValue);

        gridPane.addRow(rowNum, name, newValue, totalValue);
    }

    /**
     * Reset the modal back to its original state when being opened.
     */
    public void reset() {
        logger.debug("Resetting state of Refresh Modal");
        running = false;
        hasBeenStarted = false;
        statNewValue.clear();
        statTotalValue.clear();
        runLater(() -> {
            if (expanded) {
                expand.fire();
            }

            if (manageGroupsManager.getLatestDiff().toDays() >= 30 && !manageGroupsManager.getGroupItemList().getItems().isEmpty()) {
                btnClose.setDisable(true);
                btnClose.setVisible(false);
                btnClose.setManaged(false);

                btnDelete.setVisible(true);
                btnDelete.setManaged(true);

                apiTermsAlert.setVisible(true);
                apiTermsAlert.setManaged(true);
            } else {
                btnDelete.setVisible(false);
                btnDelete.setManaged(false);

                apiTermsAlert.setVisible(false);
                apiTermsAlert.setManaged(false);
            }

            btnClose.setVisible(true);
            btnClose.setDisable(false);
            refreshStatsPane.getChildren().clear();
            debugStatsPane.getChildren().clear();
            endStatus.setManaged(false);
            endStatus.setVisible(false);
            statusIndicator.setManaged(true);
            statusIndicator.setVisible(true);
            errorList.getItems().clear();
            alert.getStyleClass().remove("alertSuccess");
            alert.getStyleClass().add("alertWarning");
            alert.setVisible(false);
            alert.setManaged(false);
            optionsPane.setVisible(true);
            optionsPane.setManaged(true);
            statusPane.setVisible(false);
            statusPane.setManaged(false);
            btnStart.setVisible(true);
            btnStart.setManaged(true);
            btnStart.setDisable(false);
            btnStart.setText("Start");
            btnStart.getStyleClass().remove("btnWarning");
            btnStart.getStyleClass().add("btnPrimary");
        });
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnStart() {
        return btnStart;
    }

    public Button getBtnDelete() {
        return btnDelete;
    }

    public ListView<String> getErrorList() {
        return errorList;
    }

    public boolean isHasBeenStarted() {
        return hasBeenStarted;
    }

    public Label getApiTermsAlert() {
        return apiTermsAlert;
    }

    private void checkCustomStyle() {
        runLater(() -> {
            if (refreshStyle.getValue() != CUSTOM &&
                    !refreshStyle.getValue().matches(refreshTimeframe.getValue(), refreshCommentPages.getValue(),refreshCommentOrder.getValue(), refreshReplyPages.getValue())) {
                refreshStyle.setValue(CUSTOM);
            }
        });
    }

    private <T> void commitEditorText(Spinner<T> spinner) {
        if (!spinner.isEditable()) return;
        String text = spinner.getEditor().getText();
        SpinnerValueFactory<T> valueFactory = spinner.getValueFactory();
        if (valueFactory != null) {
            StringConverter<T> converter = valueFactory.getConverter();
            if (converter != null) {
                T value = converter.fromString(text);
                valueFactory.setValue(value);
            }
        }
    }

}
