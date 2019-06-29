package io.mattw.youtube.commentsuite;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import io.mattw.youtube.commentsuite.db.*;
import io.mattw.youtube.commentsuite.util.ElapsedTime;
import io.mattw.youtube.commentsuite.util.ExecutorGroup;
import io.mattw.youtube.commentsuite.util.Tuple;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressBar;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

/**
 * Refresh implementation for MGMVRefreshModal with bindable properties.
 *
 * Interacts heavily with YouTube API v3 and the CommentDatabase.
 *
 * @author mattwright324
 */
public class MGMVGroupRefresh extends Thread implements RefreshInterface {

    private static final Logger logger = LogManager.getLogger();

    private Group group;
    private ObservableList<String> errorList = FXCollections.observableArrayList();
    private boolean hardShutdown = false;
    private boolean endedOnError = false;
    private BooleanProperty ended = new SimpleBooleanProperty(false);
    private DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private StringProperty statusStep = new SimpleStringProperty("Preparing");
    private StringProperty elapsedTime = new SimpleStringProperty("0 ms");
    private LongProperty newVideos = new SimpleLongProperty(0);
    private LongProperty totalVideos = new SimpleLongProperty(0);
    private LongProperty newComments = new SimpleLongProperty(0);
    private LongProperty totalComments = new SimpleLongProperty(0);

    private final int maxAttempts = 5;
    private Double videoProgress = 0.0;
    private Double totalProgress = 0.0;
    private ElapsedTime elapsedTimer = new ElapsedTime();
    private SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");

    private ExecutorGroup gitemGroup = new ExecutorGroup(1);
    private ExecutorGroup videoCommentsGroup = new ExecutorGroup(10);
    private ExecutorGroup repliesGroup = new ExecutorGroup(20);
    private ExecutorGroup commentInsertGroup = new ExecutorGroup(2);
    private ExecutorGroup channelIdGroup = new ExecutorGroup(20);
    private ExecutorGroup channelInsertGroup = new ExecutorGroup(2);

    private LinkedBlockingQueue<GroupItem> gitemQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> videoIdQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<YouTubeVideo> videoQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Tuple<String,String>> commentThreadQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<YouTubeComment> commentQueue = new LinkedBlockingQueue<>();
    private List<CommentDatabase.GroupItemVideo> gitemVideo = new ArrayList<>();
    private LinkedBlockingQueue<String> channelQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<YouTubeChannel> channelInsertQueue = new LinkedBlockingQueue<>();

    private YouTube youtube = FXMLSuite.getYouTube();
    private CommentDatabase database = FXMLSuite.getDatabase();

    public MGMVGroupRefresh(Group group) {
        this.group = group;
    }


    /**
     * Starts group refresh, starting stopping threads, handles fatal errors.
     */
    public void run() {
        runLater(() -> progress.setValue(ProgressBar.INDETERMINATE_PROGRESS));
        logger.debug("Refresh Start");
        elapsedTimer.setNow();
        startElapsedTimer();
        try {
            gitemQueue.addAll(database.getGroupItems(group));

            runLater(() -> statusStep.setValue("Grabbing Videos"));
            startAndAwaitParsingGitems();

            if(isHardShutdown()) {
                throw new InterruptedException("Manually stopped.");
            }

            runLater(() -> statusStep.setValue("Grabbing Video Data"));
            totalProgress += videoIdQueue.size();
            updateProgress();
            List<String> videoIds = new ArrayList<>();
            while(!videoIdQueue.isEmpty()  && !isHardShutdown()) {
                videoIds.add(videoIdQueue.poll());
                if(videoIds.size() == 50) {
                    parseVideoIdsToObjects(videoIds);
                    videoIds.clear();
                }
            }
            parseVideoIdsToObjects(videoIds);
            database.insertGroupItemVideo(gitemVideo);
            database.commit();

            if(isHardShutdown()) {
                throw new InterruptedException("Manually stopped.");
            }

            videoProgress = 0.0;
            updateProgress();

            runLater(() -> {
                statusStep.setValue("Grabbing Comments");
                progress.setValue(0.0);
            });
            startVideoParse();
            startBackgroundThreads();
            videoCommentsGroup.await();
            awaitBackgroundThreads();
            database.commit();

            updateProgress();
            runLater(() -> statusStep.setValue("Done"));
        } catch (Exception e) {
            try {
                database.commit();
            } catch (SQLException e2) {
                appendError("Failed to commit dateFrom refresh failure.");
            }

            appendError(e.getLocalizedMessage());

            logger.error("FATAL: Refresh Failed", e);
            runLater(() -> {
                statusStep.setValue(statusStep.getValue() + " (FATAL)");
                if(progress.getValue() == ProgressBar.INDETERMINATE_PROGRESS) {
                    progress.setValue(0.0);
                }
            });
            endedOnError = true;
        }
        updateProgress();
        runLater(() -> ended.setValue(true));
        logger.debug("Refresh End [progress={},elapsedTime={},newVideos={},totalVideos={},newComments={}," +
                "totalComments={},lastStep={}]",
                progress.getValue(),
                elapsedTime.getValue(),
                newVideos.getValue(),
                totalVideos.getValue(),
                newComments.getValue(),
                totalComments.getValue(),
                statusStep.getValue());
    }

