package io.mattw.youtube.commentsuite.fxml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ImageCache;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.CommentQuery;
import io.mattw.youtube.commentsuite.util.Threads;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import static javafx.application.Platform.runLater;

/**
 * This modal allows the full export of all comments in a query to the specified format
 * <p>
 * exports/
 * yyyy.MM.dd HH.mm.SS/
 * searchSettings.json
 * videoId1-meta.json
 * videoId1-comments.json
 * videoId2-meta.json
 * videoId2-comments.json
 * ...
 *
 * @see SearchComments
 */
public class SCExportModal extends VBox implements Cleanable, ImageCache {

    private static final Logger logger = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String prettyFlattenedExample = gson.toJson(JsonParser.parseString(
            "[{\"type\":\"comment\"},{\"type\":\"comment\"},{\"type\":\"reply\"},{\"type\":\"reply\"},{\"type\":\"comment\"}]"));
    private static final String prettyCondensedExample = gson.toJson(JsonParser.parseString(
            "[{\"type\":\"comment\"},{\"type\":\"comment\", replies:[{\"type\":\"reply\"},{\"type\":\"reply\"}]},{\"type\":\"comment\"}]"));

    @FXML
    private Label errorMsg;
    @FXML
    private VBox exportPane;
    @FXML
    private RadioButton radioCondensed, radioFlattened;
    @FXML
    private TextArea exportModeExample;
    @FXML
    private ProgressBar exportProgress;
    @FXML
    private ScrollPane helpScrollPane;
    @FXML
    private Button btnClose;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnSubmit;

    private CommentQuery commentQuery;
    private CommentDatabase database;

    private AtomicLong atomicVideoProgress = new AtomicLong(0);
    private long totalVideos = 0;

    private SCExportProducer exportProducer;

    public SCExportModal() {
        logger.debug("Initialize SCExportModal");

        database = CommentSuite.getDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCExportModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            cleanUp();

            helpScrollPane.prefViewportHeightProperty().bind(exportPane.heightProperty());

            radioFlattened.setOnAction(ae -> runLater(() -> exportModeExample.setText(prettyFlattenedExample)));
            radioCondensed.setOnAction(ae -> runLater(() -> exportModeExample.setText(prettyCondensedExample)));

            radioFlattened.fire();

            btnStop.setOnAction(ae -> {
                exportProducer.setHardShutdown(true);

                runLater(() -> btnStop.setDisable(true));
            });

            btnSubmit.setOnAction((ae) -> new Thread(this::startExport).start());
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    private void startExport() {
        runLater(() -> {
            btnSubmit.setVisible(false);
            btnSubmit.setManaged(false);

            exportProgress.setVisible(true);
            exportProgress.setManaged(true);

            radioFlattened.setDisable(true);
            radioCondensed.setDisable(true);

            btnStop.setVisible(true);
            btnStop.setManaged(true);

            btnClose.setDisable(true);
        });

        // Duplicate a new query object because it isn't threadsafe.
        // ExecutorGroup should make export faster
        final CommentQuery localQuery = database.commentQuery()
                .setGroup(commentQuery.getGroup())
                .setGroupItem(commentQuery.getGroupItem())
                .setCommentsType(commentQuery.getCommentsType())
                .setVideos(commentQuery.getVideos())
                .setNameLike(commentQuery.getNameLike())
                .setOrder(commentQuery.getOrder())
                .setTextLike(commentQuery.getTextLike())
                .setDateFrom(commentQuery.getDateFrom())
                .setDateTo(commentQuery.getDateTo());

        try {
            exportProducer = new SCExportProducer(localQuery, radioCondensed.isSelected());
            createSearchSettingsFile(exportProducer.getThisExportFolder(), localQuery);

            exportProducer.setMessageFunc(this::onMessage);
            exportProducer.accept(localQuery.getUniqueVideoIds());
            exportProducer.startProducing();

            new Thread(() -> {
                while (exportProducer.shouldKeepAlive()) {
                    final double progress = exportProducer.getTotalProcessed().get() / (exportProducer.getTotalAccepted().get() * 1d);
                    runLater(() -> exportProgress.setProgress(progress));

                    Threads.awaitMillis(100);
                }

                if (!exportProducer.isHardShutdown()) {
                    btnClose.fire();
                }
            }).start();

            exportProducer.getExecutorGroup().await();
            exportProducer.onCompletion();
        } catch (SQLException | InterruptedException e) {
            logger.error(e);
        } finally {
            runLater(() -> {
                btnStop.setVisible(false);
                btnStop.setManaged(false);

                btnClose.setDisable(false);
            });
        }
    }

    private void createSearchSettingsFile(final File exportFolder, final CommentQuery query) {
        final File searchSettings = new File(exportFolder, "searchSettings.json");
        try (FileOutputStream fos = new FileOutputStream(searchSettings);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(writer)) {

            logger.debug("Writing file {}", searchSettings.getName());

            query.prepForExport();

            gson.toJson(query, bw);

            bw.flush();
        } catch (Exception e) {
            logger.error("Failed to write json file(s)", e);

            runLater(() -> setError("Failed to export."));
        }
    }

    private void onMessage(Level level, Throwable throwable, String message) {
        if (level == Level.ERROR || level == Level.FATAL) {
            setError(message);
        }
    }

    private void setError(String error) {
        errorMsg.setText(error);
        errorMsg.setVisible(true);
        errorMsg.setManaged(true);
    }

    public void withQuery(CommentQuery commentQuery) {
        this.commentQuery = commentQuery;

        CommentQuery.CommentsType type = commentQuery.getCommentsType();

        runLater(() -> {
            boolean condensable = type == CommentQuery.CommentsType.ALL
                    && StringUtils.isEmpty(commentQuery.getNameLike())
                    && StringUtils.isEmpty(commentQuery.getTextLike());

            radioCondensed.setDisable(!condensable);

            if (!condensable) {
                radioFlattened.fire();
            }
        });
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    @Override
    public void cleanUp() {
        errorMsg.setVisible(false);
        errorMsg.setManaged(false);

        btnSubmit.setVisible(true);
        btnSubmit.setManaged(true);

        exportProgress.setVisible(false);
        exportProgress.setManaged(false);
        exportProgress.setProgress(0.0);

        radioFlattened.setDisable(false);
        radioCondensed.setDisable(false);

        btnStop.setVisible(false);
        btnStop.setManaged(false);

        btnClose.setDisable(false);
    }
}