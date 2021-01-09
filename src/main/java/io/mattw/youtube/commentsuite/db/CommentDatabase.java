package io.mattw.youtube.commentsuite.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteConnection;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static javafx.application.Platform.runLater;

/**
 * @author mattwright324
 */
public class CommentDatabase implements Closeable {

    private static final Logger logger = LogManager.getLogger();

    private ObservableList<Group> globalGroupList = FXCollections.observableArrayList();
    private LongProperty groupRename = new SimpleLongProperty(0);

    private Connection sqlite;
    private Group defaultGroup = new Group("28da132f5f5b48d881264d892aba790a", "Default");
    private Cache<String, YouTubeChannel> channelCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    /**
     * Default constructor for testing.
     */
    protected CommentDatabase(SQLiteConnection sqlite) {
        this.sqlite = sqlite;
    }

    /**
     * Actual constructor.
     *
     * @param fileName name of database file
     * @throws SQLException failed to create database
     */
    public CommentDatabase(String fileName) throws SQLException {
        logger.debug("Initialize Database [file={}]", fileName);
        sqlite = DriverManager.getConnection(String.format("jdbc:sqlite:%s", fileName));
        sqlite.setAutoCommit(false);
        this.create();
    }

    public ObservableList<Group> getGlobalGroupList() {
        return globalGroupList;
    }

    public LongProperty groupRenameProperty() {
        return groupRename;
    }

    public void incrGroupRenameProperty() {
        groupRename.setValue(groupRename.getValue() + 1);
    }

    @Override
    public void close() throws IOException {
        try {
            sqlite.close();
        } catch (SQLException e) {
            throw new IOException("Unable to close SQLite database connection.");
        }
    }

    public Connection getConnection() {
        return sqlite;
    }

    public void commit() throws SQLException {
        logger.debug("Committing.");
        sqlite.commit();
    }

    protected void create() throws SQLException {
        logger.debug("Creating tables if not exists.");
        try (Statement s = sqlite.createStatement()) {
            s.executeUpdate(SQLLoader.CREATE_DB.toString());
        }
        this.commit();
    }

    public void reset() throws SQLException {
        logger.warn("Dropping all database contents. This cannot be undone.");
        try (Statement s = sqlite.createStatement()) {
            s.executeUpdate(SQLLoader.RESET_DB.toString());
        }
        this.commit();
        this.vacuum();
        this.create();
    }

    public void vacuum() throws SQLException {
        logger.warn("Vacuuming database. This may take a long time.");
        sqlite.setAutoCommit(true);
        try (Statement s = sqlite.createStatement()) {
            s.execute(SQLLoader.VACUUM_DB.toString());
        }
        sqlite.setAutoCommit(false);
    }

    public void cleanUp() throws SQLException {
        logger.warn("Cleaning database of unlinked content.");
        try (Statement s = sqlite.createStatement()) {
            s.executeUpdate(SQLLoader.CLEAN_DB.toString());
        }
        commit();
    }

    /**
     * Checks if the channel is cached and caches it if not.
     */
    void checkChannel(ResultSet rs) throws SQLException {
        channelCache.put(rs.getString("channel_id"), resultSetToChannel(rs));
    }

    /**
     * Refreshes the globalGroupList.
     */
    public void refreshGroups() throws SQLException {
        logger.debug("Grabbing groups and refreshing global group list.");
        try (final Statement s = sqlite.createStatement();
             final ResultSet rs = s.executeQuery(SQLLoader.GET_ALL_GROUPS.toString())) {

            final List<Group> groups = new ArrayList<>();
            while (rs.next()) {
                final Group group = resultSetToGroup(rs);
                group.reloadGroupItems();
                groups.add(group);
            }

            logger.debug(globalGroupList);

            for (int i = 0; i < globalGroupList.size(); i++) {
                if (!groups.contains(globalGroupList.get(i))) {
                    final int j = i;
                    runLater(() -> globalGroupList.remove(j));
                }
            }
            for (Group g : groups) {
                if (!globalGroupList.contains(g)) {
                    runLater(() -> globalGroupList.add(g));
                }
            }

            if (globalGroupList.isEmpty()) {
                System.out.println("INSERTING Default Group");
                s.executeUpdate(SQLLoader.GROUP_CREATE_DEFAULT.toString());
                commit();
                runLater(() -> globalGroupList.add(defaultGroup));
            }
        }
    }