    /**
     * Consumes the GroupItems listed under the group, finds the videoId's associated, and places them in a queue.
     * @throws InterruptedException executors/thread was interrupted
     */
    private void startAndAwaitParsingGitems() throws InterruptedException {
        gitemGroup.submitAndShutdown(() -> {
            logger.debug("Starting Gitem-Video Thread");
            while(!gitemQueue.isEmpty()) {
                GroupItem gitem = gitemQueue.poll();
                if(gitem != null) {
                    logger.debug("Grabbing Video for GroupItem[id={},type={},name={}]",
                            gitem.getId(), gitem.getTypeId(), gitem.getTitle());
                    try {
                        database.updateGroupItemLastChecked(gitem);

                        if(gitem.getTypeId() == YType.CHANNEL || gitem.getTypeId() == YType.PLAYLIST) {
                            String playlistId = gitem.getId();
                            if(gitem.getTypeId() == YType.CHANNEL) {
                                ChannelListResponse cl = youtube.channels().list("contentDetails")
                                        .setKey(FXMLSuite.getYouTubeApiKey())
                                        .setMaxResults(50L)
                                        .setId(gitem.getId())
                                        .execute();

                                List<Channel> items = cl.getItems();
                                if(!items.isEmpty() && items.get(0).getContentDetails() != null) {
                                    playlistId = items.get(0).getContentDetails().getRelatedPlaylists().getUploads();
                                }
                            }

                            PlaylistItemListResponse pil;
                            String pageToken = "";
                            do {
                                pil = youtube.playlistItems().list("snippet")
                                        .setKey(FXMLSuite.getYouTubeApiKey())
                                        .setMaxResults(50L)
                                        .setPlaylistId(playlistId)
                                        .setPageToken(pageToken)
                                        .execute();

                                pageToken = pil.getNextPageToken();
                                List<PlaylistItem> items = pil.getItems();
                                if(!items.isEmpty()) {
                                    List<String> videoIds = items.stream()
                                            .filter(item ->
                                                    !"Private video".equals(item.getSnippet().getTitle()) &&
                                                            !"This video is private.".equals(item.getSnippet().getDescription()) &&
                                                            item.getSnippet().getThumbnails() != null)
                                            .map(item -> item.getSnippet().getResourceId().getVideoId())
                                            .collect(Collectors.toList());
                                    videoIds.forEach(videoId ->
                                            gitemVideo.add(new CommentDatabase.GroupItemVideo(
                                                    gitem.getId(), videoId))
                                    );
                                    videoIdQueue.addAll(videoIds);
                                    incrLongProperty(newVideos, database.countVideosNotExisting(videoIds));
                                    incrLongProperty(totalVideos, videoIds.size());

                                    int diff = pil.getItems().size() - videoIds.size();
                                    if(diff > 0) {
                                        logger.debug("Ignored {} private videos", diff);
                                    }
                                }
                            } while(pil.getNextPageToken() != null  && !isHardShutdown());
                        } else if(gitem.getTypeId() == YType.VIDEO) {
                            try {
                                gitemVideo.add(new CommentDatabase.GroupItemVideo(
                                        gitem.getId(), gitem.getId()));

                                videoIdQueue.put(gitem.getId());
                                if(database.doesVideoExist(gitem.getId())) {
                                    incrLongProperty(newVideos, 1);
                                }
                                incrLongProperty(totalVideos, 1);
                            } catch (InterruptedException ignored) {}
                        }
                    } catch (SQLException | IOException e) {
                        appendError(String.format("Failed GItem[id=%s]", gitem.getId()));

                        logger.error(e);
                    }
                }
            }
            logger.debug("Ending Gitem-Video Thread");
        });
        gitemGroup.await();
    }

