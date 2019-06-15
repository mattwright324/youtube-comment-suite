package io.mattw.youtube.commentsuite;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.util.concurrent.AbstractFuture;
import io.mattw.youtube.commentsuite.db.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressBar;
import io.mattw.youtube.commentsuite.util.ElapsedTime;
import io.mattw.youtube.commentsuite.util.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
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
 * @since 2018-12-30
 * @author mattwright324
 */
public class MGMVGroupRefresh extends Thread implements RefreshInterface {

    private Logger logger = LogManager.getLogger(getClass().getSimpleName());

    private Group group;
    private ObservableList<String> errorList = FXCollections.observableArrayList();
    private boolean softShutdown = false;
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
    private long commentThreads = 0;
    private boolean keepAlive = true;
    //private ExecutorService background = Executors.newCachedThreadPool();
    private ElapsedTime elapsedTimer = new ElapsedTime();
    private SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");

    private ExecutorService gitemService;
    private ExecutorService videoCommentsService;
    private List<Future<?>> videoCommentFutures = new ArrayList<>();
    private ExecutorService repliesService;
    private List<Future<?>> repliesFutures = new ArrayList<>();
    private ExecutorService commentInsertService;
    private List<Future<?>> commentInsertFutures = new ArrayList<>();
    private ExecutorService channelIdService;
    private List<Future<?>> channelIdFutures = new ArrayList<>();
    private ExecutorService channelInsertService;
    private List<Future<?>> channelInsertFutures = new ArrayList<>();

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
            startVideoParse(10);
            startBackgroundThreads();
            shutdown();
            videoCommentsService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            awaitBackgroundThreads();
            database.commit();

