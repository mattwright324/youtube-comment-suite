package io.mattw.youtube.commentsuite.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.*;

import static io.mattw.youtube.commentsuite.db.SQLLoader.*;

public class CommentDatabase implements Closeable {

    private static final Logger logger = LogManager.getLogger();

    private final Connection sqlite;

    private GroupsTable groups;
    private GroupItemsTable groupItems;
    private VideosTable videos;
    private CommentsTable comments;
    private ModeratedCommentsTable moderatedComments;
    private ChannelsTable channels;

    /**
     * Default constructor for testing.
     */
    public CommentDatabase(final Connection sqlite) {
        this.sqlite = sqlite;
        this.init(sqlite);
    }

    /**
     * Actual constructor.
     *
     * @param fileName name of database file
     * @throws SQLException failed to create database
     */
    public CommentDatabase(final String fileName) throws SQLException {
        logger.debug("Initialize Database [file={}]", fileName);

        this.sqlite = DriverManager.getConnection(String.format("jdbc:sqlite:%s", fileName));
        this.sqlite.setAutoCommit(false);

        this.init(sqlite);
        this.create();
        groups.refreshAllGroups();
    }

    private void init(final Connection connection) {
        this.groups = new GroupsTable(connection, this);
        this.groupItems = new GroupItemsTable(connection);
        this.videos = new VideosTable(connection);
        this.comments = new CommentsTable(connection, this);
        this.moderatedComments = new ModeratedCommentsTable(connection, this);
        this.channels = new ChannelsTable(connection);
    }

    public void create() throws SQLException {
        logger.debug("Creating tables if not exists.");
        try (Statement s = sqlite.createStatement()) {
            s.executeUpdate(CREATE_DB.toString());
        }
        this.commit();
    }

    public void vacuum() throws SQLException {
        logger.warn("Vacuuming database. This may take a long time.");
        sqlite.setAutoCommit(true);
        try (Statement s = sqlite.createStatement()) {
            s.execute("VACUUM");
        }
        sqlite.setAutoCommit(false);
    }

