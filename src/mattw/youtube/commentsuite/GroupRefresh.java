package mattw.youtube.commentsuite;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.beans.property.*;
import mattw.youtube.commentsuite.db.*;
import mattw.youtube.commentsuite.io.ElapsedTime;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.YouTubeErrorException;
import mattw.youtube.datav3.resources.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class GroupRefresh extends Thread {

    private Group group;
    private CommentDatabase database;
    private YouTubeData3 youtube;
    private ElapsedTime elapsedTime = new ElapsedTime();

    private SimpleStringProperty refreshStatus = new SimpleStringProperty("Preparing");
    private SimpleStringProperty elapsedTimeValue = new SimpleStringProperty("");
    private SimpleBooleanProperty refreshing = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty completed = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty failed = new SimpleBooleanProperty(false);
    private SimpleDoubleProperty progress = new SimpleDoubleProperty(-1);
    private AtomicLong commentInsertCount = new AtomicLong(0);
    private SimpleIntegerProperty commentsNew = new SimpleIntegerProperty(0);
    private long videoInsertCount = 0;
    private SimpleIntegerProperty videosNew = new SimpleIntegerProperty(0);
    private AtomicLong errorCounter = new AtomicLong(0);
    private SimpleIntegerProperty errorCount = new SimpleIntegerProperty(0);

    private Map<String, String> threadToVideo = new HashMap<>();
    private Map<String, Integer> threadToReplies;
    private Queue<String> commentThreadQueue = new ConcurrentLinkedQueue<>();
    private Queue<String> videosQueue = new ConcurrentLinkedQueue<>();
    private boolean threadLive = true, threadMayDie = false, videoLive = true, videoMayDie = false;
    private double totalVideos = 0;
    private AtomicInteger consumedVideos = new AtomicInteger(0);
    private AtomicInteger totalThreads = new AtomicInteger(0);
    private AtomicInteger consumedThreads = new AtomicInteger(0);

    private Set<String> existingVideoIds = new HashSet<>();
    private Set<String> existingCommentIds = new HashSet<>();
    private Set<String> existingChannelIds = new HashSet<>();
    private List<GroupItem> existingGroupItems = new ArrayList<>();
    private List<CommentDatabase.GroupItemVideo> existingGIV = new ArrayList<>();

    private List<YouTubeVideo> videoInsert = new ArrayList<>();
    private List<YouTubeVideo> videoUpdate = new ArrayList<>();
    private List<YouTubeChannel> channelInsert = new ArrayList<>();
    private List<YouTubeChannel> channelUpdate = new ArrayList<>();
    private List<CommentDatabase.GroupItemVideo> givInsert = new ArrayList<>();

    public GroupRefresh(Group group, CommentDatabase db, YouTubeData3 data) {
        this.group = group;
        this.database = db;
        this.youtube = data;
    }

    private boolean listHasId(List<? extends YouTubeObject> list, String id) {
        return list.stream().anyMatch(yo -> yo.getYouTubeId().equals(id));
    }

    private void clearLists(Collection... lists) {
        for(Collection list : lists) { list.clear(); }
    }

    public void run() {
        try {
            Platform.runLater(() -> refreshing.setValue(true));
            elapsedTime.set();

            ExecutorService es = Executors.newFixedThreadPool(1);
            es.execute(() -> {
                while(!completed.getValue()) {
                    Platform.runLater(() -> {
                        elapsedTimeValue.setValue(elapsedTime.getTimeString());
                        if(errorCounter.get() > 0) {
                            errorCount.setValue(errorCounter.get());
                        }
                        long count = videoInsert.size();
                        if(count > videoInsertCount) {
                            videosNew.setValue(videoInsertCount = count);
                        }
                        if(progress.getValue() != -1) {
                            commentsNew.setValue(commentInsertCount.get());
                        }
                    });
                    try { Thread.sleep(121); } catch (Exception ignored) {}
                }
                System.out.println("ELAPSED TIME STOPPED");
            });
            es.shutdown();

            existingVideoIds.addAll(database.getAllVideoIds());
            existingChannelIds.addAll(database.getAllChannelIds());
            existingGroupItems.addAll(database.getGroupItems(group));
            existingGIV.addAll(database.getAllGroupItemVideo());

            List<GroupItem> videoItems = existingGroupItems.stream().filter(gi -> gi.getTypeId() == GroupItem.VIDEO).collect(Collectors.toList());
            List<GroupItem> playlistItems = existingGroupItems.stream().filter(gi -> gi.getTypeId() == GroupItem.PLAYLIST).collect(Collectors.toList());
            List<GroupItem> channelItems = existingGroupItems.stream().filter(gi -> gi.getTypeId() == GroupItem.CHANNEL).collect(Collectors.toList());

            System.out.println(String.format("VideoItems %s, PlaylistItems %s, ChannelItems %s", videoItems.size(), playlistItems.size(), channelItems.size()));

            Platform.runLater(() -> refreshStatus.setValue("Grabbing Videos"));
            parseGroupItems(videoItems, GroupItem.VIDEO);
            parseGroupItems(playlistItems, GroupItem.PLAYLIST);
            parseGroupItems(channelItems, GroupItem.CHANNEL);
            database.insertVideos(videoInsert);
            database.updateVideos(videoUpdate);
            database.insertGroupItemVideo(givInsert);
            database.commit();
            videoInsertCount = videoInsert.size();
            clearLists(existingVideoIds, existingGIV, videoInsert, videoUpdate, givInsert, videoItems, playlistItems, channelItems);

            Platform.runLater(() -> {
                refreshStatus.setValue("Grabbing Comments");
                progress.setValue(0);
            });
            existingCommentIds.addAll(database.getCommentIds(group));
            threadToReplies = database.getCommentThreadReplyCounts(group);
            videosQueue.addAll(database.getVideoIds(group));
            totalVideos = videosQueue.size();
            System.out.println("VIDEO QUEUE "+videosQueue.size());
            ExecutorService ces = Executors.newCachedThreadPool();
            int commentThreadCount = 20;
            for(int i = 0; i< commentThreadCount; i++) {
                // Consumes from commentThreadQueue
                ces.execute(() -> {
                    while(!commentThreadQueue.isEmpty() || threadLive) {
                        if(threadMayDie && threadLive) threadLive = false;
                        try {
                            final String threadId = commentThreadQueue.poll();
                            Thread.sleep(20);
                            if(threadId != null) {
                                handleCommentThread(threadId, threadToVideo.get(threadId));
                                Platform.runLater(() -> progress.setValue((consumedThreads.addAndGet(1)+consumedVideos.get()) / (totalVideos+totalThreads.get())));
                            }
                        } catch (Exception ignored) {}
                    }
                });
            }
            int videoThreadCount = 10;
            ExecutorService ves = Executors.newFixedThreadPool(videoThreadCount);
            for(int i = 0; i< videoThreadCount; i++) {
                // Consumes from videoQueue and produces for commentThreadQueue
                ves.execute(() -> {
                    while (!videosQueue.isEmpty() || videoLive) {
                        if (videoMayDie && videoLive) videoLive = false;
                        try {
                            final String videoId = videosQueue.poll();
                            if (videoId != null) {
                                handleVideo(videoId);
                                Platform.runLater(() -> progress.setValue((consumedThreads.get()+consumedVideos.addAndGet(1)) / (totalVideos+totalThreads.get())));
                            }
                            Thread.sleep(100);
                        } catch (Exception ignored) {}
                    }
                });
            }

            videoMayDie = true;
            ves.shutdown();
            ves.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            System.out.println("Video Threads Done");
            threadMayDie = true;
            ces.shutdown();
            ces.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            System.out.println("Comment Threads Done");

            Platform.runLater(() -> refreshStatus.setValue("Finishing up"));
            database.insertChannels(channelInsert);
            database.updateChannels(channelUpdate);
            database.commit();
            Platform.runLater(() -> {
                refreshStatus.setValue("Completed");
                refreshing.setValue(false);
                completed.setValue(true);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                failed.setValue(true);
                refreshStatus.setValue(e.getLocalizedMessage());
                refreshing.setValue(false);
                completed.setValue(true);
            });
            errorCounter.addAndGet(1);
            e.printStackTrace();
        }
    }

    /**
     * Handles GroupItems separated by the same type.
     */
    private void parseGroupItems(List<GroupItem> items, int type) throws SQLException, IOException, YouTubeErrorException {
        if(type == GroupItem.VIDEO) {
            List<String> sublist;
            for(GroupItem item : items) {
                database.updateGroupItemLastChecked(item);
            }
            for(int i=0; i<items.size(); i+=50) {
                sublist = items.subList(i, i+50 < items.size() ? i+50 : items.size())
                        .stream().map(YouTubeObject::getYouTubeId).collect(Collectors.toList());
                System.out.println("GroupItem Videos: "+sublist.toString());
                handleVideos(sublist, "");
            }
        } else if(type == GroupItem.PLAYLIST) {
            for(GroupItem item : items) {
                database.updateGroupItemLastChecked(item);
                System.out.println("GroupItem Playlist: "+item.getTitle());
                handlePlaylist(item.getYouTubeId(), item.getYouTubeId());
            }
        } else if(type == GroupItem.CHANNEL) {
            for(GroupItem item : items) {
                database.updateGroupItemLastChecked(item);
                System.out.println("GroupItem Channel: "+item.getTitle());
                ChannelsList cl = youtube.channelsList().getByChannel(ChannelsList.PART_CONTENT_DETAILS, item.getYouTubeId(), "");
                if(cl.hasItems() && cl.items[0].hasContentDetails()) {
                    String uploadPlaylistId = cl.items[0].contentDetails.relatedPlaylists.uploads;
                    handlePlaylist(uploadPlaylistId, item.getYouTubeId());
                }
            }
        }
    }

    /**
     * Grabs videos from playlists.
     */
    private void handlePlaylist(String playlistId, String gitemId) throws IOException, YouTubeErrorException {
        PlaylistItemsList pil;
        String pageToken = "";
        List<String> videoIds = new ArrayList<>();
        do {
            System.out.println("Playlist: "+playlistId+", Page ["+pageToken+"]");
            pil = youtube.playlistItemsList().get(PlaylistItemsList.PART_SNIPPET, playlistId, pageToken);
            pageToken = pil.nextPageToken;
            for(PlaylistItemsList.Item item : pil.items) {
                if(item.hasSnippet()) {
                    videoIds.add(item.snippet.resourceId.videoId);
                }
            }
        } while (pil.nextPageToken != null);
        for(int i=0; i<videoIds.size(); i+=50) {
            handleVideos(videoIds.subList(i, i+50 < videoIds.size() ? i+50 : videoIds.size()), gitemId);
        }
    }

    /**
     * Grabs video information at a max of 50 at a time.
     */
    private void handleVideos(List<String> videoIds, String gitemId) throws IOException, YouTubeErrorException {
        String ids = videoIds.stream().collect(Collectors.joining(","));
        VideosList vlSnippet = youtube.videosList().getByIds(VideosList.PART_SNIPPET, ids, "");
        VideosList vlStats = youtube.videosList().getByIds(VideosList.PART_STATISTICS, ids, "");
        for(int i=0; i<vlSnippet.items.length; i++) {
            YouTubeVideo video = new YouTubeVideo(vlSnippet.items[i], vlStats.items[i]);
            checkChannel(video.getChannelId(), null);
            CommentDatabase.GroupItemVideo giv = new CommentDatabase.GroupItemVideo(gitemId.equals("") ? video.getYouTubeId() : gitemId, video.getYouTubeId());
            if(!existingGIV.contains(giv)) { givInsert.add(giv); }
            if(!(existingVideoIds.contains(video.getYouTubeId()) || listHasId(videoInsert, video.getYouTubeId()) || listHasId(videoUpdate, video.getYouTubeId()))) {
                videoInsert.add(video);
                System.out.format("INSERT %s: %s\r\n", video.getYouTubeId(), video.getTitle());
            } else if(existingVideoIds.contains(video.getYouTubeId()) && !listHasId(videoUpdate, video.getYouTubeId())) {
                videoUpdate.add(video);
                System.out.format("UPDATE %s: %s\r\n", video.getYouTubeId(), video.getTitle());
            }
        }
    }

    /**
     * Grabs the *comments* from the specified video.
     * Automatically inserts comments into the database.
     * Does not commit.
     */
    private void handleVideo(String videoId) throws SQLException {
        List<YouTubeComment> comments = new ArrayList<>();
        CommentThreadsList ctl = null;
        String pageToken = "";
        int fails = 0;
        int page = 0;
        do {
            if(comments.size() > 500) {
                insertComments(comments);
            }
            try {
                ctl = youtube.commentThreadsList()
                        .order(CommentThreadsList.ORDER_TIME)
                        .getThreadsByVideo(CommentThreadsList.PART_SNIPPET, videoId, URLEncoder.encode(pageToken, "UTF-8"));
                pageToken = ctl.nextPageToken;
                page++;
                for(CommentThreadsList.Item item: ctl.items) {
                    if(item.hasSnippet()) {
                        String threadId = item.snippet.topLevelComment.getId();
                        boolean contains = threadToReplies.containsKey(threadId);
                        if((!contains && item.snippet.totalReplyCount > 0) || (contains && item.snippet.totalReplyCount != threadToReplies.get(item.snippet.topLevelComment.getId()))) {
                            threadToVideo.put(threadId, videoId);
                            commentThreadQueue.offer(threadId);
                            totalThreads.addAndGet(1);
                        }
                        if(!existingCommentIds.contains(threadId)) {
                            YouTubeComment comment = new YouTubeComment(item);
                            if(!"".equals(comment.getChannelId())) {
                                checkChannel(comment.getChannelId(), item.snippet.topLevelComment);
                                comments.add(comment);
                            } else {
                                System.out.println("G+ Comment: "+new Gson().toJson(item));
                            }
                        }
                    }
                }
            } catch (YouTubeErrorException yee) {
                fails++;
                try {
                    switch(yee.getError().code){
                        case 400:
                            System.err.println(String.format("BAD_REQ_HTTP_400 #%s: PAGE[%s] HTTPS[%s] URL[%s]", fails, page, youtube.getUseHttps(), yee.getRequestUrl()));
                            errorCounter.addAndGet(1);
                            Thread.sleep(5000);
                            break;
                        case 403:
                            System.err.println("Comments Disabled (403): http://youtu.be/"+videoId);
                            database.updateVideoHttpCode(videoId, yee.getError().code);
                            fails = 10;
                            break;
                        case 404:
                            System.err.println("Not found (404): http://youtu.be/"+videoId);
                            errorCounter.addAndGet(1);
                            database.updateVideoHttpCode(videoId, yee.getError().code);
                            fails = 10;
                            break;
                        default:
                            System.err.println("Unknown Error ("+yee.getError().code+"): http://youtu.be/"+videoId);
                            errorCounter.addAndGet(1);
                            database.updateVideoHttpCode(videoId, yee.getError().code);
                            fails = 10;
                            break;
                    }
                } catch (Exception e1) {
                    errorCounter.addAndGet(1);
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                errorCounter.addAndGet(1);
                fails++;
            }
        } while((ctl == null || ctl.nextPageToken != null) && fails < 5);
        if(fails == 5) {
            errorCounter.addAndGet(1);
            database.updateVideoHttpCode(videoId, 400);
        }
        if(comments.size() > 0) {
            insertComments(comments);
        }
        System.out.println("COMPLETED VIDEO "+videoId);
    }

    /**
     * Grabs the *replies* from a comment thread.
     * Automatically inserts replies into the database.
     * Does not commit.
     */
    private void handleCommentThread(String commentThreadId, String videoId) throws SQLException {
        List<YouTubeComment> comments = new ArrayList<>();
        CommentsList cl = null;
        String pageToken = "";
        int fails = 0;
        do {
            if(comments.size() > 500) {
                insertComments(comments);
            }
            try {
                cl = youtube.commentsList().getByParentId(CommentsList.PART_SNIPPET, commentThreadId, pageToken);
                pageToken = cl.nextPageToken;
                for(CommentsList.Item item : cl.items) {
                    if(item.hasSnippet()) {
                        if(!existingCommentIds.contains(item.getId())) {
                            YouTubeComment reply = new YouTubeComment(item, videoId);
                            if(!"".equals(reply.getChannelId())) {
                                checkChannel(reply.getChannelId(), item);
                                comments.add(reply);
                            } else {
                                System.out.println("G+ Comment: "+item.snippet.authorChannelUrl);
                            }

                        }
                    }
                }
            } catch (Exception e) {
                fails++;
                errorCounter.addAndGet(1);
                System.err.println("CommentThread ["+commentThreadId+"]: "+e.getLocalizedMessage());
            }
        } while((cl == null || cl.nextPageToken != null) && fails < 5);
        if(comments.size() > 0) {
            insertComments(comments);
        }
    }

    /**
     * Checks the channel of videos and comments to see if it already exists.
     * Grabs channel data if it is a video (CommentsList.Item item is null).
     */
    private void checkChannel(String channelId, CommentsList.Item item) {
        if(channelId != null) {
            if(!existingChannelIds.contains(channelId)) {
                YouTubeChannel channel = null;
                if(item != null) {
                    channel = new YouTubeChannel(item, false);
                } else {
                    try {
                        ChannelsList cl = youtube.channelsList().getByChannel(ChannelsList.PART_SNIPPET, channelId, "");
                        channel = new YouTubeChannel(cl.items[0], true); // Video authors display thumbs.
                    } catch (Exception ignored) {
                        errorCounter.addAndGet(1);
                    }
                }
                if(channel != null) {
                    if(!existingChannelIds.contains(channelId)) {
                        channelInsert.add(channel);
                        existingChannelIds.add(channelId);
                    } else if(!channelUpdate.contains(channel)) {
                        channelUpdate.add(channel);
                        existingChannelIds.add(channelId);
                    } else {
                        System.out.println("IGNORE CHANNEL "+channelId);
                    }
                } else {
                    System.err.println("Check Channel Null ["+channelId+"]");
                }
            }
        }
    }

    /**
     * Inserts the comments into the database and clears the list.
     */
    private void insertComments(List<YouTubeComment> items) throws SQLException {
        if(items.size() > 0) {
            database.insertComments(items);
            commentInsertCount.addAndGet(items.size());
            items.clear();
        }
    }

    public SimpleStringProperty refreshStatusProperty() { return refreshStatus; }
    public SimpleBooleanProperty refreshingProperty() { return refreshing; }
    public SimpleBooleanProperty completedProperty() { return completed; }
    public SimpleBooleanProperty failedProperty() { return failed; }
    public SimpleStringProperty elapsedTimeValueProperty() { return elapsedTimeValue; }
    public SimpleDoubleProperty progressProperty() { return progress; }
    public SimpleIntegerProperty commentsNewProperty() { return commentsNew; }
    public SimpleIntegerProperty videosNewProperty() { return videosNew; }
    public SimpleIntegerProperty errorCountProperty() { return errorCount; }
}