            updateProgress();
            runLater(() -> statusStep.setValue("Done"));
        } catch (Exception e) {
            try {
                database.commit();
            } catch (SQLException e2) {
                appendError("Failed to commit after refresh failure.");
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
        logger.debug(String.format("Refresh End [progress=%s,elapsedTime=%s,newVideos=%s,totalVideos=%s,newComments=%s," +
                "totalComments=%s,lastStep=%s]",
                progress.getValue(),
                elapsedTime.getValue(),
                newVideos.getValue(),
                totalVideos.getValue(),
                newComments.getValue(),
                totalComments.getValue(),
                statusStep.getValue()));
        logger.debug("YouTube API Est. Used Quota this YCS session");
    }

    /**
     * Consumes the GroupItems listed under the group, finds the videoId's associated, and places them in a queue.
     * @throws InterruptedException executors/thread was interrupted
     */
    private void startAndAwaitParsingGitems() throws InterruptedException {
        gitemService = Executors.newFixedThreadPool(1);
        gitemService.submit(() -> {
            logger.debug("Starting Gitem-Video Thread");
            while(!gitemQueue.isEmpty()) {
                GroupItem gitem = gitemQueue.poll();
                if(gitem != null) {
                    logger.debug(String.format("Grabbing Video for GroupItem[id=%s,type=%s,name=%s]",
                            gitem.getYoutubeId(), gitem.getTypeId(), gitem.getTitle()));
                    try {
                        database.updateGroupItemLastChecked(gitem);

                        if(gitem.getTypeId() == YType.CHANNEL || gitem.getTypeId() == YType.PLAYLIST) {
                            String playlistId = gitem.getYoutubeId();
                            if(gitem.getTypeId() == YType.CHANNEL) {
                                ChannelListResponse cl = youtube.channels().list("contentDetails")
                                        .setKey(FXMLSuite.getYouTubeApiKey())
                                        .setMaxResults(50L)
                                        .setId(gitem.getYoutubeId())
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
                                                gitem.getYoutubeId(), videoId))
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
                                        gitem.getYoutubeId(), gitem.getYoutubeId()));

                                videoIdQueue.put(gitem.getYoutubeId());
                                if(database.doesVideoExist(gitem.getYoutubeId())) {
                                    incrLongProperty(newVideos, 1);
                                }
                                incrLongProperty(totalVideos, 1);
                            } catch (InterruptedException ignored) {
                                ignored.printStackTrace();
                            }
                        }
                    } catch (SQLException | IOException e) {
                        appendError(String.format("Failed GItem[id=%s]", gitem.getYoutubeId()));

                        logger.error(e);
                    }
                }
            }
            logger.debug("Ending Gitem-Video Thread");
        });
        gitemService.shutdown();
        gitemService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * Consumes YouTubeVideo objects in the queue.
     * @param consumers number of consuming threads
     * @throws InterruptedException executors/thread was interrupted
     */
    private void startVideoParse(int consumers) throws InterruptedException {
        logger.debug(String.format("Starting Comment Grabbing [consumers=%s,videos=%s]", consumers, videoQueue.size()));
        videoCommentsService = Executors.newFixedThreadPool(consumers);
        for(int i=0; i < consumers; i++) {
            Future<?> future = videoCommentsService.submit(() -> {
                while(!videoQueue.isEmpty()) {
                    YouTubeVideo video = videoQueue.poll();
                    if(video != null) {
                        int attempts = 0;
                        CommentThreadListResponse ctl;
                        String pageToken = "";
                        do {
                            try {
                                logger.info(String.format("%s - %s", video.getYoutubeId(), video.getTitle()));
                                do {
                                    ctl = youtube.commentThreads().list("snippet")
                                            .setKey(FXMLSuite.getYouTubeApiKey())
                                            .setVideoId(video.getYoutubeId())
                                            .setMaxResults(50L)
                                            .setPageToken(pageToken)
                                            .execute();

                                    pageToken = ctl.getNextPageToken();

                                    if(!ctl.getItems().isEmpty()) {
                                        List<YouTubeComment> comments = ctl.getItems().stream()
                                                .map(YouTubeComment::new).collect(Collectors.toList());
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
                                                commentThreadQueue.add(new Tuple<>(c.getYoutubeId(), video.getYoutubeId()));
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
                                String message = String.format("[%s/%s] %s [videoId=%s]", attempts, 5,
                                        e.getClass().getSimpleName(), video.getYoutubeId());

                                attempts++;
                                appendError(message);
                                logger.warn(message, e);
                            }
                        } while(attempts < maxAttempts && !isHardShutdown());

                        incrVideoProgress();
                        updateProgress();
                    }
                    awaitMillis(500);
                }
                logger.debug(String.format("Video Consumer [END] [leftInQueue=%s]", videoQueue.size()));
            });
            videoCommentFutures.add(future);
        }
        videoCommentsService.shutdown();
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
        repliesService = Executors.newFixedThreadPool(20);
        for(int i=0; i < 20; i++) {
            Future<?> future = repliesService.submit(() -> {
                while(!commentThreadQueue.isEmpty() || !isAllCompleted(videoCommentFutures)) {
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
                                        .filter(yc -> !"".equals(yc.getChannelId()) /* Filter out G+ comments. */)
                                        .collect(Collectors.toList());
                                commentQueue.addAll(replies);

                                List<YouTubeChannel> channels = cl.getItems().stream()
                                        .filter(distinctByKey(Comment::getId))
                                        .map(item -> new YouTubeChannel(item, false))
                                        .collect(Collectors.toList());
                                channelInsertQueue.addAll(channels);

                                awaitMillis(50);
                            } while (pageToken != null && !isHardShutdown());
                        } catch (IOException e) {
                            logger.error(String.format("Couldn't grab commentThread[id=%s]", tuple.getFirst()), e);
                        }
                        incrVideoProgress();
                        updateProgress();
                    }
                    awaitMillis(100);

                    if(isHardShutdown()) break;
                }
                logger.debug(String.format("CommentThread Consumer [END] [leftInQueue=%s]", commentThreadQueue.size()));
            });
            repliesFutures.add(future);
        }

        logger.debug("Starting Comment/Reply Insert Threads");
        commentInsertService = Executors.newFixedThreadPool(2);
        for(int i=0; i < 2; i++) {
            Future<?> future = commentInsertService.submit(() -> {
                ElapsedTime elapsed = new ElapsedTime();
                elapsed.setNow();
                List<YouTubeComment> comments = new ArrayList<>();
                while(!commentQueue.isEmpty() || !isAllCompleted(videoCommentFutures) || !isAllCompleted(repliesFutures)) {
                    YouTubeComment comment = commentQueue.poll();
                    if(comment != null) {
                        comments.add(comment);
                    } else {
                        awaitMillis(5);
                    }

                    if(comments.size() >= 1000 || (elapsed.getElapsedMillis() >= 1200 && !comments.isEmpty())) {
                        try {
                            logger.debug(String.format("CommentConsumer[submitting=%s,leftInQueue=%s,inThreadQueue=%s]",
                                    comments.size(),
                                    commentQueue.size(),
                                    commentThreadQueue.size()));
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
                logger.debug(String.format("Comment Consumer [END] [leftInQueue=%s]", commentThreadQueue.size()));
            });
            commentInsertFutures.add(future);
        }

        logger.debug("Starting ChannelId Threads");
        channelIdService = Executors.newFixedThreadPool(20);
        for(int i=0; i < 20; i++) {
            Future<?> future = channelIdService.submit(() -> {
                ElapsedTime elapsed = new ElapsedTime();
                elapsed.setNow();

                Set<String> channelIds = new HashSet<>();
                while(!channelQueue.isEmpty() || !isAllCompleted(videoCommentFutures) || !isAllCompleted(repliesFutures)) {
                    String channel = channelQueue.poll();
                    if(channel != null) {
                        channelIds.add(channel);
                    } else {
                        awaitMillis(5);
                    }

                    if(channelIds.size() >= 50 || (elapsed.getElapsedMillis() >= 500 && !channelIds.isEmpty())) {
                        logger.debug(String.format("ChannelIdConsumer[submitting=%s,leftInQueue=%s]",
                                channelIds.size(),
                                channelQueue.size()));
                        try {
                            ChannelListResponse cl = youtube.channels().list("snippet")
                                    .setKey(FXMLSuite.getYouTubeApiKey())
                                    .setId(String.join(",", channelIds))
                                    .setMaxResults(50L)
                                    .setPageToken("")
                                    .execute();

                            List<YouTubeChannel> channels = cl.getItems().stream().map(YouTubeChannel::new)
                                    .collect(Collectors.toList());

                            channelInsertQueue.addAll(channels);

                            elapsed.setNow();
                            channelIds.clear();
                        } catch (IOException e) {
                            logger.error("Error on channel grab", e);
                        }
                    }

                    if(isHardShutdown()) break;
                }

                logger.debug(String.format("ChannelId Consumer [END] [leftInQueue=%s]", channelQueue.size()));
            });
            channelIdFutures.add(future);
        }

        logger.debug("Starting Channel Insert Threads");
        channelInsertService = Executors.newFixedThreadPool(2);
        for(int i=0; i < 2; i++) {
            Future<?> future = channelInsertService.submit(() -> {
                ElapsedTime elapsed = new ElapsedTime();
                elapsed.setNow();

                List<YouTubeChannel> insert = new ArrayList<>();
                while(!channelInsertQueue.isEmpty() || !isAllCompleted(channelIdFutures)) {
                    YouTubeChannel channel = channelInsertQueue.poll();
                    if(channel != null) {
                        insert.add(channel);
                    } else {
                        awaitMillis(5);
                    }

                    if(insert.size() >= 1000 || (elapsed.getElapsedMillis() >= 1200 && !insert.isEmpty())) {
                        try {
                            logger.debug(String.format("ChannelInsertConsumer[submitting=%s,leftInQueue=%s]",
                                    insert.size(),
                                    channelInsertQueue.size()));
                            database.insertChannels(insert);
                            elapsed.setNow();
                            insert.clear();
                        } catch (SQLException e) {
                            logger.error("Error on channel submit", e);
                        }
                    }

                    if(isHardShutdown()) break;
                }

                logger.debug(String.format("Channel Insert Consumer [END] [leftInQueue=%s]", channelInsertQueue.size()));
            });
            channelInsertFutures.add(future);
        }
    }

    /**
     * Await the background threads to finish before continuing.
     * @throws InterruptedException executors/thread was interrupted
     */
    private void awaitBackgroundThreads() throws InterruptedException {
        logger.debug("Awaiting background threads to close.");
        repliesService.shutdown();
        commentInsertService.shutdown();
        channelIdService.shutdown();
        channelInsertService.shutdown();

        repliesService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        commentInsertService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        channelIdService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        channelInsertService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
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
                runLater(() -> elapsedTime.setValue(elapsedTimer.getElapsedString()));
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
        logger.debug(String.format("Grabbing Video Data [ids=%s]", videoIds.toString()));
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
                .map(YouTubeComment::getYoutubeId).collect(Collectors.toList());
        final long notYetExisting = database.countCommentsNotExisting(commentIds);
        final long totalInserting = comments.size();
        final long currentTotal = totalComments.getValue();
        incrLongProperty(newComments, notYetExisting);
        setLongPropertyValue(totalComments, currentTotal+totalInserting);
        database.insertComments(comments);
        updateProgress();
    }

    private String getId(ResourceId resourceId) {
        if(resourceId.getVideoId() != null) {
            return resourceId.getVideoId();
        } else if(resourceId.getChannelId() != null) {
            return resourceId.getChannelId();
        } else if(resourceId.getPlaylistId() != null) {
            return resourceId.getPlaylistId();
        }
        return null;
    }

    private void awaitMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            // don't care if this gets interrupted
        }
    }

    private boolean isAllCompleted(List<Future<?>> futures) {
        for(Future<?> future : futures) {
            if(!future.isDone()) {
                return false;
            }
        }
        return true;
    }

    /**
     * This commentThreads value is used to subtract the duplicate-counted comment threads
     */
    private synchronized void incrCommentThreads() {
        commentThreads++;
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
     * Soft shutdown, signalled when main thread is done and it is okay for background
     * threads to die when their queues are empty.
     */
    public void shutdown() {
        logger.debug("Signalling Soft Shutdown");
        this.softShutdown = true;
    }

    /**
     * Hard (manual) shutdown, signalled by the user and abruptly ends the refresh as soon as it can.
     */
    public void hardShutdown() {
        logger.debug("Signalling Hard Shutdown");
        this.softShutdown = true;
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

    public Boolean isShutdown() {
        return softShutdown;
    }

    public Boolean isHardShutdown() {
        return hardShutdown;
    }
}