    private Group resultSetToGroup(ResultSet rs) throws SQLException {
        return new Group(rs.getString("group_id"), rs.getString("group_name"));
    }

    private GroupItem resultSetToGroupItem(ResultSet rs) throws SQLException {
        return new GroupItem(rs.getString("gitem_id"),
                YType.values()[rs.getInt("type_id") + 1],
                rs.getString("title"),
                rs.getString("channel_title"),
                rs.getString("thumb_url"),
                rs.getLong("published"),
                rs.getLong("last_checked"));
    }

    private GroupItemVideo resultSetToGroupItemVideo(ResultSet rs) throws SQLException {
        return new GroupItemVideo(rs.getString("gitem_id"), rs.getString("video_id"));
    }

    private YouTubeChannel resultSetToChannel(ResultSet rs) throws SQLException {
        return new YouTubeChannel(rs.getString("channel_id"),
                rs.getString("channel_name"),
                rs.getString("channel_profile_url"));
    }

    public YouTubeComment resultSetToComment(ResultSet rs) throws SQLException {
        return new YouTubeComment(rs.getString("comment_id"),
                rs.getString("comment_text"),
                rs.getLong("comment_date"),
                rs.getString("video_id"),
                rs.getString("channel_id"),
                rs.getInt("comment_likes"),
                rs.getInt("reply_count"),
                rs.getBoolean("is_reply"),
                rs.getString("parent_id"));
    }

    private YouTubeVideo resultSetToVideo(ResultSet rs) throws SQLException {
        return new YouTubeVideo(rs.getString("video_id"),
                rs.getString("channel_id"),
                rs.getString("video_title"),
                rs.getString("video_desc"),
                rs.getString("thumb_url"),
                rs.getLong("publish_date"),
                rs.getLong("grab_date"),
                rs.getLong("total_comments"),
                rs.getLong("total_likes"),
                rs.getLong("total_dislikes"),
                rs.getLong("total_views"),
                rs.getInt("http_code"));
    }

