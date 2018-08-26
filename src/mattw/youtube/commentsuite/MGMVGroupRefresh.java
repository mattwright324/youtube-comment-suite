package mattw.youtube.commentsuite;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressBar;
import mattw.youtube.commentsuite.db.*;
import mattw.youtube.commentsuite.io.ElapsedTime;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static javafx.application.Platform.runLater;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Refresh implementation for MGMVRefreshModal with bindable properties.
 *
 * Interacts heavily with YouTube API v3 and the CommentDatabase.
 *
 * @author mattwright324
 */
public class MGMVGroupRefresh extends Thread implements RefreshInterface {

    private Logger logger = LogManager.getLogger(getClass().getSimpleName());

    /**
     * Tuple to hold commentThreadId and videoId for consuming threads.
     *
     * TODO: True/False: videoId included w/ comment when grabbing commentThread children. Remove if False.
     *
     * @author mattwright324
     * @param <K>
     * @param <V>
     */
    private class Tuple<K,V> {
        public final K first;
        public final V second;
        Tuple(K first, V second) {
            this.first = first;
            this.second = second;
        }
        public K getFirst() {
            return first;
        }
        public V getSecond() {
            return second;
        }
    }

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
    private ExecutorService background = Executors.newCachedThreadPool();
    private ElapsedTime elapsedTimer = new ElapsedTime();
    private SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
    private LinkedBlockingQueue<GroupItem> gitemQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<String> videoIdQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<YouTubeVideo> videoQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Tuple<String,String>> commentThreadQueue = new LinkedBlockingQueue<>();

    private YouTubeData3 youtube = FXMLSuite.getYoutubeApi();
    private CommentDatabase database = FXMLSuite.getDatabase();

    public MGMVGroupRefresh(Group group) {
        this.group = group;
    }