    private <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * Consumes YouTubeVideo objects in the queue.
     */
    private void startVideoParse() {
        logger.debug("Starting Comment Grabbing [videos={}]", videoQueue.size());

        videoCommentsGroup.submitAndShutdown(() -> {
            while(!videoQueue.isEmpty()) {
                YouTubeVideo video = videoQueue.poll();
                if(video != null) {
                    int attempts = 0;
                    CommentThreadListResponse ctl;
                    String pageToken = "";
                    do {
                        try {
                            logger.info("{} - {}", video.getId(), video.getTitle());
                            do {
                                ctl = youtube.commentThreads().list("snippet")
                                        .setKey(FXMLSuite.getYouTubeApiKey())
                                        .setVideoId(video.getId())
                                        .setMaxResults(50L)
                                        .setPageToken(pageToken)
                                        .execute();

                                pageToken = ctl.getNextPageToken();

                                try {
                                    // Maybe comments were re-enabled if we got a 403 in the past.

                                    database.updateVideoHttpCode(video.getId(), 200);
                                } catch (SQLException sqle) {
                                    logger.error("Failed to update video http response code", sqle);
                                }

                                if(!ctl.getItems().isEmpty()) {
                                    List<YouTubeComment> comments = ctl.getItems().stream()
                                            .map(YouTubeComment::new)
                                            .collect(Collectors.toList());

                                    comments.forEach(c -> {
                                        if(c.getReplyCount() > 0) {
                                            incrTotalProgress(1);
                                            updateProgress();

                                            /*
                                             * Tuple to hold commentThreadId and videoId for consuming threads.
                                             *
                                             * VideoId's are not included in comment thread / comments.list. Using tuple
                                             * to pair a comment thread id with the video it was found on.
                                             */
                                            commentThreadQueue.add(new Tuple<>(c.getId(), video.getId()));
                                        }
                                    });

                                    commentQueue.addAll(comments);

                                    channelQueue.addAll(comments.stream()
                                            .map(YouTubeComment::getChannelId)
                                            .distinct()
                                            .collect(Collectors.toList()));
                                }

                                awaitMillis(50);
                            } while (pageToken != null && !isHardShutdown());
                            break;
                        } catch (IOException e) {
                            if(e instanceof GoogleJsonResponseException) {
                                GoogleJsonResponseException ge = (GoogleJsonResponseException) e;

                                try {
                                    database.updateVideoHttpCode(video.getId(), ge.getStatusCode());
                                } catch (SQLException sqle) {
                                    logger.error("Failed to update video http response code", sqle);
                                }

                                if(ge.getStatusCode() == 400) {
                                    String message = String.format("[%s/%s] %s [videoId=%s]", attempts, maxAttempts,
                                            e.getClass().getSimpleName(), video.getId());

                                    appendError(message);
                                    logger.warn(message, e);

                                    attempts++;
                                } else if(ge.getStatusCode() == 403) {
                                    String message = String.format("Comments Disabled [videoId=%s]", video.getId());

                                    appendError(message);
                                    logger.warn(message, e);

                                    break;
                                } else {
                                    String message = String.format("Other error received %s [videoId=%s]",
                                            ge.getStatusCode(), video.getId());

                                    appendError(message);
                                    logger.warn(message, e);

                                    break;
                                }
                            } else {
                                String message = String.format("[%s/%s] %s [videoId=%s]", attempts, maxAttempts,
                                        e.getClass().getSimpleName(), video.getId());

                                attempts++;
                                appendError(message);
                                logger.warn(message, e);
                            }
                        }
                    } while(attempts < maxAttempts && !isHardShutdown());

                    incrVideoProgress();
                    updateProgress();
                }
                awaitMillis(500);
            }
            logger.debug("Video Consumer [END] [leftInQueue={}]", videoQueue.size());
        });
    }

