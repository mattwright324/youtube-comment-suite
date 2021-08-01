package io.mattw.youtube.commentsuite.fxml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.*;
import io.mattw.youtube.commentsuite.refresh.ConsumerMultiProducer;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.mattw.youtube.commentsuite.fxml.ExportFormat.*;

public class SCExportProducer extends ConsumerMultiProducer<YouTubeVideo> {

    private static final Logger logger = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final ExecutorGroup executorGroup = new ExecutorGroup(15);

    private final CommentQuery commentQuery;
    private final CommentDatabase database;
    private final File exportFolder;
    private final ExportFormat format;

    public SCExportProducer(final CommentQuery commentQuery, final File exportFolder, final ExportFormat format) {
        this.commentQuery = commentQuery;
        this.database = CommentSuite.getDatabase();
        this.exportFolder = exportFolder;
        this.format = format;
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting SCExportProducer");

        while (shouldKeepAlive()) {
            final YouTubeVideo video = getBlockingQueue().poll();
            if (video == null) {
                awaitMillis(5);
                continue;
            }

            createCommentsFile(video);

            addProcessed(1);
        }

        logger.debug("Ending SCExportProducer");
    }

    private void createCommentsFile(final YouTubeVideo video) {
        final File commentsFile = new File(exportFolder, String.format("comments-%s.%s", video.getId(), format.getExtension()));
        final List<YouTubeVideo> videoList = Collections.singletonList(video);

        boolean hasComments = false;

        try (final FileOutputStream fos = new FileOutputStream(commentsFile);
             final OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            final JsonWriter jsonWriter = new JsonWriter(writer);
            try {
                logger.debug("Writing file {}", commentsFile.getName());

                if (format == JSON) {
                    jsonWriter.beginArray();
                } else {
                    printer.printRecord((Object[]) YouTubeVideo.CSV_HEADER);
                }

                final CommentQuery duplicateQuery = commentQuery.duplicate();
                try (final NamedParameterStatement namedParamStatement = duplicateQuery
                        .setVideos(Optional.of(videoList))
                        .toStatement();
                     final ResultSet resultSet = namedParamStatement.executeQuery()) {

                    while (resultSet.next() && isNotHardShutdown()) {
                        hasComments = true;

                        final YouTubeComment comment = database.comments().to(resultSet);
                        comment.setAuthor(database.channels().getOrNull(comment.getChannelId()));
                        comment.prepForExport();

                        if (format == JSON) {
                            gson.toJson(gson.toJsonTree(comment), jsonWriter);
                        } else {
                            printer.printRecord(comment.getCsvRow());
                        }
                    }
                }

                if (format == JSON) {
                    jsonWriter.endArray();
                }
            } finally {
                if (format == JSON) {
                    jsonWriter.close();
                }
            }
        } catch (SQLException e) {
            logger.error("Error while grabbing comments", e);

            sendMessage(Level.ERROR, "Error during export, check logs.");
        } catch (IOException e) {
            logger.error("Failed to write file(s)", e);
        } finally {
            if (!hasComments) {
                commentsFile.delete();
            }
        }
    }

    @Override
    public void onCompletion() {
        try {
            logger.debug("Opening folder {}", exportFolder.getAbsolutePath());

            Desktop.getDesktop().open(exportFolder);
        } catch (IOException e) {
            logger.warn("Failed to open export folder");
        }
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
