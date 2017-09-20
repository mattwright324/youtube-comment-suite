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

    public final ObservableList<Group> groupsList = FXCollections.observableArrayList();
    public final Map<String,YouTubeChannel> channelCache = new HashMap<>();

    public CommentDatabase(String dbfile) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:"+dbfile);
        con.setAutoCommit(false);
        create();
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
        for(int i=0; i<groupsList.size(); i++) {
            if(!groups.contains(groupsList.get(i))) {
                groupsList.remove(i);
            }
        }
        for(Group g : groups) {
            if(!groupsList.contains(g)) {
                groupsList.add(g);
            }
        }
        if(groupsList.isEmpty()) {
            groupsList.add(noGroup);
        }
    }

    public void create() throws SQLException {
        Statement s = con.createStatement();
        s.addBatch("CREATE TABLE IF NOT EXISTS gitem_type (type_id INTEGER PRIMARY KEY, nameProperty STRING);");
        s.addBatch("INSERT OR IGNORE INTO gitem_type VALUES (0, 'video'),(1, 'channel'),(2, 'playlist');");
        s.addBatch("CREATE TABLE IF NOT EXISTS gitem_list ("
                + "gitem_id STRING PRIMARY KEY,"
                + "type_id INTEGER,"
                + "youtube_id STRING UNIQUE,"
                + "title STRING,"
                + "channel_title STRING,"
                + "published DATE,"
                + "last_checked DATE,"
                + "thumb_url STRING, "
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

    private Group resultSetToGroup(ResultSet rs) throws SQLException {
        return new Group(rs.getString("group_id"), rs.getString("group_name"));
    }

    private GroupItem resultSetToGroupItem(ResultSet rs) throws SQLException {
        return new GroupItem(rs.getString("gitem_id"),
                rs.getInt("type_id"),
                rs.getString("youtube_id"),
                rs.getString("title"),
                rs.getString("channel_title"),
                rs.getString("thumb_url"),
                rs.getLong("published"),
                rs.getLong("last_checked"));
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
     * Commits and refreshes groupsList.
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
}
