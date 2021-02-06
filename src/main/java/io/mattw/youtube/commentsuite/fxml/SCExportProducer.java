package io.mattw.youtube.commentsuite.fxml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.*;
import io.mattw.youtube.commentsuite.refresh.ConsumerMultiProducer;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SCExportProducer extends ConsumerMultiProducer<String> {

    private static final Logger logger = LogManager.getLogger();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File EXPORT_FOLDER = new File("exports/");

    private final ExecutorGroup executorGroup = new ExecutorGroup(15);

    private final CommentQuery commentQuery;
    private final CommentDatabase database;
    private final boolean condensedMode;
    private final File thisExportFolder;

    public SCExportProducer(final CommentQuery commentQuery, final boolean condensed) {
        this.commentQuery = commentQuery;
        this.condensedMode = condensed;
        this.database = CommentSuite.getDatabase();

        this.thisExportFolder = new File(EXPORT_FOLDER, formatter.format(LocalDateTime.now()) + "/");
        this.thisExportFolder.mkdirs();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting SCExportProducer");

        while (shouldKeepAlive()) {
            final String videoId = getBlockingQueue().poll();
            if (videoId == null) {
                awaitMillis(5);
                continue;
            }

            try {
                final YouTubeVideo video = database.videos().get(videoId);
                if (video == null) {
                    logger.debug("Couldn't find video {}", videoId);
                    continue;
                }

                createVideoMetaFile(video);
                createCommentsFile(video);

                addProcessed(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        logger.debug("Ending SCExportProducer");
    }

    private void createVideoMetaFile(final YouTubeVideo video) {
        final File videoFile = new File(thisExportFolder, String.format("%s-meta.json", video.getId()));
        try (final FileOutputStream fos = new FileOutputStream(videoFile);
             final OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             final BufferedWriter bw = new BufferedWriter(writer)) {

            video.setAuthor(database.channels().getOrNull(video.getChannelId()));
            video.prepForExport();

            logger.debug("Writing file {}", videoFile.getName());

            gson.toJson(video, bw);

            bw.flush();
        } catch (Exception e) {
            logger.error("Failed to write json file(s)", e);

            sendMessage(Level.ERROR, "Error during export, check logs.");
        }
    }

    private void createCommentsFile(final YouTubeVideo video) {
        final File commentsFile = new File(thisExportFolder, String.format("%s-comments.json", video.getId()));
        final List<YouTubeVideo> videoList = Collections.singletonList(video);

        if (condensedMode && commentQuery.getCommentsType() == CommentQuery.CommentsType.ALL) {
            // When condensed, we want base comments to be first.
            // We'll grab replies later if the replyCount > 0
            commentQuery.setCommentsType(CommentQuery.CommentsType.COMMENTS_ONLY);
        }

        try (final FileOutputStream fos = new FileOutputStream(commentsFile);
             final OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             final JsonWriter jsonWriter = new JsonWriter(writer)) {
            logger.debug("Writing file {}", commentsFile.getName());

            jsonWriter.beginArray();

            try (final NamedParameterStatement namedParamStatement = commentQuery
                    .setVideos(Optional.of(videoList))
                    .toStatement();
                 final ResultSet resultSet = namedParamStatement.executeQuery()) {

                // starting with flattened mode (easier)
                while (resultSet.next() && !isHardShutdown()) {
                    final YouTubeComment comment = database.comments().to(resultSet);
                    comment.setAuthor(database.channels().getOrNull(comment.getChannelId()));
                    comment.prepForExport();

                    if (condensedMode && comment.getReplyCount() > 0 && !comment.isReply()) {
                        final List<YouTubeComment> replyList = database.getCommentTree(comment.getId(), false);
                        for (final YouTubeComment reply : replyList) {
                            reply.setAuthor(database.channels().getOrNull(reply.getChannelId()));
                            reply.prepForExport();
                        }

                        comment.setReplies(replyList);
                    }

                    gson.toJson(gson.toJsonTree(comment), jsonWriter);
                }
            }

            jsonWriter.endArray();

        } catch (SQLException e) {
            logger.error("Error while grabbing comments", e);

            sendMessage(Level.ERROR, "Error during export, check logs.");
            //runLater(() -> setError("Error during export, check logs."));
        } catch (IOException e) {
            logger.error("Failed to write json file", e);
        }
    }

    @Override
    public void onCompletion() {
        try {
            logger.debug("Opening folder {}", thisExportFolder.getAbsolutePath());

            Desktop.getDesktop().open(thisExportFolder);
        } catch (IOException e) {
            logger.warn("Failed to open export folder");
        }
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }

    public File getThisExportFolder() {
        return thisExportFolder;
    }
}