    /**
     * Background threads:
     * - Comment Consumers (take from queue into database)
     * - Comment Thread Consumers (produce comments from comment-threads)
     * - ChannelId Consumers (produce channels from channel-ids for channel-insert consumers)
     * - Channel Insert Consumers (take from queue into database)
     */
    private void startBackgroundThreads() {
        logger.debug("Starting Reply Threads");
        repliesGroup.submitAndShutdown(() -> {
            while(!commentThreadQueue.isEmpty() || videoCommentsGroup.isStillWorking()) {
                Tuple<String,String> tuple = commentThreadQueue.poll();
                if(tuple != null) {
                    try {
                        CommentListResponse cl;
                        String pageToken = "";
                        do {
                            cl = youtube.comments().list("snippet")
                                    .setKey(FXMLSuite.getYouTubeApiKey())
                                    .setParentId(tuple.getFirst())
                                    .setPageToken(pageToken)
                                    .execute();

                            pageToken = cl.getNextPageToken();

                            List<Comment> comments = cl.getItems();

                            List<YouTubeComment> replies = comments.stream()
                                    .map(item -> new YouTubeComment(item, tuple.getSecond()))
                                    .filter(yc -> StringUtils.isNotEmpty(yc.getChannelId())/* filter out G+ comments */)
                                    .collect(Collectors.toList());
                            commentQueue.addAll(replies);

                            List<YouTubeChannel> channels = cl.getItems().stream()
                                    .filter(distinctByKey(Comment::getId))
                                    .map(YouTubeChannel::new)
                                    .collect(Collectors.toList());
                            channelInsertQueue.addAll(channels);

                            awaitMillis(50);
                        } while (pageToken != null && !isHardShutdown());
                    } catch (IOException e) {
                        logger.error("Couldn't grab commentThread[id={}]", tuple.getFirst(), e);
                    }
                    incrVideoProgress();
                    updateProgress();
                }
                awaitMillis(100);

                if(isHardShutdown()) break;
            }
            logger.debug("CommentThread Consumer [END] [leftInQueue={}]", commentThreadQueue.size());
        });

        logger.debug("Starting Comment/Reply Insert Threads");
        commentInsertGroup.submitAndShutdown(() -> {
            ElapsedTime elapsed = new ElapsedTime();

            List<YouTubeComment> comments = new ArrayList<>();
            while(!commentQueue.isEmpty() || videoCommentsGroup.isStillWorking() || repliesGroup.isStillWorking() || !comments.isEmpty()) {
                YouTubeComment comment = commentQueue.poll();
                if(comment != null) {
                    comments.add(comment);
                } else {
                    awaitMillis(5);
                }

                if(comments.size() >= 1000 || (elapsed.getElapsed().toMillis() >= 1200 && !comments.isEmpty())) {
                    try {
                        logger.trace("CommentConsumer[submitting={},leftInQueue={},inThreadQueue={}]",
                                comments.size(),
                                commentQueue.size(),
                                commentThreadQueue.size());
                        submitComments(comments);

                        elapsed.setNow();
                        comments.clear();
                    } catch (SQLException e) {
                        logger.error("Error on comment submit", e);
                    }
                }

                if(isHardShutdown()) break;
            }
            if(!comments.isEmpty()) {
                try {
                    submitComments(comments);
                } catch (SQLException e) {
                    logger.error("Error on comment submit", e);
                }
            }
            logger.debug("Comment Consumer [END] [leftInQueue={}]", commentThreadQueue.size());
        });

        logger.debug("Starting ChannelId Threads");
        channelIdGroup.submitAndShutdown(() -> {
            ElapsedTime elapsed = new ElapsedTime();

            List<String> channelIds = new ArrayList<>();
            while(!channelQueue.isEmpty() || videoCommentsGroup.isStillWorking() || repliesGroup.isStillWorking() || !channelIds.isEmpty()) {
                String channelId = channelQueue.poll();
                if(channelId != null && !channelIds.contains(channelId) && channelIds.size() < 50) {
                    channelIds.add(channelId);
                } else {
                    awaitMillis(5);
                }

                if(channelIds.size() == 50 || (elapsed.getElapsed().toMillis() >= 500 && !channelIds.isEmpty())) {
                    threadChannelIdConsume(channelIds);
                    elapsed.setNow();
                }

                if(isHardShutdown()) break;
            }

            if(channelIds.isEmpty()) {
                threadChannelIdConsume(channelIds);
            }

            logger.debug("ChannelId Consumer [END] [leftInQueue={}]", channelQueue.size());
        });

        logger.debug("Starting Channel Insert Threads");
        channelInsertGroup.submitAndShutdown(() -> {
            ElapsedTime elapsed = new ElapsedTime();

            List<YouTubeChannel> insert = new ArrayList<>();
            while(!channelInsertQueue.isEmpty() || channelIdGroup.isStillWorking() || !insert.isEmpty()) {
                YouTubeChannel channel = channelInsertQueue.poll();
                if(channel != null) {
                    insert.add(channel);
                } else {
                    awaitMillis(5);
                }

                // Interval insertion for more efficient work.
                if(insert.size() >= 1000 || (elapsed.getElapsed().toMillis() >= 1200 && !insert.isEmpty())) {
                    threadInsertChannels(insert);
                    elapsed.setNow();
                }

                if(isHardShutdown()) break;
            }

            logger.debug("Channel Insert Consumer [END] [leftInQueue={}]", channelInsertQueue.size());
        });
    }

