package io.mattw.youtube.commentsuite.fxml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.ImageCache;
import io.mattw.youtube.commentsuite.db.*;
import io.mattw.youtube.commentsuite.util.Threads;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Set;

import static io.mattw.youtube.commentsuite.fxml.ExportCommentFormat.*;
import static io.mattw.youtube.commentsuite.fxml.ExportFormat.*;
import static javafx.application.Platform.runLater;

/**
 * This modal allows the full export of all comments in a query to the specified format
 *
 * @see SearchComments
 */
public class SCExportModal extends VBox implements Cleanable, ImageCache {

    private static final Logger logger = LogManager.getLogger();
    private static final File EXPORT_FOLDER = new File("exports/");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String ls = System.getProperty("line.separator");
    private static final String example =
            "about.txt%1$s - Metadata about this export%1$s" +
                    "videos.%2$s%1$s - %3$s of all videos%1$s - Affected by group(item) filters%1$s" +
                    "comments%4$s.%2$s%1$s - %3$s of comments%1$s - Affected by comment filters%5$s";
    private static final String exampleJsonSeparate = String.format(example, ls, "json", "Array", "-{videoId}", ls + " - Separate file per video");
    private static final String exampleJsonSingle = String.format(example, ls, "json", "Array", "", ls + " - All in one file");
    private static final String exampleCsvSeparate = String.format(example, ls, "csv", "List", "-{videoId}", ls + " - Separate file per video");
    private static final String exampleCsvSingle = String.format(example, ls, "csv", "List", "", ls + " - All in one file");


    @FXML
    private Label errorMsg;
    @FXML
    private VBox exportPane;
    @FXML
    private RadioButton radioJSON, radioCSV, radioSeparate, radioSingle;
    @FXML
    private TextArea exportExample;
    @FXML
    private ProgressBar exportProgress;
    @FXML
    private Button btnClose;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnSubmit;

    private CommentQuery commentQuery;
    private CommentDatabase database;
    private SCExportProducer exportProducer;
    private boolean shutdown = false;
    private long commentsProcessed;

