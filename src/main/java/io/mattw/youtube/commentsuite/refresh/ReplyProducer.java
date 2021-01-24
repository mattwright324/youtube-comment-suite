package io.mattw.youtube.commentsuite.refresh;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentListResponse;
import io.mattw.youtube.commentsuite.FXMLSuite;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import io.mattw.youtube.commentsuite.db.YouTubeComment;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import io.mattw.youtube.commentsuite.util.StringTuple;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReplyProducer extends ConsumerMultiProducer<StringTuple> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(20);

    private final RefreshOptions options;
    private final YouTube youTube;

    public ReplyProducer(final RefreshOptions options) {
        this.options = options;
        this.youTube = FXMLSuite.getYouTube();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting ReplyProducer");

        while (shouldKeepAlive()) {
            final StringTuple tuple = getBlockingQueue().poll();
            if (tuple == null) {
                awaitMillis(5);
                continue;
            }

            try {
                CommentListResponse response;
                String pageToken = "";
                int page = 1;
                RefreshReplyPages replyPages = options.getReplyPages();
                do {
                    response = youTube.comments()
                            .list("snippet")
                            .setKey(FXMLSuite.getYouTubeApiKey())
                            .setParentId(tuple.getFirst())
                            .setPageToken(pageToken)
                            .setMaxResults(100L)
                            .execute();

                    pageToken = response.getNextPageToken();

                    final List<Comment> comments = response.getItems();

                    final List<YouTubeComment> replies = comments.stream()
                            .map(item -> new YouTubeComment(item, tuple.getSecond()))
                            .filter(yc -> StringUtils.isNotEmpty(yc.getChannelId()) /* filter out G+ comments */)
                            .collect(Collectors.toList());
                    sendCollection(replies, YouTubeComment.class);

                    final List<YouTubeChannel> channels = response.getItems().stream()
                            .filter(distinctByKey(Comment::getId))
                            .map(YouTubeChannel::new)
                            .collect(Collectors.toList());
                    sendCollection(channels, YouTubeChannel.class);

                    awaitMillis(50);
                } while (pageToken != null && page++ < replyPages.getPageCount() && !isHardShutdown());
            } catch (IOException e) {
                sendMessage(Level.ERROR, e, String.format("Couldn't grab commentThread[id={}]", tuple.getFirst()));
            }

            addProcessed(1);
            awaitMillis(5);
        }

        logger.debug("Ending ReplyProducer");
    }

    private <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