    public void reset() throws SQLException {
        logger.warn("Dropping all database contents. This cannot be undone.");
        try (Statement s = sqlite.createStatement()) {
            s.executeUpdate(RESET_DB.toString());
        }
        this.commit();
        this.vacuum();
        this.create();
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

    public void cleanUp() throws SQLException {
        logger.warn("Cleaning database of unlinked content.");
        try (Statement s = sqlite.createStatement()) {
            s.executeUpdate(CLEAN_DB.toString());
        }
        commit();
    }

    public GroupsTable groups() {
        return groups;
    }

    public GroupItemsTable groupItems() {
        return groupItems;
    }

    public VideosTable videos() {
        return videos;
    }

    public CommentsTable comments() {
        return comments;
    }

    public ModeratedCommentsTable moderatedComments() {
        return moderatedComments;
    }

    public ChannelsTable channels() {
        return channels;
    }

    public Collection<String> findChannelsNotExisting(Collection<String> channelIds) {
        Set<String> idList = new HashSet<>();

        for (String id : channelIds) {
            try {
                if (channels.notExists(id)) {
                    idList.add(id);
                }
            } catch (SQLException e) {
                logger.error(e);
            }
        }

        return idList;
    }

    public long countVideosNotExisting(Collection<String> videoIds) {
        long notExists = 0;
        for (String id : videoIds) {
            try {
                if (videos.notExists(id)) {
                    notExists++;
                }
            } catch (SQLException e) {
                notExists++;
            }
        }
        return notExists;
    }

    public long countCommentsNotExisting(Collection<String> commentIds) {
        // logger.trace("Checking if commentIds's exist [size={},ids={}]", commentIds.size(), commentIds.toString());
        long notExists = 0;
        for (String id : commentIds) {
            try {
                if (comments.notExists(id)) {
                    notExists++;
                }
            } catch (SQLException e) {
                notExists++;
            }
        }
        return notExists;
    }

    public long countModeratedCommentsNotExisting(Collection<String> commentIds) {
        // logger.trace("Checking if commentIds's exist [size={},ids={}]", commentIds.size(), commentIds.toString());
        long notExists = 0;
        for (String id : commentIds) {
            try {
                if (moderatedComments.notExists(id)) {
                    notExists++;
                }
            } catch (SQLException e) {
                notExists++;
            }
        }
        return notExists;
    }

    public CommentQuery commentQuery() {
        return new CommentQuery(this);
    }

    /**
     * Returns all the comments associated with a comment parentId.
     */
    public List<YouTubeComment> getCommentTree(String parentId, boolean includeModeration) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(GET_COMMENT_TREE.toString())) {
            ps.setString(1, parentId);
            ps.setString(2, parentId);
            ps.setBoolean(3, includeModeration);
            ps.setString(4, parentId);
            ps.setString(5, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<YouTubeComment> tree = new ArrayList<>();
                while (rs.next()) {
                    channels.check(rs.getString("channel_id"));
                    tree.add(comments.to(rs));
                }
                return tree;
            }
        }
    }

    /******** Stats and Info Methods **********/

    public long getLastChecked(Group group) {
        try (PreparedStatement ps = sqlite.prepareStatement(GET_GROUP_LAST_CHECKED.toString())) {
            ps.setString(1, group.getGroupId());
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
        try (PreparedStatement ps = sqlite.prepareStatement(GET_VIDEO_STATS.toString())) {
            ps.setString(1, group.getGroupId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalVideos(rs.getLong("total_videos"));
                    stats.setTotalViews(rs.getLong("total_views"));
                    stats.setTotalLikes(rs.getLong("total_likes"));
                    stats.setTotalComments(rs.getLong("total_comments"));
                }
            }
        }
        try (PreparedStatement ps = sqlite.prepareStatement(GET_UNIQUE_VIEWERS_BY_GROUP.toString())) {
            ps.setString(1, group.getGroupId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setUniqueViewers(rs.getLong("unique_viewers"));
                }
            }
        }
        stats.setMostViewed(this.getMostPopularVideos(group, 10));
        stats.setMostCommented(this.getMostCommentedVideos(group, 10));
        stats.setCommentsDisabled(this.getDisabledVideos(group, 25));
        stats.setWeeklyUploadHistogram(this.getWeekByWeekVideoHistogram(group));

        try (PreparedStatement ps = sqlite.prepareStatement(GET_COMMENT_STATS.toString())) {
            ps.setString(1, group.getGroupId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalCommentLikes(rs.getLong("total_likes"));
                    stats.setTotalGrabbedComments(rs.getLong("total_comments"));
                }
            }
        }
        try (PreparedStatement ps = sqlite.prepareStatement(GET_MODERATED_COMMENT_STATS.toString())) {
            ps.setString(1, group.getGroupId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalModeratedComments(rs.getLong("total_comments"));
                }
            }
        }
        stats.setMostLikedViewers(this.getMostPopularViewers(group, 25));
        stats.setMostActiveViewers(this.getMostActiveViewers(group, 25));
        stats.setWeeklyCommentHistogram(this.getWeekByWeekCommentHistogram(group));

        return stats;
    }

    private Map<Long, Long> getWeekByWeekCommentHistogram(Group group) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(GET_COMMENT_WEEK_HISTOGRAM.toString())) {
            ps.setString(1, group.getGroupId());
            return resultSetToHistogram(ps);
        }
    }

    private Map<Long, Long> getWeekByWeekVideoHistogram(Group group) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(GET_VIDEO_WEEK_HISTOGRAM.toString())) {
            ps.setString(1, group.getGroupId());
            return resultSetToHistogram(ps);
        }
    }

    private LinkedHashMap<YouTubeChannel, Long> getMostActiveViewers(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(GET_GROUP_ACTIVE_VIEWERS.toString())) {
            ps.setString(1, group.getGroupId());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                LinkedHashMap<YouTubeChannel, Long> map = new LinkedHashMap<>();
                while (rs.next()) {
                    channels.check(rs.getString("channel_id"));
                    map.put(channels.to(rs), rs.getLong("count"));
                }
                return map;
            }
        }
    }

    private LinkedHashMap<YouTubeChannel, Long> getMostPopularViewers(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(GET_GROUP_POPULAR_VIEWERS.toString())) {
            ps.setString(1, group.getGroupId());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                LinkedHashMap<YouTubeChannel, Long> map = new LinkedHashMap<>();
                while (rs.next()) {
                    channels.check(rs.getString("channel_id"));
                    map.put(channels.to(rs), rs.getLong("total_likes"));
                }
                return map;
            }
        }
    }

    private List<YouTubeVideo> getMostPopularVideos(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(GET_GROUP_POPULAR_VIDEOS.toString())) {
            ps.setString(1, group.getGroupId());
            ps.setInt(2, limit);
            return videos.toList(ps);
        }
    }

    private List<YouTubeVideo> getMostCommentedVideos(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(GET_GROUP_COMMENTED_VIDEOS.toString())) {
            ps.setString(1, group.getGroupId());
            ps.setInt(2, limit);
            return videos.toList(ps);
        }
    }

    private List<YouTubeVideo> getDisabledVideos(Group group, int limit) throws SQLException {
        try (PreparedStatement ps = sqlite.prepareStatement(GET_GROUP_DISABLED_VIDEOS.toString())) {
            ps.setString(1, group.getGroupId());
            ps.setInt(2, limit);

            return videos.toList(ps);
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

}
