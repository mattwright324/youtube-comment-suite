package mattw.youtube.commentsuite;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class CommentDatabase {

    private final Connection con;

    private final ObservableList<Group> groupsList = FXCollections.observableArrayList();
    private final ObservableList<YouTubeAccount> accountsList = FXCollections.observableArrayList();
    private final Map<String,YouTubeChannel> channelCache = new HashMap<>();

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
                + "gitem_id STRING PRIMARY KEY,"
                + "type_id INTEGER,"
                + "youtube_id STRING UNIQUE,"
                + "title STRING,"
                + "channel_title STRING,"
                + "published DATE,"
                + "last_checked DATE,"
                + "thumb_url STRING, "
                + "FOREIGN KEY(type_id) REFERENCES gitem_type(type_id));");
        s.addBatch("CREATE TABLE IF NOT EXISTS groups (group_id INTEGER PRIMARY KEY AUTOINCREMENT, group_name STRING UNIQUE);");
        s.addBatch("INSERT OR IGNORE INTO groups VALUES (0, 'Default');");
        s.addBatch("CREATE TABLE IF NOT EXISTS group_gitem ("
                + "group_id INTEGER,"
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
}
