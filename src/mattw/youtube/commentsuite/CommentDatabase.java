package mattw.youtube.commentsuite;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentDatabase {

    private final Connection con;

    private final Group noGroup = new Group(Group.NO_GROUP, "No groups");
    private final GroupItem noItems = new GroupItem(GroupItem.NO_ITEMS, "No items");

    public final ObservableList<Group> globalGroupList = FXCollections.observableArrayList();
    public final Map<String,YouTubeChannel> channelCache = new HashMap<>();

    public CommentDatabase(String dbfile) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:"+dbfile);
        con.setAutoCommit(false);
        create();
    }

    public void create() throws SQLException {
        Statement s = con.createStatement();
        s.addBatch("CREATE TABLE IF NOT EXISTS gitem_type (type_id INTEGER PRIMARY KEY, nameProperty STRING);");
        s.addBatch("INSERT OR IGNORE INTO gitem_type VALUES (0, 'video'),(1, 'channel'),(2, 'playlist');");
        s.addBatch("CREATE TABLE IF NOT EXISTS gitem_list ("
                + "gitem_id STRING PRIMARY KEY," // gitem_id is now previous field youtube_id
                + "type_id INTEGER,"
                + "title STRING,"
                + "channel_title STRING,"
                + "published DATE,"
                + "last_checked DATE,"
                + "thumb_url STRING,"
                + "FOREIGN KEY(type_id) REFERENCES gitem_type(type_id));");
        s.addBatch("CREATE TABLE IF NOT EXISTS groups (group_id STRING PRIMARY KEY, group_name STRING UNIQUE);");
        s.addBatch("INSERT OR IGNORE INTO groups VALUES ('28da132f5f5b48d881264d892aba790a', 'Default');");
        s.addBatch("CREATE TABLE IF NOT EXISTS group_gitem ("
                + "group_id STRING,"
                + "gitem_id STRING,"
                + "FOREIGN KEY(group_id) REFERENCES groups(group_id),"
                + "FOREIGN KEY(gitem_id) REFERENCES gitem_list(gitem_id));");
        s.addBatch("CREATE TABLE IF NOT EXISTS gitem_video ("
                + "gitem_id STRING,"
                + "video_id STRING,"
                + "FOREIGN KEY(gitem_id) REFERENCES gitem_list(gitem_id),"
                + "FOREIGN KEY(video_id) REFERENCES videos(video_id));");
        s.addBatch("CREATE TABLE IF NOT EXISTS videos ("
                + "video_id STRING PRIMARY KEY,"
                + "channel_id STRING,"
                + "grab_date INTEGER,"
                + "publish_date INTEGER,"
                + "video_title STRING,"
                + "total_comments INTEGER,"
                + "total_views INTEGER,"
                + "total_likes INTGEGER,"
                + "total_dislikes INTEGER,"
                + "video_desc STRING,"
                + "thumb_url STRING,"
                + "http_code int,"
                + "FOREIGN KEY(channel_id) REFERENCES channels(channel_id))");
        s.addBatch("CREATE TABLE IF NOT EXISTS comments ("
                + "comment_id STRING PRIMARY KEY,"
                + "channel_id STRING,"
                + "video_id STRING,"
                + "comment_date INTEGER,"
                + "comment_likes INTEGER,"
                + "reply_count INTEGER,"
                + "is_reply BOOLEAN,"
                + "parent_id STRING,"
                + "comment_text TEXT,"
                + "FOREIGN KEY(channel_id) REFERENCES channels(channel_id),"
                + "FOREIGN KEY(video_id) REFERENCES videos(video_id))");
        s.addBatch("CREATE TABLE IF NOT EXISTS channels ("
                + "channel_id STRING PRIMARY KEY,"
                + "channel_name STRING,"
                + "channel_profile_url STRING,"
                + "download_profile BOOLEAN)");
        s.executeBatch();
        s.close();
        commit();
    }

    public void commit() throws SQLException {
        con.commit();
    }

    /**
     * Cleans up unlinked data in all tables.
     * To be ran after deleting a Group or GroupItem.
     */
    public void cleanUp() throws SQLException {
        Statement s = con.createStatement();
        int ggs = s.executeUpdate("DELETE FROM group_gitem WHERE group_id NOT IN (SELECT DISTINCT group_id FROM groups)");
        int gitems = s.executeUpdate("DELETE FROM gitem_list WHERE gitem_id NOT IN (SELECT DISTINCT gitem_id FROM group_gitem)");
        int vgs = s.executeUpdate("DELETE FROM gitem_video WHERE gitem_id NOT IN (SELECT DISTINCT gitem_id FROM gitem_list)");
        int videos = s.executeUpdate("DELETE FROM videos WHERE video_id NOT IN (SELECT DISTINCT video_id FROM gitem_video)");
        int comments = s.executeUpdate("DELETE FROM comments WHERE video_id NOT IN (SELECT DISTINCT video_id FROM videos)");
        int channels = s.executeUpdate("WITH clist AS (SELECT DISTINCT channel_id FROM videos UNION SELECT channel_id FROM comments) DELETE FROM channels WHERE channel_id NOT IN clist");
        System.out.format("DELETED FROM group_gitem, gitem_list, gitem_video, videos, comments, channels (%s, %s, %s, %s, %s, %s)\r\n", ggs, gitems, vgs, videos, comments, channels);
        s.close();
    }

    /**
     * Attempts to get cached channel or from database to cache it.
     */
    public YouTubeChannel getChannel(String channelId) {
        if(channelCache.containsKey(channelId)) {
            return channelCache.get(channelId);
        }
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM channels WHERE channel_id = ?");
            ps.setString(1, channelId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                YouTubeChannel channel = resultSetToChannel(rs);
                channelCache.put(channelId, channel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void refreshGroups() throws SQLException {
        Statement s = con.createStatement();
        ResultSet rs = s.executeQuery("SELECT * FROM groups");
        List<Group> groups = new ArrayList<>();
        while(rs.next()) {
            groups.add(resultSetToGroup(rs));
        }
        rs.close();
        s.close();
        for(int i = 0; i< globalGroupList.size(); i++) {
            if(!groups.contains(globalGroupList.get(i))) {
                globalGroupList.remove(i);
            }
        }
        for(Group g : groups) {
            if(!globalGroupList.contains(g)) {
                globalGroupList.add(g);
            }
        }
        if(globalGroupList.isEmpty()) {
            globalGroupList.add(noGroup);
        }
    }

    private Group resultSetToGroup(ResultSet rs) throws SQLException {
        return new Group(rs.getString("group_id"), rs.getString("group_name"));
    }

    private GroupItem resultSetToGroupItem(ResultSet rs) throws SQLException {
        return new GroupItem(rs.getString("gitem_id"),
                rs.getInt("type_id"),
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
                rs.getString("channel_profile_url"),
                rs.getBoolean("download_profile"));
    }

    private YouTubeComment resultSetToComment(ResultSet rs) throws SQLException {
        return new YouTubeComment(rs.getString("comment_id"),
                rs.getString("comment_text"),
                rs.getLong("comment_date"),
                rs.getString("video_id"),
                rs.getString("channel_id"),
                rs.getInt("likes"),
                rs.getInt("replies"),
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

    /**
     * Returns list of GroupItems for a given Group.
     * If no GroupItems are present, returns a "No groups" Item with id GroupItem.NO_ITEMS
     */
    public List<GroupItem> getGroupItems(Group g) {
        List<GroupItem> items = new ArrayList<>();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM gitem_list JOIN group_gitem USING (gitem_id) WHERE group_id = ?");
            ps.setString(1, g.getId());
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                items.add(resultSetToGroupItem(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if(items.isEmpty()) { items.add(noItems); }
        return items;
    }

    /**
     * Attempts insert of a new group. Throws exception if name already exists.
     * Commits and refreshes globalGroupList.
     */
    public Group createGroup(String name) throws SQLException {
        Group group = new Group(name);
        PreparedStatement ps = con.prepareStatement("INSERT INTO groups (group_id, group_name) VALUES (?, ?)");
        ps.setString(1, group.getId());
        ps.setString(2, group.getName());
        ps.executeUpdate();
        ps.close();
        commit();
        refreshGroups();
        return group;
    }

    /**
     * Attempts rename of existing group.
     * Commits.
     */
    public Group renameGroup(Group g, String newName) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE groups SET group_name = ? WHERE group_id = ?");
        ps.setString(1, newName);
        ps.setString(2, g.getId());
        ps.executeUpdate();
        ps.close();
        commit();
        g.setName(newName);
        return g;
    }

    /**
     * Deletes a Group.
     * Recommended to run cleanUp() afterwards.
     */
    public void deleteGroup(Group g) throws SQLException {
        PreparedStatement ps = con.prepareStatement("DELETE FROM groups WHERE group_id = ?");
        ps.setString(1, g.getId());
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Will ignore inserting duplicate GroupItem(s) if same YouTube ID is present.
     * Could create links to the same GroupItem to multiple Group(s).
     */
    public void insertGroupItems(Group g, List<GroupItem> items) throws SQLException {
        PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO gitem_list (gitem_id, type_id, youtube_id, title, channel_title, published, last_checked, thumb_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        PreparedStatement ps2 = con.prepareStatement("INSERT OR IGNORE INTO group_gitem (group_id, gitem_id) VALUES (?, ?)");
        for(GroupItem gi : items) {
            ps.setString(1, gi.getYouTubeId());
            ps.setInt(2, gi.typeId);
            ps.setString(3, gi.getYouTubeId());
            ps.setString(4, gi.getTitle());
            ps.setString(5, gi.getChannelTitle());
            ps.setLong(6, gi.getPublished());
            ps.setLong(7, gi.getLastChecked());
            ps.addBatch();
            ps2.setString(1, g.getId());
            ps2.setString(2, gi.getYouTubeId());
            ps2.addBatch();
        }
        ps.executeBatch();
        ps.close();
        ps2.executeBatch();
        ps2.close();
    }

    /**
     * Updates gitem_list with set lastChecked values.
     */
    public void updateGroupItemsLastChecked(List<GroupItem> items) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE gitem_list SET last_checked = ? WHERE gitem_id = ?");
        for(GroupItem gi : items) {
            ps.setLong(1, gi.getLastChecked());
            ps.setString(2, gi.getYouTubeId());
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    /**
     * Deletes GroupItem(s).
     * Recommended to run cleanUp() afterwards.
     */
    public void deleteGroupItems(List<GroupItem> items) throws SQLException {
        PreparedStatement ps = con.prepareStatement("DELETE FROM gitem_list WHERE gitem_id = ?");
        for(GroupItem gi : items) {
            ps.setString(1, gi.getYouTubeId());
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    /**
     * TODO
     * insertChannels
     * updateVideos
     * updateVideoHttpCode
     * updateChannels
     * getVideos
     */

    public void insertComments(List<YouTubeComment> items) throws SQLException {
        PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO comments (comment_id, channel_id, video_id, comment_date, comment_text, comment_likes, reply_count, is_reply, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        for(YouTubeComment ct : items) {
            ps.setString(1, ct.getYouTubeId());
            ps.setString(2, ct.getChannelId());
            ps.setString(3, ct.getVideoId());
            ps.setLong(4, ct.getDate().getTime());
            ps.setString(5, ct.getText());
            ps.setLong(6, ct.getLikes());
            ps.setLong(7, ct.getReplyCount());
            ps.setBoolean(8, ct.isReply());
            ps.setString(9, ct.getParentId());
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    /**
     * Gets all comment ids for the selected group's videos.
     * Used for group refresh.
     */
    public List<String> getCommentIds(Group group) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT comment_id FROM comments JOIN gitem_video USING (video_id) JOIN group_gitem USING (gitem_id) WHERE group_id = ?");
        ps.setString(1, group.getId());
        ResultSet rs = ps.executeQuery();
        List<String> list = new ArrayList<>();
        while(rs.next()) {
            list.add(rs.getString("comment_id"));
        }
        ps.close();
        rs.close();
        return list;
    }

    /**
     * Insert videos for group refreshing.
     */
    public void insertVideos(List<YouTubeVideo> items) throws SQLException {
        PreparedStatement ps = con.prepareStatement("INSERT INTO videos (video_id, channel_id, grab_date, publish_date, video_title, total_comments, total_views, total_likes, total_dislikes, video_desc, thumb_url, http_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        for(YouTubeVideo video : items) {
            ps.setString(1, video.getYouTubeId());
            ps.setString(2, video.getChannelId());
            ps.setLong(3, video.getLastGrabDate());
            ps.setLong(4, video.getPublishedDate());
            ps.setString(5, video.getTitle());
            ps.setLong(6, video.getCommentCount());
            ps.setLong(7, video.getViews());
            ps.setLong(8, video.getLikes());
            ps.setLong(9, video.getDislikes());
            ps.setString(10, video.getDescription());
            ps.setString(11, video.getThumbUrl());
            ps.setInt(12, video.getHttpCode());
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    /**
     * Get a list of all video ids.
     * Used for group refreshing.
     */
    public List<String> getAllVideoIds() throws SQLException {
        Statement s = con.createStatement();
        ResultSet rs = s.executeQuery("SELECT DISTINCT video_id FROM videos");
        List<String> ids = new ArrayList<>();
        while(rs.next()) {
            ids.add(rs.getString("video_id"));
        }
        rs.close();
        s.close();
        return ids;
    }

    /**
     * Gets a list of all video ids by group.
     * Used for group refeshing.
     */
    public List<String> getVideoIds(Group group) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT video_id FROM videos WHERE video_id IN (SELECT video_id FROM gitem_video JOIN group_gitem USING (gitem_id) WHERE group_id = ?) ORDER BY publish_date DESC");
        ps.setString(1, group.getId());
        ResultSet rs = ps.executeQuery();
        List<String> list = new ArrayList<>();
        while(rs.next()) {
            list.add(rs.getString("video_id"));
        }
        ps.close();
        rs.close();
        return list;
    }

    /**
     * Gets a list of videos by group.
     * Used for group refreshing.
     */
    public List<YouTubeVideo> getVideos(Group group) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM videos JOIN channels USING(channel_id) WHERE video_id IN (SELECT video_id FROM gitem_video JOIN group_gitem USING (gitem_id) WHERE group_id = ?) ORDER BY publish_date DESC");
        ps.setString(1, group.getId());
        ResultSet rs = ps.executeQuery();
        List<YouTubeVideo> list = new ArrayList<>();
        while(rs.next()) {
            list.add(resultSetToVideo(rs));
            String channelId = rs.getString("channel_id");
            if(!channelCache.containsKey(channelId)) {
                channelCache.put(channelId, resultSetToChannel(rs));
            }
        }
        ps.close();
        rs.close();
        return list;
    }

    /**
     * Updates video data for group refreshing.
     */
    public void updateVideos(List<YouTubeVideo> items) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE videos SET grab_date = ?, video_title = ?, total_comments = ?, total_views = ?, total_likes = ?, total_dislikes = ?, video_desc = ?, thumb_url = ? "
                + "WHERE video_id = ?");
        for(YouTubeVideo video : items) {
            ps.setLong(1, video.getLastGrabDate());
            ps.setString(2, video.getTitle());
            ps.setLong(3, video.getCommentCount());
            ps.setLong(4, video.getViews());
            ps.setLong(5, video.getLikes());
            ps.setLong(6, video.getDislikes());
            ps.setString(7, video.getDescription());
            ps.setString(8, video.getThumbUrl());
            ps.setString(9, video.getYouTubeId());
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    /**
     * Insert channels for group refreshing.
     */
    public void insertChannels(List<YouTubeChannel> items) throws SQLException {
        PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO channels (channel_id, channel_name, channel_profile_url, download_profile) VALUES (?, ?, ?, ?)");
        for(YouTubeChannel c : items) {
            ps.setString(1, c.getYouTubeId());
            ps.setString(2, c.getTitle());
            ps.setString(3, c.getThumbUrl());
            ps.setBoolean(4, c.fetchThumb());
        }
        ps.executeBatch();
        ps.close();
    }

    /**
     * Updates channels for group refreshing.
     */
    public void updateChannels(List<YouTubeChannel> items) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE channels SET channel_name = ?, channel_profile_url = ?, download_profile = ? WHERE channel_id = ?");
        for(YouTubeChannel c : items) {
            ps.setString(1, c.getTitle());
            ps.setString(2, c.getThumbUrl());
            ps.setBoolean(3, c.fetchThumb());
            ps.setString(4, c.getYouTubeId());
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    /**
     * Returns all channel ids for group refreshing.
     */
    public List<String> getAllChannelIds() throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT channel_id FROM channels");
        ResultSet rs = ps.executeQuery();
        List<String> list = new ArrayList<>();
        while(rs.next()) {
            list.add(rs.getString("channel_id"));
        }
        ps.close();
        rs.close();
        return list;
    }

    /**
     * Inserts to gitem_video for group refreshing.
     */
    public void insertGroupItemVideo(List<GroupItemVideo> items) throws SQLException {
        PreparedStatement ps = con.prepareStatement("INSERT INTO video_group (gitem_id, video_id) VALUES (?, ?)");
        for(GroupItemVideo vg : items) {
            ps.setString(1, vg.gitemId);
            ps.setString(2, vg.videoId);
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
    }

    /**
     * Gets all gitem_video for group refreshing.
     */
    public List<GroupItemVideo> getAllGroupItemVideo() throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * from gitem_video");
        ResultSet rs = ps.executeQuery();
        List<GroupItemVideo> list = new ArrayList<>();
        while(rs.next()) {
            list.add(resultSetToGroupItemVideo(rs));
        }
        rs.close();
        ps.close();
        return list;
    }

    class GroupItemVideo {
        private String gitemId;
        private String videoId;
        public GroupItemVideo(String gitemId, String videoId) {
            this.gitemId = gitemId;
            this.videoId = videoId;
        }
        public boolean equals(Object o) {
            if(o instanceof GroupItemVideo) {
                GroupItemVideo giv = (GroupItemVideo) o;
                return giv.gitemId.equals(gitemId) && giv.videoId.equals(videoId);
            }
            return false;
        }
    }

    public CommentQuery commentQuery() { return new CommentQuery(); }

    /**
     * Query for a comment list based on the constraints.
     */
    class CommentQuery {
        private long totalResults = 0;
        private int page = 1;

        private Group group = null;
        private GroupItem gitem = null;
        private int orderBy = 0;
        private int ctype = 0;
        private int limit = 500;
        private String nameLike;
        private String textLike;
        private long before = Long.MAX_VALUE;
        private long after = Long.MIN_VALUE;

        final private String[] order = {
                "comment_date DESC ",
                "comment_date ASC ",
                "comment_likes DESC ",
                "reply_count DESC ",
                "LENGTH(comment_text) DESC ",
                "channel_name ASC, comment_date DESC ",
                "comment_text ASC "
        };

        public CommentQuery orderBy(int order) {
            this.orderBy = order;
            return this;
        }

        public CommentQuery ctype(int ctype) {
            this.ctype = ctype;
            return this;
        }

        public CommentQuery limit(int limit) {
            this.limit = limit;
            return this;
        }

        public CommentQuery before(long time) {
            this.before = time;
            return this;
        }

        public CommentQuery after(long time) {
            this.after = time;
            return this;
        }

        public CommentQuery nameLike(String nameLike) {
            this.nameLike = nameLike;
            return this;
        }

        public CommentQuery textLike(String textLike) {
            this.textLike = textLike;
            return this;
        }

        public int getPageCount() {
            return (int) ((totalResults*1.0) / limit) + 1;
        }

        public long getTotalResults() {
            return totalResults;
        }

        public List<YouTubeComment> get(int page, Group group, GroupItem gitem) throws SQLException {
            this.page = Math.abs(page);
            this.group = group;
            this.gitem = gitem;
            List<YouTubeComment> items = new ArrayList<>();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM comments LEFT JOIN channels USING (channel_id) " +
                    "WHERE comments.video_id IN (SELECT video_id FROM videos JOIN gitem_video USING (video_id) JOIN group_gitem USING (gitem_id) WHERE "+(gitem != null ? "gitem_id = ?":"group_id = ?")+" ) " +
                    "AND channel_name LIKE ? AND comment_text LIKE ? AND comment_date > ? AND comment_date < ? "+(ctype != 0 ? "AND is_reply = ? ":"")+"ORDER BY "+order[orderBy]);
            ps.setString(1, gitem != null ? gitem.getYouTubeId() : group.getId());
            ps.setString(2, "%"+nameLike+"%");
            ps.setString(3, "%"+textLike+"%");
            ps.setLong(4, after);
            ps.setLong(5, before);
            if(ctype != 0) ps.setBoolean(6, ctype == 2);
            ResultSet rs = ps.executeQuery();
            long start = limit * (page-1);
            long end = limit * page;
            long pos = 0;
            while(rs.next()) {
                if(pos >= start && pos <= end) {
                    items.add(resultSetToComment(rs));
                    String channelId = rs.getString("channel_id");
                    if(!channelCache.containsKey(channelId)) {
                        channelCache.put(channelId, resultSetToChannel(rs));
                    }
                }
            }
            ps.close();
            return items;
        }
    }
}