    public boolean doesChannelExist(String channelId) {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.DOES_CHANNEL_EXIST.toString())) {
            ps.setString(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public long countChannelsNotExisting(Collection<String> channelIds) {
        long notExists = 0;
        for (String id : channelIds) {
            if (!doesChannelExist(id)) {
                notExists++;
            }
        }
        return notExists;
    }

    public Collection<String> findChannelsNotExisting(Collection<String> channelIds) {
        Set<String> idList = new HashSet<>();

        for (String id : channelIds) {
            if(!doesChannelExist(id)) {
                idList.add(id);
            }
        }

        return idList;
    }

    public boolean doesVideoExist(String videoId) {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.DOES_VIDEO_EXIST.toString())) {
            ps.setString(1, videoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error(e);
            e.printStackTrace();
        }
        return false;
    }

    public long countVideosNotExisting(Collection<String> videoIds) {
        long notExists = 0;
        for (String id : videoIds) {
            if (!doesVideoExist(id)) {
                notExists++;
            }
        }
        return notExists;
    }

    public boolean doesCommentExist(String commentId) {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.DOES_COMMENT_EXIST.toString())) {
            ps.setString(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error(e);
            e.printStackTrace();
        }
        return false;
    }

    public long countCommentsNotExisting(Collection<String> commentIds) {
        // logger.trace("Checking if commentIds's exist [size={},ids={}]", commentIds.size(), commentIds.toString());
        long notExists = 0;
        for (String id : commentIds) {
            if (!doesCommentExist(id)) {
                notExists++;
            }
        }
        return notExists;
    }

    /**
     * Returns list of GroupItems for a given Group.
     * If no GroupItems are present, returns a "No groups" Item with id GroupItem.NO_ITEMS
     */
    public List<GroupItem> getGroupItems(Group g) {
        List<GroupItem> items = new ArrayList<>();
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_GROUPITEMS.toString())) {
            ps.setString(1, g.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(resultSetToGroupItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Attempts insert of a new group. Throws exception if name already exists.
     * Commits and refreshes globalGroupList.
     */
    public Group createGroup(String name) throws SQLException {
        Group group = new Group(name);
        logger.trace("Created Group [id={},name={}]", group.getId(), name);

        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GROUP_CREATE.toString())) {
            ps.setString(1, group.getId());
            ps.setString(2, group.getName());
            ps.executeUpdate();

            this.commit();
            this.refreshGroups();
        }

        return group;
    }

    /**
     * Attempts rename of existing group.
     * Commits.
     */
    public void renameGroup(Group g, String newName) throws SQLException {
        if (!g.getName().equals(newName)) {
            logger.trace("Renaming Group [id={},name={},newName={}]", g.getId(), g.getName(), newName);

            try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GROUP_RENAME.toString())) {
                ps.setString(1, newName);
                ps.setString(2, g.getId());
                ps.executeUpdate();
            }

            this.commit();
            g.setName(newName);
            runLater(this::incrGroupRenameProperty);
        }
    }

    /**
     * Deletes a Group.
     * Recommended to run cleanUp() afterwards.
     */
    public void deleteGroup(Group g) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.DELETE_GROUP.toString())) {
            ps.setString(1, g.getId());
            ps.executeUpdate();

            logger.warn("Cleaning up after group delete [id={},name={}]", g.getId(), g.getName());
            this.cleanUp();
            this.commit();
            this.refreshGroups();
        }
    }

    /**
     * Will ignore inserting duplicate GroupItem(s) if same YouTube ID is present.
     * Could create links to the same GroupItem to multiple Group(s).
     */
    public void insertGroupItems(Group g, List<GroupItem> items) throws SQLException {
        try (PreparedStatement psCG = sqlite.prepareStatement(SQLLoader.CREATE_GITEM.toString());
             PreparedStatement psCGG = sqlite.prepareStatement(SQLLoader.CREATE_GROUP_GITEM.toString())) {

            for (GroupItem gi : items) {
                psCG.setString(1, gi.getId());
                psCG.setInt(2, gi.getTypeId().id());
                psCG.setString(3, gi.getTitle());
                psCG.setString(4, gi.getChannelTitle());
                psCG.setLong(5, gi.getPublished());
                psCG.setLong(6, gi.getLastChecked());
                psCG.setString(7, gi.getThumbUrl());
                psCG.addBatch();
                psCGG.setString(1, g.getId());
                psCGG.setString(2, gi.getId());
                psCGG.addBatch();
            }

            psCG.executeBatch();
            psCGG.executeBatch();
        }
    }

    /**
     * Updates gitem_list with set lastChecked values.
     */
    public void updateGroupItem(GroupItem item) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.UPDATE_GITEM.toString())) {
            ps.setString(1, item.getTitle());
            ps.setString(2, item.getChannelTitle());
            ps.setLong(3, item.getPublished());
            ps.setLong(4, System.currentTimeMillis());
            ps.setString(5, item.getThumbUrl());
            ps.setString(6, item.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes GroupItem(s).
     * Recommended to run cleanUp() afterwards.
     */
    public void deleteGroupItemLinks(Group group, List<GroupItem> items) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.DELETE_GROUP_GITEM.toString())) {
            for (GroupItem gi : items) {
                ps.setString(1, gi.getId());
                ps.setString(2, group.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Inserts comments for group refreshing.
     */
    public void insertComments(List<YouTubeComment> items) throws SQLException {
        // logger.trace("Inserting Comments [size={}]", items.size());
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.INSERT_IGNORE_COMMENTS.toString())) {
            for (YouTubeComment ct : items) {
                ps.setString(1, ct.getId());
                ps.setString(2, ct.getChannelId());
                ps.setString(3, ct.getVideoId());
                ps.setLong(4, ct.getPublished());
                ps.setString(5, ct.getCommentText());
                ps.setLong(6, ct.getLikes());
                ps.setLong(7, ct.getReplyCount());
                ps.setBoolean(8, ct.isReply());
                ps.setString(9, ct.getParentId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Gets all comment ids for the selected group's videos.
     * Used for group refresh.
     */
    public List<String> getCommentIds(Group group) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_ALL_COMMENT_IDS_BY_GROUP.toString())) {
            ps.setString(1, group.getId());
            try (ResultSet rs = ps.executeQuery()) {
                List<String> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(rs.getString("comment_id"));
                }

                return list;
            }
        }

    }

    /**
     * Insert videos for group refreshing.
     */
    public void insertVideos(List<YouTubeVideo> items) throws SQLException {
        logger.debug("Inserting Videos [size={}]", items.size());
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.INSERT_REPLACE_VIDEOS.toString())) {
            for (YouTubeVideo video : items) {
                ps.setString(1, video.getId());
                ps.setString(2, video.getChannelId());
                ps.setLong(3, video.getRefreshedOn());
                ps.setLong(4, video.getPublishedDate());
                ps.setString(5, video.getTitle());
                ps.setLong(6, video.getCommentCount());
                ps.setLong(7, video.getViewCount());
                ps.setLong(8, video.getLikes());
                ps.setLong(9, video.getDislikes());
                ps.setString(10, video.getDescription());
                ps.setString(11, video.getThumbUrl());
                ps.setInt(12, video.getResponseCode());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Get a list of all video ids.
     * Used for group refreshing.
     */
    public List<String> getAllVideoIds() throws SQLException {
        try (Statement s = sqlite.createStatement();
             ResultSet rs = s.executeQuery(SQLLoader.GET_ALL_UNIQUE_VIDEO_IDS.toString())) {

            List<String> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getString("video_id"));
            }

            return ids;
        }
    }

    /**
     * Gets a list of all video ids by group.
     * Used for group refeshing & export.
     */
    public List<String> getVideoIds(Group group) throws SQLException {
        logger.debug("GET_ALL_VIDEO_IDS_BY_GROUP");
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_ALL_VIDEO_IDS_BY_GROUP.toString())) {
            ps.setString(1, group.getId());
            try (ResultSet rs = ps.executeQuery()) {
                List<String> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(rs.getString("video_id"));
                }

                return list;
            }
        }
    }


    /**
     * Gets a list of all video ids by groupItem.
     * Used for comment export.
     */
    public List<String> getVideoIds(GroupItem gitem) throws SQLException {
        logger.debug("GET_ALL_VIDEO_IDS_BY_GROUPITEM");
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_ALL_VIDEO_IDS_BY_GROUPITEM.toString())) {
            ps.setString(1, gitem.getId());
            try (ResultSet rs = ps.executeQuery()) {
                List<String> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(rs.getString("video_id"));
                }

                return list;
            }
        }
    }


    public YouTubeVideo getVideo(String videoId) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_VIDEO_BY_ID.toString())) {
            ps.setString(1, videoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return resultSetToVideo(rs);
                }

                return null;
            }
        }
    }

    public List<YouTubeVideo> getVideos(GroupItem gitem, String keyword, String order, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_VIDEOS_BY_CRITERIA_GITEM.toString()
                .replace(":order", order))) {
            ps.setString(1, gitem.getId());
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, keyword);
            ps.setInt(4, limit);

            return resultSetToVideoList(ps);
        }
    }

    public List<YouTubeVideo> getVideos(Group group, String keyword, String order, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_VIDEOS_BY_CRITERIA_GROUP.toString()
                .replace(":order", order))) {
            ps.setString(1, group.getId());
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, keyword);
            ps.setInt(4, limit);

            return resultSetToVideoList(ps);
        }
    }

    /**
     * Updates video data for group refreshing.
     */
    /*public void updateVideos(List<YouTubeVideo> items) throws SQLException {
        try(PreparedStatement ps = sqlite.prepareStatement(SQLLoader.UPDATE_VIDEO.toString())) {
            for(YouTubeVideo video : items) {
                ps.setLong(1, video.getRefreshedOn());
                ps.setString(2, video.getTitle());
                ps.setLong(3, video.getCommentCount());
                ps.setLong(4, video.getViewCount());
                ps.setLong(5, video.getLikes());
                ps.setLong(6, video.getDislikes());
                ps.setString(7, video.getDescription());
                ps.setString(8, video.getThumbUrl());
                ps.setString(9, video.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }*/

    /**
     * Updates http code for group refreshing.
     */
    public void updateVideoHttpCode(String videoId, int httpCode) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.UPDATE_VIDEO_HTTPCODE.toString())) {
            ps.setInt(1, httpCode);
            ps.setString(2, videoId);
            ps.executeUpdate();
        }
    }

    /**
     * Returns existing threads and reply counts for group refreshing.
     * Threads with different reply counts are rechecked.
     */
    /*public Map<String,Integer> getCommentThreadReplyCounts(Group group) throws SQLException {
        try(PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_COMMENTTHREAD_REPLY_COUNT_BY_GROUP.toString())) {
            ps.setBoolean(1, false);
            ps.setString(2, group.getId());
            try(ResultSet rs = ps.executeQuery()) {
                Map<String,Integer> map = new HashMap<>();
                while(rs.next()) {
                    map.put(rs.getString("comment_id"), rs.getInt("db_replies"));
                }
                return map;
            }
        }
    }*/

    /**
     * Insert channels for group refreshing.
     */
    public void insertChannels(List<YouTubeChannel> items) throws SQLException {
        // logger.debug("Inserting Channels [size={}]", items.size());
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.INSERT_IGNORE_CHANNELS.toString())) {
            for (YouTubeChannel c : items) {
                ps.setString(1, c.getId());
                ps.setString(2, c.getTitle());
                ps.setString(3, c.getThumbUrl());
                ps.setBoolean(4, false);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Updates channels for group refreshing.
     */
    /*public void updateChannels(List<YouTubeChannel> items) throws SQLException {
        try(PreparedStatement ps = sqlite.prepareStatement(SQLLoader.UPDATE_CHANNEL.toString())) {
            for(YouTubeChannel c : items) {
                ps.setString(1, c.getTitle());
                ps.setString(2, c.getThumbUrl());
                ps.setBoolean(3, false);
                ps.setString(4, c.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }*/

    /**
     * Checks if exists and caches it if it does.
     */
    public YouTubeChannel channelExists(String channelId) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_CHANNEL_EXISTS.toString())) {
            ps.setString(1, channelId);
            try (ResultSet rs = ps.executeQuery()) {
                YouTubeChannel channel = null;
                if (rs.next()) {
                    channelCache.put(channelId, channel = resultSetToChannel(rs));
                }
                return channel;
            }
        }
    }

    /**
     * Returns all channel ids for group refreshing.
     */
    /*public List<String> getAllChannelIds() throws SQLException {
        try(PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_ALL_UNIQUE_CHANNEL_IDS.toString());
            ResultSet rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while(rs.next()) {
                list.add(rs.getString("channel_id"));
            }
            return list;
        }
    }*/

    /**
     *
     */
    public YouTubeChannel getChannel(String channelId) {
        YouTubeChannel channel = channelCache.getIfPresent(channelId);
        if (channel != null) {
            return channel;
        } else {
            try {
                return channelExists(channelId);
            } catch (SQLException e) {
                return null;
            }
        }
    }

    /**
     * Inserts to gitem_video for group refreshing.
     */
    public void insertGroupItemVideo(List<GroupItemVideo> items) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.INSERT_IGNORE_GITEM_VIDEO.toString())) {
            for (GroupItemVideo vg : items) {
                ps.setString(1, vg.gitemId);
                ps.setString(2, vg.videoId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public CommentQuery commentQuery() {
        return new CommentQuery(this);
    }

    /**
     * Returns all of the comments associated with a comment parentId.
     */
    public List<YouTubeComment> getCommentTree(String parentId) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_COMMENT_TREE.toString())) {
            ps.setString(1, parentId);
            ps.setString(2, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<YouTubeComment> tree = new ArrayList<>();
                while (rs.next()) {
                    checkChannel(rs);
                    tree.add(resultSetToComment(rs));
                }
                return tree;
            }
        }
    }

    /******** Stats and Info Methods **********/

    public long getLastChecked(Group group) {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_GROUP_LAST_CHECKED.toString())) {
            ps.setString(1, group.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("checked");
                }
            }
        } catch (SQLException ignored) {
        }

        return 0;
    }

    public GroupStats getGroupStats(Group group) throws SQLException {
        GroupStats stats = new GroupStats();
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_VIDEO_STATS.toString())) {
            ps.setString(1, group.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalVideos(rs.getLong("total_videos"));
                    stats.setTotalViews(rs.getLong("total_views"));
                    stats.setTotalLikes(rs.getLong("total_likes"));
                    stats.setTotalDislikes(rs.getLong("total_dislikes"));
                    stats.setTotalComments(rs.getLong("total_comments"));
                }
            }
        }
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_UNIQUE_VIEWERS_BY_GROUP.toString())) {
            ps.setString(1, group.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setUniqueViewers(rs.getLong("unique_viewers"));
                }
            }
        }
        stats.setMostViewed(this.getMostPopularVideos(group, 10));
        stats.setMostDisliked(this.getMostDislikedVideos(group, 10));
        stats.setMostCommented(this.getMostCommentedVideos(group, 10));
        stats.setCommentsDisabled(this.getDisabledVideos(group, 25));
        stats.setWeeklyUploadHistogram(this.getWeekByWeekVideoHistogram(group));

        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_COMMENT_STATS.toString())) {
            ps.setString(1, group.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalCommentLikes(rs.getLong("total_likes"));
                    stats.setTotalGrabbedComments(rs.getLong("total_comments"));
                }
            }
        }
        stats.setMostLikedViewers(this.getMostPopularViewers(group, 25));
        stats.setMostActiveViewers(this.getMostActiveViewers(group, 25));
        stats.setWeeklyCommentHistogram(this.getWeekByWeekCommentHistogram(group));

        return stats;
    }

    private Map<Long, Long> getWeekByWeekCommentHistogram(Group group) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_COMMENT_WEEK_HISTOGRAM.toString())) {
            ps.setString(1, group.getId());
            return resultSetToHistogram(ps);
        }
    }

    private Map<Long, Long> getWeekByWeekVideoHistogram(Group group) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_VIDEO_WEEK_HISTOGRAM.toString())) {
            ps.setString(1, group.getId());
            return resultSetToHistogram(ps);
        }
    }

    private LinkedHashMap<YouTubeChannel, Long> getMostActiveViewers(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_GROUP_ACTIVE_VIEWERS.toString())) {
            ps.setString(1, group.getId());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                LinkedHashMap<YouTubeChannel, Long> map = new LinkedHashMap<>();
                while (rs.next()) {
                    checkChannel(rs);
                    map.put(resultSetToChannel(rs), rs.getLong("count"));
                }
                return map;
            }
        }
    }

    private LinkedHashMap<YouTubeChannel, Long> getMostPopularViewers(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_GROUP_POPULAR_VIEWERS.toString())) {
            ps.setString(1, group.getId());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                LinkedHashMap<YouTubeChannel, Long> map = new LinkedHashMap<>();
                while (rs.next()) {
                    checkChannel(rs);
                    map.put(resultSetToChannel(rs), rs.getLong("total_likes"));
                }
                return map;
            }
        }
    }

    private List<YouTubeVideo> getMostPopularVideos(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_GROUP_POPULAR_VIDEOS.toString())) {
            ps.setString(1, group.getId());
            ps.setInt(2, limit);
            return resultSetToVideoList(ps);
        }
    }

    private List<YouTubeVideo> getMostDislikedVideos(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_GROUP_DISLIKED_VIDEOS.toString())) {
            ps.setString(1, group.getId());
            ps.setInt(2, limit);
            return resultSetToVideoList(ps);
        }
    }

    private List<YouTubeVideo> getMostCommentedVideos(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_GROUP_COMMENTED_VIDEOS.toString())) {
            ps.setString(1, group.getId());
            ps.setInt(2, limit);
            return resultSetToVideoList(ps);
        }
    }

    private List<YouTubeVideo> getDisabledVideos(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_GROUP_DISABLED_VIDEOS.toString())) {
            ps.setString(1, group.getId());
            ps.setInt(2, limit);
            return resultSetToVideoList(ps);
        }
    }

    private List<YouTubeVideo> resultSetToVideoList(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<YouTubeVideo> list = new ArrayList<>();
            while (rs.next()) {
                list.add(resultSetToVideo(rs));
            }
            return list;
        }
    }

    private Map<Long, Long> resultSetToHistogram(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            Map<Long, Long> data = new LinkedHashMap<>();
            while (rs.next()) {
                data.put(rs.getLong("week"), rs.getLong("count"));
            }
            return data;
        }
    }

    /**
     * Gets all gitem_video for group refreshing.
     */
    /*public List<GroupItemVideo> getAllGroupItemVideo() throws SQLException {
        try(PreparedStatement ps = sqlite.prepareStatement(SQLLoader.GET_ALL_GITEM_VIDEO.toString());
            ResultSet rs = ps.executeQuery()) {

            List<GroupItemVideo> list = new ArrayList<>();
            while(rs.next()) {
                list.add(resultSetToGroupItemVideo(rs));
            }
            return list;
        }
    }*/

    public static class GroupItemVideo {
        private String gitemId;
        private String videoId;

        public GroupItemVideo(String gitemId, String videoId) {
            this.gitemId = gitemId;
            this.videoId = videoId;
        }

        public boolean equals(Object o) {
            if (o instanceof GroupItemVideo) {
                GroupItemVideo giv = (GroupItemVideo) o;
                return giv.gitemId.equals(gitemId) && giv.videoId.equals(videoId);
            }
            return false;
        }
    }
}