    private void threadChannelIdConsume(List<String> channelIds) {
        while(channelIds.size() > 50) {
            String overflowId = channelIds.get(0);
            channelIds.remove(0);

            logger.trace("Removing overflow id and putting back in queue {}", overflowId);

            channelQueue.add(overflowId);
        }

        logger.trace("ChannelId Consumer[submitting={},leftInQueue={}]",
                channelIds.size(),
                channelQueue.size());

        try {
            ChannelListResponse cl = youtube.channels().list("snippet")
                    .setKey(FXMLSuite.getYouTubeApiKey())
                    .setId(String.join(",", channelIds))
                    .setMaxResults(50L)
                    .execute();

            List<YouTubeChannel> channels = cl.getItems().stream().map(YouTubeChannel::new)
                    .collect(Collectors.toList());

            channelInsertQueue.addAll(channels);
            channelIds.clear();
        } catch (GoogleJsonResponseException e) {
            if(e.getStatusCode() == 400) {
                logger.warn(e.getDetails().getMessage());
                logger.warn("filter parameters [id={}]", String.join(",", channelIds));
            } else {
                e.printStackTrace();
            }
        } catch (IOException e) {
            logger.error("Error on channel grab", e);
        }
    }

    private void threadInsertChannels(List<YouTubeChannel> channels) {
        try {
            logger.trace("Channel Insert Consumer [submitting={},leftInQueue={}]",
                    channels.size(),
                    channelInsertQueue.size());

            database.insertChannels(channels);
            channels.clear();
        } catch (SQLException e) {
            logger.error("Error on channel submit", e);
        }
    }

    /**
     * Await the background threads to finish dateTo continuing.
     * @throws InterruptedException executors/thread was interrupted
     */
    private void awaitBackgroundThreads() throws InterruptedException {
        logger.debug("Awaiting background threads to close.");
        repliesGroup.await();
        commentInsertGroup.await();
        channelIdGroup.await();
        channelInsertGroup.await();
    }

