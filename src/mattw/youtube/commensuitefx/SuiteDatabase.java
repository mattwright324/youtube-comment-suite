package mattw.youtube.commensuitefx;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuiteDatabase {
	
	public String dbfile;
	public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
	
	public Connection con;
	public Statement s;
	
	public SuiteDatabase(String db) {
		dbfile = db;
	}
	
	public void create() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		if(con != null) con.close();
		con = DriverManager.getConnection("jdbc:sqlite:"+dbfile);
		s = con.createStatement();
		setup();
	}
	
	public void setup() throws SQLException {
		s.executeUpdate("CREATE TABLE IF NOT EXISTS gitem_type (type_id INTEGER PRIMARY KEY, name STRING);");
		s.executeUpdate("INSERT OR IGNORE INTO gitem_type VALUES (0, 'video'),(1, 'channel'),(2, 'playlist');");
		
		s.executeUpdate("CREATE TABLE IF NOT EXISTS gitem_list ("
				+ "gitem_id INTEGER PRIMARY KEY,"
				+ "type_id INTEGER,"
				+ "youtube_id STRING UNIQUE," // Could be video_id, channel_id, or playlist_id
				+ "title STRING,"
				+ "channel_title STRING,"
				+ "published DATE,"
				+ "last_checked DATE,"
				+ "thumb_url STRING, "
				+ "FOREIGN KEY(type_id) REFERENCES gitem_type(type_id));");
		
		s.executeUpdate("CREATE TABLE IF NOT EXISTS groups (group_id INTEGER PRIMARY KEY AUTOINCREMENT, group_name STRING UNIQUE);");
		
		s.executeUpdate("INSERT OR IGNORE INTO groups VALUES (0, 'Default');");
		
		s.executeUpdate("CREATE TABLE IF NOT EXISTS group_gitem ("
				+ "group_id INTEGER,"
				+ "gitem_id INTEGER,"
				+ "FOREIGN KEY(group_id) REFERENCES groups(group_id),"
				+ "FOREIGN KEY(gitem_id) REFERENCES gitem_list(gitem_id));");
		
		s.executeUpdate("CREATE TABLE IF NOT EXISTS video_group ("
				+ "gitem_id INTEGER,"
				+ "video_id STRING,"
				+ "FOREIGN KEY(gitem_id) REFERENCES gitem_list(gitem_id),"
				+ "FOREIGN KEY(video_id) REFERENCES videos(video_id));");
		
		s.executeUpdate("CREATE TABLE IF NOT EXISTS videos ("
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
		
		s.executeUpdate("CREATE TABLE IF NOT EXISTS comments ("
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
		
		s.executeUpdate("CREATE TABLE IF NOT EXISTS channels ("
				+ "channel_id STRING PRIMARY KEY,"
				+ "channel_name STRING,"
				+ "channel_profile_url STRING,"
				+ "download_profile BOOLEAN)");
	}
	
	public void clean() throws SQLException {
		if(!con.getAutoCommit()) {
			con.commit();
			con.setAutoCommit(true);
		}
		s.execute("VACUUM");
	}
	
	public void dropAllTables() throws SQLException {
		if(!con.getAutoCommit()) {
			con.commit();
			con.setAutoCommit(true);
		}
		for(String table : "gitem_type,gitem_list,groups,group_gitem,video_group,videos,comments,channels".split(","))
			s.executeUpdate("DROP TABLE IF EXISTS "+table);
	}
	
	/* TODO
	 * COMMENTS TABLE
	 */
	
	public void insertComment(Comment c) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO comments (comment_id, channel_id, video_id, comment_date, comment_text, comment_likes, reply_count, is_reply, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
		if(c != null && ps != null) {
			ps.setString(1, c.comment_id);
			ps.setString(2, c.channel.channel_id);
			ps.setString(3, c.video_id);
			ps.setLong(4, c.comment_date.getTime());
			ps.setString(5, c.comment_text);
			ps.setLong(6, c.comment_likes);
			ps.setLong(7, c.reply_count);
			ps.setBoolean(8, c.is_reply);
			ps.setString(9, c.parent_id);
			ps.executeUpdate();
			System.out.println("Inserted 1 comment");
		}
		ps.close();
	}
	
	public void insertComments(List<Comment> comments) throws SQLException {
		System.out.println("Inserting "+comments.size()+" comments");
		PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO comments (comment_id, channel_id, video_id, comment_date, comment_text, comment_likes, reply_count, is_reply, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
		for(Comment c : comments) {
			if(c != null && ps != null) {
				ps.setString(1, c.comment_id);
				ps.setString(2, c.channel.channel_id);
				ps.setString(3, c.video_id);
				ps.setLong(4, c.comment_date.getTime());
				ps.setString(5, c.comment_text);
				ps.setLong(6, c.comment_likes);
				ps.setLong(7, c.reply_count);
				ps.setBoolean(8, c.is_reply);
				ps.setString(9, c.parent_id);
				ps.addBatch();
			} else {
				System.out.println("NULL VALUE ON COMMENT INSERT c:"+(c==null)+",ps:"+(ps==null)+"");
			}
		}
		ps.executeBatch();
		System.out.println("Inserted "+comments.size()+" comments");
		ps.close();
	}
	
	public List<String> getCommentIDs(String group_name) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT comment_id FROM comments "
				+ "WHERE comments.video_id IN ( "
				+ "    SELECT video_id FROM video_group "
				+ "    LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id "
				+ "    LEFT JOIN groups ON groups.group_id = group_gitem.group_id "
				+ "    WHERE group_name = ?)");
		ps.setString(1, group_name);
		ResultSet rs = ps.executeQuery();
		
		List<String> list = new ArrayList<String>();
		while(rs.next()) {
			list.add(rs.getString("comment_id"));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public Map<String,Long> getCommentThreadReplies(String group_name) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT comment_id, reply_count FROM comments "
				+ "WHERE comments.video_id IN ( "
				+ "    SELECT video_id FROM video_group "
				+ "    LEFT JOIN group_gitem USING (gitem_id) "
				+ "    LEFT JOIN groups USING (group_id) "
				+ "    WHERE group_name = ?)"
				+ "AND is_reply = ?");
		ps.setString(1, group_name);
		ps.setBoolean(2, false);
		ResultSet rs = ps.executeQuery();
		Map<String,Long> list = new HashMap<String,Long>();
		while(rs.next()) {
			list.put(rs.getString("comment_id"), rs.getLong("reply_count"));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public List<Comment> getCommentTree(String commentId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM comments "
				+ "LEFT JOIN channels ON channels.channel_id = comments.channel_id "
				+ "WHERE comment_id = ? OR parent_id = ? "
				+ "ORDER BY is_reply ASC, comment_date ASC");
		ps.setString(1, commentId);
		ps.setString(2, commentId);
		ResultSet rs = ps.executeQuery();
		
		List<Comment> list = new ArrayList<Comment>();
		Map<String, Channel> channels = new HashMap<String, Channel>();
		while(rs.next()) {
			Channel author;
			if(channels.containsKey(rs.getString("channel_id"))) {
				author = channels.get(rs.getString("channel_id"));
			} else {
				author = new Channel(rs.getString("channel_id"), rs.getString("channel_name"), rs.getString("channel_profile_url"), rs.getBoolean("download_profile"));
			}
			Comment comment = new Comment(rs.getString("comment_id"), author, rs.getString("video_id"), new Date(rs.getLong("comment_date")), rs.getString("comment_text"), rs.getLong("comment_likes"), rs.getLong("reply_count"), rs.getBoolean("is_reply"), rs.getString("parent_id"));
			list.add(comment);
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public CommentSearch getComments(String group_name, int orderby, String name_like, String text_like, int limit, GroupItem gitem, int type) throws SQLException {
		return getComments(group_name, orderby, name_like, text_like, limit, gitem, type, false, 0, false);
	}
	
	public CommentSearch getComments(String group_name, int orderby, String name_like, String text_like, int limit, GroupItem gitem, int type, boolean random, int rlimit, boolean fair) throws SQLException {
		String order = "comment_date DESC ";
		if(orderby == 1) order = "comment_date ASC ";
		if(orderby == 2) order = "comment_likes DESC ";
		if(orderby == 3) order = "reply_count DESC ";
		if(orderby == 4) order = "LENGTH(comment_text) DESC ";
		if(orderby == 5) order = "channel_name ASC, comment_date DESC ";
		if(orderby == 6) order = "comment_text ASC ";
		
		String ctype = "";
		if(type == 1) ctype = " AND is_reply = 0 ";
		if(type == 2) ctype = " AND is_reply = 1 ";
		
		String stmt;
		if(!random) {
			stmt = "SELECT * FROM comments "
					+ "LEFT JOIN channels ON channels.channel_id = comments.channel_id "
					+ "WHERE comments.video_id IN ("
					+ "    SELECT video_id FROM video_group "
					+ "    LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id "
					+ "    LEFT JOIN groups ON group_gitem.group_id = groups.group_id "
					+ "    WHERE "
					+     (gitem == null ? 
							"group_name = ? "
							:"video_group.gitem_id = ? ")
					+ ") AND channel_name LIKE ? AND comment_text LIKE ? "+ctype
					+ "ORDER BY "+order;
		} else {
			if(!fair) {
				stmt = "SELECT * FROM comments "
						+ "LEFT JOIN channels ON channels.channel_id = comments.channel_id "
						+ "WHERE comment_id IN ("
						+ "    SELECT comment_id FROM comments "
						+ "    LEFT JOIN channels ON channels.channel_id = comments.channel_id "
						+ "    LEFT JOIN video_group ON comments.video_id = video_group.video_id "
						+ "    LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id "
						+ "    LEFT JOIN groups ON group_gitem.group_id = groups.group_id "
						+ "    WHERE "
						+ (gitem == null ? 
								"group_name = ? "
								:"video_group.gitem_id = ? ")
						+ "    AND channel_name LIKE ? AND comment_text LIKE ? "+ctype
						+ "    ORDER BY RANDOM()"
						+ "    LIMIT "+rlimit+") "
						+ "ORDER BY "+order;
			} else {
				stmt = "";
			}
		}
		System.out.println(stmt);
		PreparedStatement ps = con.prepareStatement(stmt);
		if(gitem == null) {ps.setString(1, group_name);} else {ps.setInt(1, gitem.gitem_id);}
		ps.setString(2, "%"+name_like+"%");
		ps.setString(3, "%"+text_like+"%");
		
		ResultSet rs = ps.executeQuery();
		
		int count = 0;
		List<Comment> list = new ArrayList<Comment>();
		Map<String, Channel> channels = new HashMap<String, Channel>();
		while(rs.next()) {
			if(count < limit) {
				Channel author;
				if(channels.containsKey(rs.getString("channel_id"))) {
					author = channels.get(rs.getString("channel_id"));
				} else {
					author = new Channel(rs.getString("channel_id"), rs.getString("channel_name"), rs.getString("channel_profile_url"), rs.getBoolean("download_profile"));
				}
				Comment comment = new Comment(rs.getString("comment_id"), author, rs.getString("video_id"), new Date(rs.getLong("comment_date")), rs.getString("comment_text"), rs.getLong("comment_likes"), rs.getLong("reply_count"), rs.getBoolean("is_reply"), rs.getString("parent_id"));
				list.add(comment);
			}
			count++;
		}
		ps.close();
		rs.close();
		return new CommentSearch(count, list);
	}
	
	/* TODO
	 * CHANNELS TABLE
	 */
	
	public void insertChannel(Channel channel) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO channels (channel_id, channel_name, channel_profile_url, download_profile) VALUES (?, ?, ?, ?)");
		if(channel != null && ps != null) {
			ps.setString(1, channel.channel_id);
			ps.setString(2, channel.channel_name);
			ps.setString(3, channel.channel_profile_url);
			ps.setBoolean(4, channel.download_profile);
			ps.executeUpdate();
			System.out.println("Inserted 1 channel");
		}
		ps.close();
	}
	
	public void insertChannels(List<Channel> channels) throws SQLException {
		System.out.println("Inserting "+channels.size()+" channels");
		PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO channels (channel_id, channel_name, channel_profile_url, download_profile) VALUES (?, ?, ?, ?)");
		for(Channel c : channels) {
			if(c != null && ps != null) {
				ps.setString(1, c.channel_id);
				ps.setString(2, c.channel_name);
				ps.setString(3, c.channel_profile_url);
				ps.setBoolean(4, c.download_profile);
			} else {
				System.out.println("NULL VALUE ON CHANNEL INSERT c:"+(c==null)+",ps:"+(ps==null)+"");
			}
			
			ps.addBatch();
		}
		ps.executeBatch();
		System.out.println("Inserted "+channels.size()+" channels");
		ps.close();
	}
	
	public void updateChannels(List<Channel> channels) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE channels SET "
				+ "channel_name = ?, "
				+ "channel_profile_url = ?, "
				+ "download_profile = ? "
				+ "WHERE channel_id = ?");
		for(Channel c : channels) {
			ps.setString(1, c.channel_name);
			ps.setString(2, c.channel_profile_url);
			ps.setBoolean(3, c.download_profile);
			ps.setString(4, c.channel_id);
			ps.addBatch();
		}
		System.out.println("Updating "+channels.size()+" channels");
		ps.executeBatch();
		System.out.println("Updated "+channels.size()+" channels");
		ps.close();
	}
	
	public boolean hasChannel(String channelId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) AS count FROM channels WHERE channel_id = ?");
		ps.setString(1, channelId);
		ResultSet rs = ps.executeQuery();
		if(rs.next()) {
			return rs.getInt("count") >= 1;
		}
		return false;
	}
	
	public List<String> getAllChannelIDs() throws SQLException {
		ResultSet rs = s.executeQuery("SELECT DISTINCT channel_id FROM channels");
		
		List<String> list = new ArrayList<String>();
		while(rs.next()) {
			list.add(rs.getString("channel_id"));
		}
		rs.close();
		return list;
	}
	
	
	/* TODO
	 * VIDEOS TABLE
	 */
	
	public void insertVideos(List<Video> videos) throws SQLException {
		System.out.println("Inserting "+videos.size()+" videos");
		PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO videos (video_id, channel_id, grab_date, publish_date, video_title, total_comments, total_views, total_likes, total_dislikes, video_desc, thumb_url, http_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		for(Video v : videos) {
			ps.setString(1, v.video_id);
			ps.setString(2, v.channel.channel_id);
			ps.setLong(3, v.grab_date.getTime());
			ps.setLong(4, v.publish_date.getTime());
			ps.setString(5, v.video_title);
			ps.setLong(6, v.total_comments);
			ps.setLong(7, v.total_views);
			ps.setLong(8, v.total_likes);
			ps.setLong(9, v.total_dislikes);
			ps.setString(10, v.video_desc);
			ps.setString(11, v.thumb_url);
			ps.setInt(12, v.http_code);
			ps.addBatch();
		}
		ps.executeBatch();
		System.out.println("Inserted "+videos.size()+" videos");
		ps.close();
	}
	
	public void updateVideos(List<Video> videos) throws SQLException {
		System.out.println("Updating "+videos.size()+" videos");
		PreparedStatement ps = con.prepareStatement("UPDATE videos SET grab_date = ?, "
				+ "video_title = ?, "
				+ "total_comments = ?, "
				+ "total_views = ?, "
				+ "total_likes = ?, "
				+ "total_dislikes = ?, "
				+ "video_desc = ?, "
				+ "thumb_url = ? "
				+ "WHERE video_id = ?");
		for(Video v : videos) {
			ps.setLong(1, System.currentTimeMillis());
			ps.setString(2, v.video_title);
			ps.setLong(3, v.total_comments);
			ps.setLong(4, v.total_views);
			ps.setLong(5, v.total_likes);
			ps.setLong(6, v.total_dislikes);
			ps.setString(7, v.video_desc);
			ps.setString(8, v.thumb_url);
			ps.setString(9, v.video_id);
			ps.addBatch();
		}
		ps.executeBatch();
		System.out.println("Updated "+videos.size()+" videos");
		ps.close();
	}
	
	public List<String> getAllVideoIds() throws SQLException {
		ResultSet rs = s.executeQuery("SELECT video_id FROM videos");
		List<String> list = new ArrayList<String>();
		while(rs.next()) {
			list.add(rs.getString("video_id"));
		}
		rs.close();
		return list;
	}
	
	public List<String> getVideoIds(String group_name) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos "
				+ "LEFT JOIN channels ON channels.channel_id = videos.channel_id "
				+ "WHERE videos.video_id IN ( "
				+ "    SELECT video_id FROM video_group "
				+ "    LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id "
				+ "    LEFT JOIN groups ON groups.group_id = group_gitem.group_id "
				+ "    WHERE group_name = ?) ");
		ps.setString(1, group_name);
		ResultSet rs = ps.executeQuery();
		
		List<String> list = new ArrayList<String>();
		while(rs.next()) {
			list.add(rs.getString("video_id"));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public Video getVideo(String videoId, boolean needImage) throws SQLException, ParseException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos "
				+ "LEFT JOIN (SELECT video_id, count(video_id) as comment_count FROM videos WHERE video_id = ?) AS cc ON cc.video_id = videos.video_id "
				+ "LEFT JOIN channels ON channels.channel_id = videos.channel_id "
				+ "WHERE videos.video_id = ?");
		ps.setString(1, videoId);
		ps.setString(2, videoId);
		ResultSet rs = ps.executeQuery();
		if(rs.next()) {
			Channel author = new Channel(rs.getString("channel_id"), rs.getString("channel_name"), rs.getString("channel_profile_url"), rs.getBoolean("download_profile"));
			Video video = new Video(rs.getString("video_id"), author, new Date(rs.getLong("grab_date")), new Date(rs.getLong("publish_date")), rs.getString("video_title"), rs.getString("video_desc"), rs.getLong("total_comments"), rs.getLong("total_likes"), rs.getLong("total_dislikes"), rs.getLong("total_views"), rs.getString("thumb_url"), rs.getInt("http_code"), needImage);
			video.setCommentCount(rs.getLong("comment_count"));
			return video;
		}
		ps.close();
		rs.close();
		return null;
	}
	
	public List<Video> getVideos(String group_name, boolean needImage) throws SQLException, ParseException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos "
				+ "LEFT JOIN (SELECT videos.video_id, count(videos.video_id) as comment_count FROM videos "
				+ "    LEFT JOIN comments on videos.video_id = comments.video_id "
				+ "    GROUP BY videos.video_id "
				+ "    ORDER BY comment_count DESC) AS cc ON cc.video_id = videos.video_id "
				+ "LEFT JOIN channels ON channels.channel_id = videos.channel_id "
				+ "WHERE videos.video_id IN ( "
				+ "    SELECT video_id FROM video_group "
				+ "    LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id "
				+ "    LEFT JOIN groups ON groups.group_id = group_gitem.group_id "
				+ "    WHERE group_name = ?) "
				+ "ORDER BY videos.publish_date DESC");
		ps.setString(1, group_name);
		ResultSet rs = ps.executeQuery();
		
		List<Video> list = new ArrayList<Video>();
		Map<String, Channel> channels = new HashMap<String, Channel>();
		while(rs.next()) {
			Channel author;
			if(channels.containsKey(rs.getString("channel_id"))) {
				author = channels.get(rs.getString("channel_id"));
			} else {
				author = new Channel(rs.getString("channel_id"), rs.getString("channel_name"), rs.getString("channel_profile_url"), rs.getBoolean("download_profile"));
				channels.put(rs.getString("channel_id"), author);
			}
			Video video = new Video(rs.getString("video_id"), author, new Date(rs.getLong("grab_date")), new Date(rs.getLong("publish_date")), rs.getString("video_title"), rs.getString("video_desc"), rs.getLong("total_comments"), rs.getLong("total_likes"), rs.getLong("total_dislikes"), rs.getLong("total_views"), rs.getString("thumb_url"), rs.getInt("http_code"), needImage);
			video.setCommentCount(rs.getLong("comment_count"));
			list.add(video);
		}
		rs.close();
		ps.close();
		return list;
	}
	
	
	public void updateVideoHttpCode(String video_id, int code) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE videos SET http_code = ? WHERE video_id = ?");
		ps.setInt(1, code);
		ps.setString(2, video_id);
		ps.executeUpdate();
		ps.close();
	}
	
	/* TODO
	 * GROUPS TABLE
	 */
	
	public void createGroup(String name) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT INTO groups (group_name) VALUES (?)");
		ps.setString(1, name);
		ps.executeUpdate();
		System.out.println("Created new group ["+name+"]");
		ps.close();
	}
	
	public void deleteGroup(String group_name) throws SQLException {
		deleteGroup(getGroup(group_name));
	}
	
	public void deleteGroup(Group g) throws SQLException {
		System.out.println("Grabbing relevant video_ids.");
		PreparedStatement ps = con.prepareStatement(""
				+ "WITH vlist AS ("
				+ "    SELECT video_id, group_id "
				+ "    FROM video_group "
				+ "    JOIN group_gitem USING (gitem_id)"
				+ ")"
				+ "SELECT video_id FROM videos "
				+ "WHERE video_id IN (SELECT video_id FROM vlist WHERE group_id = ?) AND video_id NOT IN (SELECT video_id FROM vlist WHERE group_id != ?)");
		ps.setInt(1, g.group_id);
		ps.setInt(2, g.group_id);
		ResultSet rs = ps.executeQuery();
		List<String> videos = new ArrayList<String>();
		
		System.out.println("Deleting video thumbnails, COMMENTS, VIDEO_GROUP, and VIDEOS entries.");
		PreparedStatement ps_vgroup = con.prepareStatement(""
				+ "WITH items AS ("
				+ "   SELECT gitem_id "
				+ "   FROM group_gitem "
				+ "   WHERE group_id = ?"
				+ ")"
				+ "DELETE FROM video_group WHERE video_id = ? AND gitem_id IN (SELECT gitem_id FROM items);");
		PreparedStatement ps_comments = con.prepareStatement("DELETE FROM comments WHERE video_id = ?;");
		PreparedStatement ps_video = con.prepareStatement("DELETE FROM videos WHERE video_id = ?");
		File thumbs = new File("Thumbs/");
		File thumbFile;
		while(rs.next()) {
			videos.add(rs.getString("video_id"));
			if((thumbFile = new File(thumbs, rs.getString("video_id")+".jpg")).exists()) {
				thumbFile.delete();
			}
			
			ps_vgroup.setInt(1, g.group_id);
			ps_vgroup.setString(2, rs.getString("video_id"));
			ps_comments.setString(1, rs.getString("video_id"));
			ps_video.setString(1, rs.getString("video_id"));
			
			ps_vgroup.addBatch();
			ps_comments.addBatch();
			ps_video.addBatch();
		}
		ps_vgroup.executeBatch();
		ps_comments.executeBatch();
		ps_video.executeBatch();
		
		System.out.println("Deleting GITEM_LIST, GROUP_GITEM, and GROUP entries.");
		PreparedStatement ps_gitem = con.prepareStatement("DELETE FROM gitem_list WHERE gitem_id IN (SELECT gitem_id FROM group_gitem WHERE group_id = ?);");
		PreparedStatement ps_ggitem = con.prepareStatement("DELETE FROM group_gitem WHERE group_id = ?;");
		ps_gitem.setInt(1, g.group_id);
		ps_ggitem.setInt(1, g.group_id);
		System.out.println(ps_gitem.executeUpdate());
		System.out.println(ps_ggitem.executeUpdate());
		if(g.group_id != 0) {
			PreparedStatement ps_group = con.prepareStatement("DELETE FROM groups WHERE group_id = ?;");
			ps_group.setInt(1, g.group_id);
			System.out.println(ps_group.executeUpdate());
			ps_group.close();
		}
		
		System.out.println("Deleting channel profile thumbs.");
		PreparedStatement ps1 = con.prepareStatement(""
				+ "WITH clist AS ("
				+ "    SELECT channel_id FROM comments"
				+ "    UNION"
				+ "    SELECT channel_id FROM videos"
				+ ")"
				+ "SELECT channel_id FROM channels WHERE channel_id NOT IN (SELECT channel_id FROM clist)");
		rs = ps1.executeQuery();
		while(rs.next()) {
			if((thumbFile = new File(thumbs, rs.getString("channel_id")+".jpg")).exists()) {
				thumbFile.delete();
			}
		}
		
		System.out.println("Deleting CHANNELS entries.");
		PreparedStatement ps_channels = con.prepareStatement("DELETE FROM channels WHERE channel_id NOT IN (SELECT channel_id FROM comments) AND channel_id NOT IN (SELECT channel_id FROM videos)");
		System.out.println(ps_channels.executeUpdate());
		
		ps_vgroup.close();
		ps_comments.close();
		ps_video.close();
		ps_gitem.close();
		ps_ggitem.close();
		ps_channels.close();
		ps1.close();
		ps.close();
		rs.close();
	}
	
	public Group getGroup(int group_id) throws SQLException {
		PreparedStatement q = con.prepareStatement("SELECT * FROM groups WHERE group_id = ?");
		q.setInt(1, group_id);
		ResultSet rs = q.executeQuery();
		if(rs.next()) {
			Group g = new Group(rs.getInt("group_id"), rs.getString("group_name"));
			q.close();
			rs.close();
			return g;
		} else {
			q.close();
			rs.close();
			return null;
		}
	}
	
	public Group getGroup(String group_name) throws SQLException {
		PreparedStatement q = con.prepareStatement("SELECT * FROM groups WHERE group_name = ?");
		q.setString(1, group_name);
		ResultSet rs = q.executeQuery();
		if(rs.next()) {
			Group g = new Group(rs.getInt("group_id"), rs.getString("group_name"));
			q.close();
			rs.close();
			return g;
		} else {
			q.close();
			rs.close();
			return null;
		}
	}
	
	public List<Group> getGroups() throws SQLException {
		List<Group> groups = new ArrayList<Group>();
		ResultSet rs = s.executeQuery("SELECT * FROM groups");
		while(rs.next()) {
			groups.add(new Group(rs.getInt("group_id"), rs.getString("group_name")));
		}
		rs.close();
		return groups;
	}
	
	public void editGroupName(String group_name, String new_name) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE groups SET group_name = ? WHERE group_name = ?");
		ps.setString(1, new_name);
		ps.setString(2, group_name);
		ps.executeUpdate();
		ps.close();
	}
	
	/* TODO
	 * GITEM_LIST TABLE
	 * GROUP_GITEM TABLE
	 */
	
	public void insertGroupItems(String group_name, List<GroupItem> items) throws SQLException {
		Group g = getGroup(group_name);
		if(g == null) g = getGroup(0);
		int gitem_id = s.executeQuery("SELECT MAX(gitem_id) AS max_id FROM gitem_list").getInt("max_id") + 1;
		System.out.println(gitem_id);
		PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO gitem_list (gitem_id, type_id, youtube_id, title, channel_title, published, last_checked, thumb_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
		PreparedStatement ps1 = con.prepareStatement("INSERT INTO group_gitem (group_id, gitem_id) VALUES (?, ?)");
		System.out.println("Inserting "+items.size()+" Group Items");
		for(GroupItem gi : items) {
			ps.setInt(1, gitem_id);
			ps.setInt(2, gi.type_id);
			ps.setString(3, gi.youtube_id);
			ps.setString(4, gi.title);
			ps.setString(5, gi.channel_title);
			ps.setLong(6, gi.published.getTime());
			ps.setLong(7, gi.last_checked.getTime());
			ps.setString(8, gi.thumb_url);
			ps.addBatch();
			
			ps1.setInt(1, g.group_id);
			ps1.setInt(2, gitem_id);
			ps1.addBatch();
			gitem_id++;
		}
		ps.executeBatch();
		ps1.executeBatch();
		System.out.println("Inserted "+items.size()+" Group Items");
		ps.close();
		ps1.close();
	}
	
	public void updateGroupItemsChecked(Collection<GroupItem> items, Date date) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE gitem_list SET last_checked = ? WHERE gitem_id = ?");
		for(GroupItem gi : items) {
			ps.setLong(1, date.getTime());
			ps.setInt(2, gi.gitem_id);
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
	}
	
	public void deleteGroupItems(Collection<GroupItem> items) throws SQLException {
		
	}
	
	public List<GroupItem> getGroupItems(String group_name, boolean needImage) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM gitem_list "
				+ "LEFT JOIN gitem_type ON gitem_type.type_id = gitem_list.type_id "
				+ "LEFT JOIN group_gitem ON group_gitem.gitem_id = gitem_list.gitem_id "
				+ "LEFT JOIN groups ON groups.group_id = group_gitem.group_id "
				+ "WHERE group_name = ?");
		ps.setString(1, group_name);
		ResultSet rs = ps.executeQuery();
		
		List<GroupItem> list = new ArrayList<GroupItem>();
		while(rs.next()) {
			GroupItem gi = new GroupItem(rs.getInt("type_id"), rs.getString("name"), rs.getString("youtube_id"), rs.getString("title"), rs.getString("channel_title"), new Date(rs.getLong("published")), new Date(rs.getLong("last_checked")), rs.getString("thumb_url"), needImage);
			gi.setID(rs.getInt("gitem_id"));
			list.add(gi);
		}
		rs.close();
		ps.close();
		return list;
	}
	
	/* TODO
	 * VIDEO_GROUP TABLE
	 */
	
	public void insertVideoGroups(List<VideoGroup> video_groups) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT INTO video_group (gitem_id, video_id) VALUES (?, ?)");
		for(VideoGroup vg : video_groups) {
			ps.setInt(1, vg.gitem_id);
			ps.setString(2, vg.video_id);
			ps.addBatch();
		}
		System.out.println("Inserting "+video_groups.size()+" video groups");
		ps.executeBatch();
		System.out.println("Inserted "+video_groups.size()+" video groups");
		ps.close();
		ps.close();
	}
	
	public List<VideoGroup> getVideoGroups() throws SQLException {
		List<VideoGroup> list = new ArrayList<VideoGroup>();
		ResultSet rs = s.executeQuery("SELECT * FROM video_group");
		while(rs.next()) {
			list.add(new VideoGroup(rs.getInt("gitem_id"), rs.getString("video_id")));
		}
		rs.close();
		return list;
	}
	
	
	/*
	 * TODO
	 * Table Analytics
	 */
	
	public List<String> getAnalytics(String group_name, GroupItem gi, int type) throws SQLException {
		List<String> output = new ArrayList<String>();
		if(type == 0) output.addAll(commentAnalytics(group_name, gi));
		if(type == 1) output.addAll(videoAnalytics(group_name, gi));
		return output;
	}
	
	public List<String> videoAnalytics(String group_name, GroupItem gi) throws SQLException {
		List<String> output = new ArrayList<String>();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
		
		output.add("<h1>Videos</h1>");
		
		String ps0_stmt = "SELECT AVG(total_comments) AS avg_comments, AVG(total_views) AS avg_views, AVG(total_likes) AS avg_likes, AVG(total_dislikes) AS avg_dislikes FROM videos "
				+ "LEFT JOIN (SELECT video_id, COUNT(video_id) AS comment_count FROM comments GROUP BY video_id) AS cc ON cc.video_id = videos.video_id "
				+ (gi.gitem_id == 0 ? 
					"WHERE videos.video_id IN (SELECT video_id FROM video_group LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id LEFT JOIN groups ON group_gitem.group_id = groups.group_id WHERE group_name = ?) "
					:"WHERE videos.video_id IN (SELECT video_id FROM video_group WHERE gitem_id = ?) ");
		PreparedStatement ps0 = con.prepareStatement(ps0_stmt);
		if(gi.gitem_id == 0) {ps0.setString(1, group_name);} else {ps0.setInt(1, gi.gitem_id);}
		ResultSet rs = ps0.executeQuery();
		output.add("<b>Basic Stats</b>");
		String table = "<table>";
		if(rs.next()) {
			table += "<tr>"
					+ "<td>Average comments per video</td>"
					+ "<td><b>"+rs.getLong("avg_comments")+"</b></td>"
				  + "</tr>";
			table += "<tr>"
					+ "<td>Average views per video</td>"
					+ "<td><b>"+rs.getLong("avg_views")+"</b></td>"
				  + "</tr>";
			table += "<tr>"
					+ "<td>Average likes / dislikes per video</td>"
					+ "<td><b><span color=green>+"+rs.getLong("avg_likes")+"</span> / <span color=red>-"+rs.getLong("avg_dislikes")+"</span></b></td>"
				  + "</tr>";
		}
		output.add(table += "</table>");
		
		String ps1_stmt = "SELECT videos.video_id, video_title, publish_date, comment_count, total_comments FROM videos "
				+ "LEFT JOIN (SELECT video_id, COUNT(video_id) AS comment_count FROM comments GROUP BY video_id) AS cc ON cc.video_id = videos.video_id "
				+ (gi.gitem_id == 0 ? 
					"WHERE videos.video_id IN (SELECT video_id FROM video_group LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id LEFT JOIN groups ON group_gitem.group_id = groups.group_id WHERE group_name = ?) "
					:"WHERE videos.video_id IN (SELECT video_id FROM video_group WHERE gitem_id = ?) ")
				+ "AND http_code = ? "
				+ "ORDER BY comment_count DESC "
				+ "LIMIT 5 ";
		PreparedStatement ps1 = con.prepareStatement(ps1_stmt);
		if(gi.gitem_id == 0) {ps1.setString(1, group_name);} else {ps1.setInt(1, gi.gitem_id);}
		ps1.setInt(2, 200);
		rs = ps1.executeQuery();
		output.add("<br><b>Most Commented Videos</b>");
		table = "<table>";
		while(rs.next()) {
			table += "<tr>"
					+ "<td>"+rs.getInt("comment_count")+" comments ("+(rs.getInt("total_comments")-rs.getInt("comment_count"))+" off)</td>"
					+ "<td><b>"+sdf.format(rs.getLong("publish_date"))+"</b></td>"
					+ "<td><a href='http://youtu.be/"+rs.getString("video_id")+"'>"+rs.getString("video_title")+"<a></td>"
				  + "</tr>";
		}
		output.add(table += "</table>");
		
		String ps2_stmt = "SELECT videos.video_id, video_title, publish_date, comment_count, total_comments FROM videos "
				+ "LEFT JOIN (SELECT video_id, COUNT(video_id) AS comment_count FROM comments GROUP BY video_id) AS cc ON cc.video_id = videos.video_id "
				+ (gi.gitem_id == 0 ? 
					"WHERE videos.video_id IN (SELECT video_id FROM video_group LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id LEFT JOIN groups ON group_gitem.group_id = groups.group_id WHERE group_name = ?) "
					:"WHERE videos.video_id IN (SELECT video_id FROM video_group WHERE gitem_id = ?) ")
				+ "AND http_code = ? "
				+ "ORDER BY comment_count ASC "
				+ "LIMIT 5 ";
		PreparedStatement ps2 = con.prepareStatement(ps2_stmt);
		if(gi.gitem_id == 0) {ps2.setString(1, group_name);} else {ps2.setInt(1, gi.gitem_id);}
		ps2.setInt(2, 200);
		rs = ps2.executeQuery();
		output.add("<br><b>Least Commented Videos</b>");
		table = "<table>";
		while(rs.next()) {
			table += "<tr>"
					+ "<td>"+rs.getInt("comment_count")+" comments ("+(rs.getInt("total_comments")-rs.getInt("comment_count"))+" off)</td>"
					+ "<td><b>"+sdf.format(rs.getLong("publish_date"))+"</b></td>"
					+ "<td><a href='http://youtu.be/"+rs.getString("video_id")+"'>"+rs.getString("video_title")+"<a></td>"
				  + "</tr>";
		}
		output.add(table += "</table>");
		
		String ps3_stmt = "SELECT videos.video_id, video_title, publish_date, total_likes, total_dislikes, total_views, total_comments FROM videos "
				+ (gi.gitem_id == 0 ? 
					"WHERE videos.video_id IN (SELECT video_id FROM video_group LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id LEFT JOIN groups ON group_gitem.group_id = groups.group_id WHERE group_name = ?) "
					:"WHERE videos.video_id IN (SELECT video_id FROM video_group WHERE gitem_id = ?) ")
				+ "AND http_code = ? "
				+ "ORDER BY total_views DESC, total_likes DESC, total_dislikes ASC "
				+ "LIMIT 5 ";
		PreparedStatement ps3 = con.prepareStatement(ps3_stmt);
		if(gi.gitem_id == 0) {ps3.setString(1, group_name);} else {ps3.setInt(1, gi.gitem_id);}
		ps3.setInt(2, 200);
		rs = ps3.executeQuery();
		output.add("<br><b>Most Popular Videos</b>");
		table = "<table>";
		while(rs.next()) {
			table += "<tr>"
					+ "<td>"+rs.getLong("total_views")+" views</td>"
					+ "<td><span color=green>+"+rs.getLong("total_likes")+"</span> / <span color=red>-"+rs.getLong("total_dislikes")+"</span></td>"
					+ "<td><b>"+sdf.format(rs.getLong("publish_date"))+"</b></td>"
					+ "<td><a href='http://youtu.be/"+rs.getString("video_id")+"'>"+rs.getString("video_title")+"<a></td>"
				  + "</tr>";
		}
		output.add(table += "</table>");
		
		String ps4_stmt = "SELECT videos.video_id, video_title, publish_date, total_likes, total_dislikes, total_views, total_comments FROM videos "
				+ (gi.gitem_id == 0 ? 
					"WHERE videos.video_id IN (SELECT video_id FROM video_group LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id LEFT JOIN groups ON group_gitem.group_id = groups.group_id WHERE group_name = ?) "
					:"WHERE videos.video_id IN (SELECT video_id FROM video_group WHERE gitem_id = ?) ")
				+ "AND http_code = ? "
				+ "ORDER BY total_dislikes DESC, total_likes ASC "
				+ "LIMIT 5 ";
		PreparedStatement ps4 = con.prepareStatement(ps4_stmt);
		if(gi.gitem_id == 0) {ps4.setString(1, group_name);} else {ps4.setInt(1, gi.gitem_id);}
		ps4.setInt(2, 200);
		rs = ps4.executeQuery();
		output.add("<br><b>Most Disliked Videos</b>");
		table = "<table>";
		while(rs.next()) {
			table += "<tr>"
					+ "<td>"+rs.getLong("total_views")+" views</td>"
					+ "<td><span color=green>+"+rs.getLong("total_likes")+"</span> / <span color=red>-"+rs.getLong("total_dislikes")+"</span></td>"
					+ "<td><b>"+sdf.format(rs.getLong("publish_date"))+"</b></td>"
					+ "<td><a href='http://youtu.be/"+rs.getString("video_id")+"'>"+rs.getString("video_title")+"<a></td>"
				  + "</tr>";
		}
		output.add(table += "</table>");
		
		String ps5_stmt = "SELECT videos.video_id, video_title, publish_date, http_code FROM videos "
				+ (gi.gitem_id == 0 ? 
					"WHERE videos.video_id IN (SELECT video_id FROM video_group LEFT JOIN group_gitem ON video_group.gitem_id = group_gitem.gitem_id LEFT JOIN groups ON group_gitem.group_id = groups.group_id WHERE group_name = ?) "
					:"WHERE videos.video_id IN (SELECT video_id FROM video_group WHERE gitem_id = ?) ")
				+ "AND http_code = ? "
				+ "ORDER BY publish_date DESC ";
		PreparedStatement ps5 = con.prepareStatement(ps5_stmt);
		if(gi.gitem_id == 0) {ps5.setString(1, group_name);} else {ps5.setInt(1, gi.gitem_id);}
		ps5.setInt(2, 403);
		rs = ps5.executeQuery();
		output.add("<br><b>Comments Disabled</b>");
		table = "<table>";
		int i = 1;
		while(rs.next()) {
			table += "<tr>"
					+ "<td>"+i+".</td>"
					+ "<td><b>"+sdf.format(rs.getLong("publish_date"))+"</b></td>"
					+ "<td><a href='http://youtu.be/"+rs.getString("video_id")+"'>"+rs.getString("video_title")+"<a></td>"
				  + "</tr>";
			i++;
		}
		output.add((table += "</table>").equals("<table></table>") ? "<br>Nothing disabled." : table);
		
		return output;
	}
	
	public List<String> commentAnalytics(String group_name, GroupItem gi) throws SQLException {
		List<String> output = new ArrayList<String>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
		output.add("<h1>Comments</h1>");
		
		String ps1_stmt = "SELECT channels.channel_id, channel_name, comment_count, comment_date FROM channels "
				+ "LEFT JOIN ("
				+ "    SELECT channel_id, COUNT(channel_id) AS comment_count, MAX(comment_date) AS comment_date FROM comments "
				+ "    WHERE video_id IN ("
				+ "        SELECT video_id FROM video_group "
				+ (gi.gitem_id == 0 ? 
						  "LEFT JOIN group_gitem ON group_gitem.gitem_id = video_group.gitem_id LEFT JOIN groups ON groups.group_id = group_gitem.group_id WHERE group_name = ? "
						  :"WHERE video_group.gitem_id = ? ")
				+ "    ) GROUP BY channel_id "
				+ ") AS cc ON cc.channel_id = channels.channel_id "
				+ "WHERE comment_count > 0 "
				+ "ORDER BY comment_count DESC "
				+ "LIMIT ?";
		PreparedStatement ps1 = con.prepareStatement(ps1_stmt);
		if(gi.gitem_id == 0) {ps1.setString(1, group_name);} else {ps1.setInt(1, gi.gitem_id);}
		ps1.setInt(2, 10);
		ResultSet rs = ps1.executeQuery();
		output.add("<b>Most Active Viewers</b>");
		String table = "<table>";
		while(rs.next()) {
			table += "<tr>"
					+ "<td>"+rs.getInt("comment_count")+" comments</td>"
					+ "<td><a href='http://www.youtube.com/channel/"+rs.getString("channel_id")+"'>"+rs.getString("channel_name")+"<a></td>"
					+ "<td><i>Last commented on "+sdf.format(new Date(rs.getLong("comment_date")))+"</i></td>"
				  + "</tr>";
		}
		output.add(table += "</table>");
		
		String ps2_stmt = "SELECT channels.channel_id, channel_name, comment_count, total_comment_likes FROM channels "
				+ "LEFT JOIN ("
				+ "    SELECT channel_id, COUNT(channel_id) AS comment_count, SUM(comment_likes) AS total_comment_likes FROM comments "
				+ "    WHERE video_id IN (SELECT video_id FROM video_group "
				+ (gi.gitem_id == 0 ? 
				         "LEFT JOIN group_gitem ON group_gitem.gitem_id = video_group.gitem_id LEFT JOIN groups ON groups.group_id = group_gitem.group_id WHERE group_name = ? "
						 :"WHERE video_group.gitem_id = ? ")
				+ "    ) GROUP BY channel_id "
				+ ") AS cc ON cc.channel_id = channels.channel_id "
				+ "WHERE comment_count > 0 "
				+ "ORDER BY total_comment_likes DESC "
				+ "LIMIT ?";
		PreparedStatement ps2 = con.prepareStatement(ps2_stmt);
		if(gi.gitem_id == 0) {ps2.setString(1, group_name);} else {ps2.setInt(1, gi.gitem_id);}
		ps2.setInt(2, 10);
		rs = ps2.executeQuery();
		output.add("<br><b>Most Popular Viewers</b>");
		table = "<table>";
		while(rs.next()) {
			table += "<tr>"
					+ "<td>"+rs.getLong("total_comment_likes")+" likes over</td>"
					+ "<td>"+rs.getInt("comment_count")+" comments</td>"
					+ "<td><a href='http://www.youtube.com/channel/"+rs.getString("channel_id")+"'>"+rs.getString("channel_name")+"<a></td>"
				  + "</tr>";
		}
		output.add(table += "</table>");
		
		String ps3_stmt = "SELECT comment_text, COUNT(LOWER(comment_text)) AS comment_count, MAX(comment_date) AS comment_date FROM comments "
				+ "WHERE video_id IN (SELECT video_id FROM video_group "
				+ (gi.gitem_id == 0 ? 
						"LEFT JOIN group_gitem ON group_gitem.gitem_id = video_group.gitem_id LEFT JOIN groups ON groups.group_id = group_gitem.group_id WHERE group_name = ? "
						:"WHERE video_group.gitem_id = ? ")
				+ ") GROUP BY LOWER(comment_text) "
				+ "ORDER BY comment_count DESC "
				+ "LIMIT ?";
		PreparedStatement ps3 = con.prepareStatement(ps3_stmt);
		if(gi.gitem_id == 0) {ps3.setString(1, group_name);} else {ps3.setInt(1, gi.gitem_id);}
		ps3.setInt(2, 10);
		rs = ps3.executeQuery();
		output.add("<br><b>Most Common Comments</b> <i>Ignore case, full comment.</i>");
		table = "<table>";
		while(rs.next()) {
			table += "<tr>"
					+ "<td>"+rs.getInt("comment_count")+" occurances</td>"
					+ "<td>"+rs.getString("comment_text")+"</td>"
					+ "<td><i>Last occured "+sdf.format(new Date(rs.getLong("comment_date")))+"</i></td>"
				  + "</tr>";
		}
		output.add(table += "</table>");
		
		return output;
	}
}