    public SCExportModal() {
        logger.debug("Initialize SCExportModal");

        database = CommentSuite.getDatabase();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCExportModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            cleanUp();

            radioJSON.setOnAction(ae -> updateExample());
            radioCSV.setOnAction(ae -> updateExample());
            radioSeparate.setOnAction(ae -> updateExample());
            radioSingle.setOnAction(ae -> updateExample());

            radioJSON.fire();
            radioSeparate.fire();

            btnStop.setOnAction(ae -> {
                shutdown = true;
                exportProducer.setHardShutdown(true);

                runLater(() -> btnStop.setDisable(true));
            });

            btnSubmit.setOnAction((ae) -> new Thread(this::startExport).start());
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    private void updateExample() {
        runLater(() -> {
            String exampleText;
            if (radioJSON.isSelected()) {
                exampleText = radioSeparate.isSelected() ? exampleJsonSeparate : exampleJsonSingle;
            } else {
                exampleText = radioSeparate.isSelected() ? exampleCsvSeparate : exampleCsvSingle;
            }
            exportExample.setText(exampleText);
        });
    }

    private void startExport() {
        runLater(() -> {
            btnSubmit.setVisible(false);
            btnSubmit.setManaged(false);

            exportProgress.setVisible(true);
            exportProgress.setManaged(true);

            radioCSV.setDisable(true);
            radioJSON.setDisable(true);
            radioSeparate.setDisable(true);
            radioSingle.setDisable(true);

            btnStop.setVisible(true);
            btnStop.setManaged(true);

            btnClose.setDisable(true);
        });

        try {
            final CommentQuery duplicateQuery = commentQuery.duplicate();

            final Set<YouTubeVideo> videos = duplicateQuery.getUniqueVideos();
            final ExportFormat format = radioJSON.isSelected() ? JSON : CSV;
            final ExportCommentFormat commentFormat = radioSeparate.isSelected() ? PER_VIDEO : SINGLE;

            final File exportFolder = new File(EXPORT_FOLDER, formatter.format(LocalDateTime.now()) + "/");
            exportFolder.mkdirs();

            createAboutFile(exportFolder, duplicateQuery);
            createVideosFile(exportFolder, videos, format);

            if (commentFormat == PER_VIDEO) {
                exportProducer = new SCExportProducer(commentQuery, exportFolder, format);
                exportProducer.setMessageFunc(this::onMessage);
                exportProducer.accept(videos);
                exportProducer.startProducing();

                new Thread(() -> {
                    while (exportProducer.shouldKeepAlive()) {
                        updateProgressPerVideo();

                        Threads.awaitMillis(100);
                    }

                    if (exportProducer.isNotHardShutdown()) {
                        btnClose.fire();
                    }
                }).start();

                exportProducer.getExecutorGroup().await();
                updateProgressPerVideo();
                exportProducer.onCompletion();
            } else {
                createSingleCommentsFile(exportFolder, duplicateQuery, format);
                updateProgressSingle();

                try {
                    logger.debug("Opening folder {}", exportFolder.getAbsolutePath());

                    Desktop.getDesktop().open(exportFolder);
                } catch (IOException e) {
                    logger.warn("Failed to open export folder");
                }
            }

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

    private void updateProgressPerVideo() {
        final double progress = exportProducer.getTotalProcessed().get() / (exportProducer.getTotalAccepted().get() * 1d);
        runLater(() -> exportProgress.setProgress(progress));
    }

    private void updateProgressSingle() {
        final double progress = commentsProcessed / (commentQuery.getTotalResults() * 1d);
        runLater(() -> exportProgress.setProgress(progress));
    }

    private void createAboutFile(final File exportFolder, final CommentQuery query) {
        final File aboutFile = new File(exportFolder, "about.txt");
        try (final FileOutputStream fos = new FileOutputStream(aboutFile);
             final OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             final BufferedWriter bw = new BufferedWriter(writer)) {

            logger.debug("Writing file {}", aboutFile.getName());

            query.prepForExport();

            bw.append("YouTube Comment Suite ").append(CommentSuite.getProperties().getProperty("version")).append(ls).append(ls);
            bw.append("Export date: ").append(exportFolder.getName()).append(ls).append(ls);
            bw.append("Search settings json:").append(ls);

            gson.toJson(query, bw);

            bw.flush();
        } catch (Exception e) {
            logger.error("Failed to write about file", e);

            runLater(() -> setError("Problem during export"));
        }
    }

    private void createVideosFile(final File exportFolder, final Collection<YouTubeVideo> videos, final ExportFormat format) {
        final File videosFile = new File(exportFolder, "videos." + format.getExtension());
        try (final FileOutputStream fos = new FileOutputStream(videosFile);
             final OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             final BufferedWriter bw = new BufferedWriter(writer);
             final CSVPrinter printer = new CSVPrinter(bw, CSVFormat.DEFAULT)) {

            logger.debug("Writing file {}", videosFile.getName());

            if (format == JSON) {
                gson.toJson(videos, bw);
            } else {
                printer.printRecord((Object[]) YouTubeVideo.CSV_HEADER);

                for (YouTubeVideo video : videos) {
                    printer.printRecord(video.getCsvRow());
                }
            }

            bw.flush();
        } catch (Exception e) {
            logger.error("Failed to write video file", e);

            runLater(() -> setError("Problem during export"));
        }
    }

    private void createSingleCommentsFile(final File exportFolder, final CommentQuery query, final ExportFormat format) {
        final File videosFile = new File(exportFolder, "comments." + format.getExtension());

        try (final FileOutputStream fos = new FileOutputStream(videosFile);
             final OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             final BufferedWriter bw = new BufferedWriter(writer);
             final CSVPrinter printer = new CSVPrinter(bw, CSVFormat.DEFAULT)) {
            final JsonWriter jsonWriter = new JsonWriter(bw);
            try {
                logger.debug("Writing file {}", videosFile.getName());

                if (format == CSV) {
                    printer.printRecord((Object[]) YouTubeComment.CSV_HEADER);
                }

                try (final NamedParameterStatement namedParamStatement = query.toStatement()) {
                    final ResultSet resultSet = namedParamStatement.executeQuery();

                    while (resultSet.next() && !shutdown) {
                        final YouTubeComment comment = database.comments().to(resultSet);
                        comment.setAuthor(database.channels().getOrNull(comment.getChannelId()));
                        comment.prepForExport();

                        if (format == JSON) {
                            gson.toJson(gson.toJsonTree(comment), jsonWriter);
                        } else {
                            printer.printRecord(comment.getCsvRow());
                        }

                        commentsProcessed++;

                        final long total = query.getTotalResults();
                        if (total < 25000 && commentsProcessed % 10 == 0 || commentsProcessed % 100 == 0) {
                            updateProgressSingle();
                        }
                    }
                }

                bw.flush();
            } finally {
                if (format == JSON) {
                    jsonWriter.close();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to write comments file", e);

            runLater(() -> setError("Problem during export"));
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

        radioCSV.setDisable(false);
        radioJSON.setDisable(false);
        radioSeparate.setDisable(false);
        radioSingle.setDisable(false);

        btnStop.setVisible(false);
        btnStop.setManaged(false);

        btnClose.setDisable(false);

        commentsProcessed = 0;
    }
}