    /**
     * Elapsed timer start and stopped only through the endedProperty()
     *
     * Measures how long run() takes from start to finish.
     */
    private void startElapsedTimer() {
        ExecutorService es = Executors.newFixedThreadPool(1);
        es.submit(() -> {
            logger.debug("Starting Elapsed Timer");
            while(!ended.getValue()) {
                runLater(() -> elapsedTime.setValue(elapsedTimer.humanReadableFormat()));
                awaitMillis(27);
            }
            logger.debug("Ended Elapsed Timer");
        });
        es.shutdown();
    }

    /**
     * Takes a list of videoId's and adds their data to the video queue and submits to database.
     * @param videoIds list of YouTube video ids
     * @throws IOException failed to call YouTube API
     * @throws SQLException failed to insert to database
     */
    private void parseVideoIdsToObjects(List<String> videoIds) throws IOException, SQLException {
        logger.debug("Grabbing Video Data [ids={}]", videoIds.toString());
        YouTube.Videos.List yvl = youtube.videos().list("snippet,statistics")
                .setKey(FXMLSuite.getYouTubeApiKey())
                .setMaxResults(50L)
                .setId(String.join(",", videoIds))
                .setPageToken("");
        VideoListResponse vl = yvl.execute();
        if(!vl.getItems().isEmpty()) {
            List<YouTubeVideo> videos = vl.getItems().stream()
                    .map(YouTubeVideo::new).collect(Collectors.toList());
            videoQueue.addAll(videos);
            database.insertVideos(videos);
        }
        incrVideoProgress(videoIds.size());
        updateProgress();
    }

    /**
     * Inserts comments into database and updates statuses.
     * @param comments list of YouTubeComments
     * @throws SQLException failed to insert to database
     */
    private synchronized void submitComments(List<YouTubeComment> comments) throws SQLException {
        List<String> commentIds = comments.stream()
                .map(YouTubeComment::getId).collect(Collectors.toList());
        final long notYetExisting = database.countCommentsNotExisting(commentIds);
        final long totalInserting = comments.size();
        final long currentTotal = totalComments.getValue();
        incrLongProperty(newComments, notYetExisting);
        setLongPropertyValue(totalComments, currentTotal+totalInserting);
        database.insertComments(comments);
        updateProgress();
    }

    private void awaitMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }

    private synchronized void incrVideoProgress(double value) {
        videoProgress += value;
    }

    private synchronized void incrVideoProgress() {
        incrVideoProgress(1);
    }

    private synchronized void incrTotalProgress(double value) {
        totalProgress += value;
    }

    private synchronized void updateProgress() {
        runLater(() -> progress.setValue(videoProgress / totalProgress));
    }

    private synchronized void setLongPropertyValue(LongProperty longProp, long value) {
        runLater(() -> longProp.setValue(value));
    }

    private synchronized void incrLongProperty(LongProperty longProp, long value) {
        setLongPropertyValue(longProp, longProp.getValue()+value);
    }

    public void appendError(String error) {
        String time = sdf.format(new Date());
        runLater(() -> errorList.add(0, String.format("%s - %s", time, error)));
    }

    /**
     * Hard (manual) shutdown, signalled by the user and abruptly ends the refresh as soon as it can.
     */
    public void hardShutdown() {
        logger.debug("Signalling Hard Shutdown");
        this.hardShutdown = true;
    }

    public LongProperty newVideosProperty() {
        return newVideos;
    }

    public LongProperty totalVideosProperty() {
        return totalVideos;
    }

    public LongProperty newCommentsProperty() {
        return newComments;
    }

    public LongProperty totalCommentsProperty() {
        return totalComments;
    }

    public BooleanProperty endedProperty() {
        return ended;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public StringProperty statusStepProperty() {
        return statusStep;
    }

    public StringProperty elapsedTimeProperty() {
        return elapsedTime;
    }

    public ObservableList<String> getObservableErrorList() {
        return errorList;
    }

    public Boolean isEndedOnError() {
        return endedOnError;
    }

    public Boolean isHardShutdown() {
        return hardShutdown;
    }
}
