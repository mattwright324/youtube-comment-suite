package io.mattw.youtube.commentsuite.refresh;

import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
import io.mattw.youtube.commentsuite.util.ElapsedTime;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class CommentConsumer extends ConsumerMultiProducer<YouTubeComment> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(10);

    private final RefreshOptions options;
    private final CommentDatabase database;

    private AtomicLong totalComments = new AtomicLong();
    private AtomicLong newComments = new AtomicLong();

    public CommentConsumer(final RefreshOptions options) {
        this.options = options;
        this.database = CommentSuite.getDatabase();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting CommentConsumer");

        final ElapsedTime elapsedTime = new ElapsedTime();
        final List<YouTubeComment> comments = new ArrayList<>();
        while (shouldKeepAlive()) {
            final YouTubeComment comment = getBlockingQueue().poll();
            if (comment == null) {
                awaitMillis(100);
                continue;
            } else {
                comments.add(comment);
                addProcessed(1);
            }

            if (comments.size() >= 1000 || (elapsedTime.getElapsed().toMillis() >= 2000 && !comments.isEmpty())) {
                insertComments(comments);
                elapsedTime.setNow();
            }
        }

        if (!comments.isEmpty()) {
            insertComments(comments);
        }

        logger.debug("Ending CommentConsumer");
    }

    private void insertComments(final List<YouTubeComment> comments) {
        try {
            final List<String> commentIds = comments.stream()
                    .map(YouTubeComment::getId)
                    .collect(Collectors.toList());

            totalComments.addAndGet(comments.size());
            newComments.addAndGet(database.countCommentsNotExisting(commentIds));

            if (options.isUpdateCommentsChannels()) {
                database.comments().updateAll(comments);
            } else {
                database.comments().insertAll(comments);
            }

            logger.debug("Inserted comments {}", comments.size());

            comments.clear();
        } catch (SQLException e) {
            logger.error("Error on comment submit", e);
        }
    }

    public AtomicLong getTotalComments() {
        return totalComments;
    }

    public AtomicLong getNewComments() {
        return newComments;
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
