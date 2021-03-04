package io.mattw.youtube.commentsuite.refresh;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.GroupItem;
import io.mattw.youtube.commentsuite.db.YouTubeType;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class VideoIdProducer extends ConsumerMultiProducer<GroupItem> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(10);

    private final YouTube youTube;
    private final CommentDatabase database;

    public VideoIdProducer() {
        this.youTube = CommentSuite.getYouTube();
        this.database = CommentSuite.getDatabase();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting VideoIdProducer");

        final BlockingQueue<GroupItem> queue = getBlockingQueue();
        while (shouldKeepAlive()) {
            final GroupItem item = queue.poll();
            if (item == null) {
                awaitMillis(100);
                continue;
            }

            try {
                final YouTubeType type = item.getType();
                if (type == YouTubeType.CHANNEL) {
                    fromChannel(item);
                } else if (type == YouTubeType.PLAYLIST) {
                    fromPlaylist(item);
                } else if (type == YouTubeType.VIDEO) {
                    fromVideo(item);
                }

                updateGroupItem(item);
            } catch (IOException | SQLException e) {
                sendMessage(Level.ERROR, e, String.format("Failed GroupItem %s", item));
            }

            addProcessed(1);
        }

        logger.debug("Ending VideoIdProducer");
    }

    private void fromChannel(final GroupItem channel) throws IOException, SQLException {
        logger.debug("fromChannel {}", channel);
        final ChannelListResponse response = youTube.channels()
                .list("contentDetails")
                .setKey(CommentSuite.getYouTubeApiKey())
                .setId(channel.getId())
                .execute();

        getEstimatedQuota().incrementAndGet();

        final String uploadsPlaylistId = response.getItems()
                .stream()
                .map(Channel::getContentDetails)
                .map(ChannelContentDetails::getRelatedPlaylists)
                .map(ChannelContentDetails.RelatedPlaylists::getUploads)
                .findFirst()
                .orElse(null);

        if (uploadsPlaylistId != null) {
            fromPlaylist(channel, uploadsPlaylistId);
        }
    }

    private void fromPlaylist(final GroupItem playlist) throws IOException, SQLException {
        fromPlaylist(playlist, playlist.getId());
    }

    private void fromPlaylist(final GroupItem item, final String playlistId) throws IOException, SQLException {
        logger.debug("fromPlaylist {} {}", item, playlistId);

        PlaylistItemListResponse response;
        String pageToken = "";
        do {
            response = youTube.playlistItems()
                    .list("snippet")
                    .setKey(CommentSuite.getYouTubeApiKey())
                    .setMaxResults(50L)
                    .setPlaylistId(playlistId)
                    .setPageToken(pageToken)
                    .execute();

            getEstimatedQuota().incrementAndGet();

            pageToken = response.getNextPageToken();

            final List<PlaylistItem> playlistItems = response.getItems();
            if (!playlistItems.isEmpty()) {
                final List<String> videoIds = playlistItems.stream()
                        .map(PlaylistItem::getSnippet)
                        .filter(snippet ->
                                !"Private video".equals(snippet.getTitle()) &&
                                        !"This video is private.".equals(snippet.getDescription()) &&
                                        snippet.getThumbnails() != null)
                        .map(PlaylistItemSnippet::getResourceId)
                        .map(ResourceId::getVideoId)
                        .collect(Collectors.toList());

                database.groupItems().associateVideos(item, videoIds);

                sendCollection(videoIds, String.class);

                final int diff = response.getItems().size() - videoIds.size();
                if (diff > 0) {
                    logger.debug("Ignored {} private videos", diff);
                }
            } else {
                logger.debug("Empty ?");
            }
        } while (response.getNextPageToken() != null);
    }

    private void fromVideo(final GroupItem video) throws SQLException {
        logger.debug("fromVideo {}", video);

        database.groupItems().associateVideos(video, Collections.singletonList(video.getId()));

        send(video.getId());
    }

    private void updateGroupItem(GroupItem gitem) throws SQLException {
        database.groupItems().update(gitem);
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