    public void run() {
        runLater(() -> progress.setValue(ProgressBar.INDETERMINATE_PROGRESS));
        logger.debug("Refresh Start");
        elapsedTimer.setNow();
        startElapsedTimer();
        startBackgroundThreads();
        try {
            gitemQueue.addAll(database.getGroupItems(group));

            runLater(() -> statusStep.setValue("Grabbing Videos"));
            startParsingGitems();

            if(isShutdown()) {
                throw new InterruptedException("Manually stopped.");
            }

            runLater(() -> statusStep.setValue("Grabbing Video Data"));
            totalProgress += videoIdQueue.size();
            updateProgress();
            List<String> videoIds = new ArrayList<>();
            while(!videoIdQueue.isEmpty()  && !isShutdown()) {
                videoIds.add(videoIdQueue.poll());
                if(videoIds.size() == 50) {
                    parseVideoIdsToObjects(videoIds);
                    videoIds.clear();
                }
            }
            parseVideoIdsToObjects(videoIds);
            database.commit();

            if(isShutdown()) {
                throw new InterruptedException("Manually stopped.");
            }

            videoProgress = 0.0;
            updateProgress();

            runLater(() -> {
                statusStep.setValue("Grabbing Comments");
                progress.setValue(0.0);
            });
            startAndAwaitVideoParse(10);
            shutdown();
            awaitBackgroundThreads();
            database.commit();

            updateProgress();
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
        logger.debug("Refresh Finished");
    }

    private void startParsingGitems() {
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
                            ChannelsList cl = youtube.channelsList().getByChannel(ChannelsList.PART_CONTENT_DETAILS, gitem.getYoutubeId(), "");
                            if(cl.hasItems() && cl.items[0].hasContentDetails()) {
                                playlistId = cl.items[0].contentDetails.relatedPlaylists.uploads;
                            }
                        }

                        PlaylistItemsList pil;
                        String pageToken = "";
                        do {
                            pil = youtube.playlistItemsList().get(PlaylistItemsList.PART_SNIPPET,
                                    playlistId, pageToken);
                            pageToken = pil.nextPageToken;
                            if(pil.hasItems()) {
                                List<String> videoIds = Arrays.stream(pil.items)
                                        .map(item -> item.snippet.resourceId.videoId)
                                        .collect(Collectors.toList());
                                videoIdQueue.addAll(videoIds);
                                incrLongProperty(newVideos, database.countVideosNotExisting(videoIds));
                                incrLongProperty(totalVideos, pil.items.length);
                            }
                        } while(pil.nextPageToken != null  && !isShutdown());
                    } else if(gitem.getTypeId() == YType.VIDEO) {
                        try {
                            videoIdQueue.put(gitem.getYoutubeId());
                            if(database.doesVideoExist(gitem.getYoutubeId())) {
                                incrLongProperty(newVideos, 1);
                            }
                            incrLongProperty(totalVideos, 1);
                        } catch (InterruptedException ignored) {}
                    }
                } catch (SQLException | IOException | YouTubeErrorException e) {
                    appendError(String.format("Failed GItem[id=%s]", gitem.getYoutubeId()));
                    logger.error(e);
                }
            }
        }
    }

    /**
     * Background threads:
     * - Comment Thread Consumers
     */
    private void startBackgroundThreads() {
        logger.debug("Starting Comment Thread Consumers");
        for(int i=0; i < 10; i++) {
            background.execute(() -> {
                while(!commentThreadQueue.isEmpty() || !isShutdown()) {
                    Tuple<String,String> tuple = commentThreadQueue.poll();
                    if(tuple != null) {
                        try {
                            CommentsList cl;
                            String pageToken = "";
                            do {
                                cl = youtube.commentsList()
                                        .getByParentId(CommentsList.PART_SNIPPET, tuple.getFirst(), pageToken);
                                pageToken = cl.nextPageToken;

                                List<YouTubeComment> replies = Arrays.stream(cl.items)
                                        .map(item -> new YouTubeComment(item, tuple.getSecond()))
                                        .filter(yc -> !"".equals(yc.getChannelId()) /* Filter out G+ comments. */)
                                        .collect(Collectors.toList());
                                submitComments(replies);
                            } while (pageToken != null && !isShutdown());
                        } catch (YouTubeErrorException | IOException | SQLException e) {
                            logger.error(String.format("Couldn't grab commentThread[id=%s]", tuple.getFirst()), e);
                        }
                        incrVideoProgress();
                        updateProgress();
                    }
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}

                    if(isHardShutdown()) break;
                }
                logger.debug("Comment Thread Consumer Ended ");
            });
        }
    }

    /**
     * Await the background threads to finish before continuing.
     * @throws InterruptedException
     */
    private void awaitBackgroundThreads() throws InterruptedException {
        logger.debug("Awaiting background threads to close.");
        background.shutdown();
        background.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void startAndAwaitVideoParse(int consumers) throws InterruptedException {
        logger.debug(String.format("Starting Comment Grabbing [consumers=%s]", consumers));
        ExecutorService es = Executors.newCachedThreadPool();
        for(int i=0; i < consumers; i++) {
            es.execute(() -> {
                while(!videoQueue.isEmpty() && !isShutdown()) {
                    YouTubeVideo video = videoQueue.poll();
                    int attempts = 0;
                    CommentThreadsList ctl;
                    String pageToken = "";
                    do {
                        try {
                            logger.info(String.format("%s - %s", video.getYoutubeId(), video.getTitle()));
                            do {
                                ctl = youtube.commentThreadsList()
                                        .getThreadsByVideo(CommentThreadsList.PART_SNIPPET, video.getYoutubeId(), pageToken);
                                pageToken = ctl.nextPageToken;

                                if(ctl.hasItems()) {
                                    List<YouTubeComment> comments = Arrays.stream(ctl.items)
                                            .map(YouTubeComment::new).collect(Collectors.toList());
                                    comments.stream().forEach(c -> {
                                        if(c.getReplyCount() > 0) {
                                            incrTotalProgress(1);
                                            updateProgress();
                                            commentThreadQueue.add(new Tuple<>(c.getYoutubeId(), video.getYoutubeId()));
                                        }
                                    });
                                    submitComments(comments);
                                }
                            } while (pageToken != null && !isShutdown());
                            break;
                        } catch (SQLException | YouTubeErrorException | IOException e) {
                            String message;
                            if(e instanceof YouTubeErrorException) {
                                YouTubeErrorException yee = (YouTubeErrorException) e;
                                int responseCode = yee.getError().code;
                                if(responseCode == 400) {
                                    // Either there is a slow connection or we hit the YouTube API quota limit.
                                    message = String.format("[%s/%s] HTTP400 [videoId=%s]", attempts, 5, video.getYoutubeId());
                                    attempts++;
                                    appendError(message);
                                    logger.warn(message, e);
                                } else if(responseCode == 403) {
                                    // Comments were disabled, update DB, move on.
                                    message = String.format("Comments Disabled [videoId=%s]", video.getYoutubeId());
                                    try {
                                        database.updateVideoHttpCode(video.getYoutubeId(), 403);
                                    } catch (SQLException e1) { logger.error(e1); }
                                    appendError(message);
                                    logger.warn(message, e);
                                    break;
                                } else if(responseCode == 404) {
                                    // Bad videoId, move on to the next one.
                                    message = String.format("HTTP404 Not Found [videoId=%s]", video.getYoutubeId());
                                    appendError(message);
                                    logger.warn(message, e);
                                    break;
                                }
                            } else if(e instanceof IOException){
                                // Some other connection error, try again?
                                message = String.format("[%s/%s] %s [videoId=%s]", attempts, 5,
                                        e.getClass().getSimpleName(), video.getYoutubeId());
                                attempts++;
                                appendError(message);
                                logger.warn(message, e);
                            } else {
                                // SQLException, something really bad happened, move on.
                                message = e.getLocalizedMessage();
                                appendError(message);
                                logger.warn(message, e);
                                break;
                            }
                        }
                    } while(attempts < maxAttempts);

                    incrVideoProgress();
                    updateProgress();
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            });
        }
        es.shutdown();
        es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void startElapsedTimer() {
        ExecutorService es = Executors.newFixedThreadPool(1);
        es.execute(() -> {
            logger.debug("Starting Elapsed Timer");
            while(!ended.getValue() && !isShutdown()) {
                runLater(() -> elapsedTime.setValue(elapsedTimer.getElapsedString()));
                try { Thread.sleep(27); } catch (InterruptedException ignored) {}
            }
            logger.debug("Ended Elapsed Timer");
        });
        es.shutdown();
    }

    private void parseVideoIdsToObjects(List<String> videoIds) throws IOException, YouTubeErrorException, SQLException {
        logger.debug(String.format("Grabbing Video Data [ids=%s]", videoIds.toString()));
        VideosList vl = youtube.videosList().getByIds(VideosList.PART_SNIPPET+","+VideosList.PART_STATISTICS,
                videoIds.stream().collect(Collectors.joining(",")), "");
        if(vl.items != null) {
            List<YouTubeVideo> videos = Arrays.stream(vl.items)
                    .map(YouTubeVideo::new).collect(Collectors.toList());
            videoQueue.addAll(videos);
            database.insertVideos(videos);
        }
        incrVideoProgress(videoIds.size());
        updateProgress();
    }

    private void submitComments(List<YouTubeComment> comments) throws SQLException {
        List<String> commentIds = comments.stream()
                .map(YouTubeComment::getYoutubeId).collect(Collectors.toList());
        incrLongProperty(newComments, database.countCommentsNotExisting(commentIds));
        incrLongProperty(totalComments, comments.size());
        database.insertComments(comments);
        updateProgress();
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

    public void shutdown() {
        this.softShutdown = true;
    }

    public void hardShutdown() {
        this.shutdown();
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
