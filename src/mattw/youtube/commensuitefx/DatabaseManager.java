package mattw.youtube.commensuitefx;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
	
	final private Connection con;
	final private Statement s;
	
	final public static Map<String, ChannelType> channelCache = new HashMap<>();
	
	public DatabaseManager() throws SQLException, ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		con = DriverManager.getConnection("jdbc:sqlite:commentsuite.db");
		s = con.createStatement();
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
	
	public Connection getConnection() {
		return con;
	}
	
	public Statement getStatement() {
		return s;
	}
	
	public void commit() throws SQLException {
		con.commit();
	}
	
	public void setAutoCommit(boolean b) throws SQLException {
		con.setAutoCommit(b);
	}
	
	public static boolean isChannelLoaded(String channelId) {
		return channelCache.containsKey(channelId);
	}
	
	public static ChannelType getChannel(String channelId) {
		return channelCache.get(channelId);
	}
	
	public static void clearChannelCache() {
		channelCache.clear();
	}
	
	/*
	 * TODO
	 * Removes all data related to a group and the thumbnails associated.
	 */
	private void deleteUnlinkedChannels() throws SQLException {
		Statement s = con.createStatement();
		s.executeUpdate("WITH clist AS (SELECT DISTINCT channel_id FROM videos UNION SELECT channel_id FROM comments) DELETE FROM channels WHERE channel_id NOT IN clist");
		s.close();
	}
	
	public void removeGroupAndData(Group group) throws SQLException {
		List<String> ids = getUniqueVideoIds(group.group_id);
		PreparedStatement delVid = con.prepareStatement("DELETE FROM videos WHERE video_id = ?");
		PreparedStatement delComs = con.prepareStatement("DELETE FROM comments WHERE video_id = ?");
		for(String videoId : ids) {
			File f = new File("Thumbs\\"+videoId+".jpg");
			if(f.exists()) {
				f.delete();
			}
			delVid.setString(1, videoId);
			delVid.addBatch();
			delComs.setString(1, videoId);
			delComs.addBatch();
		}
		delVid.executeBatch();
		delVid.close();
		delComs.executeBatch();
		delComs.close();
		deleteUnlinkedChannels();
		deleteGroup(group.group_id);
		deleteUniqueGitems(group.group_id);
	}
	
	private void deleteUniqueGitems(int groupId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("WITH glist AS (SELECT gitem_id, group_id FROM gitem_list JOIN group_gitem USING (gitem_id)) "
				+ "DELETE FROM gitem_list WHERE gitem_id IN (SELECT gitem_id FROM glist WHERE group_id = ?) "
				+ "AND gitem_id NOT IN (SELECT gitem_id FROM glist WHERE group_id != ?)");
		ps.setInt(1, groupId);
		ps.setInt(2, groupId);
		ps.executeUpdate();
		ps.close();
	}
	
	private List<String> getUniqueVideoIds(int groupId) throws SQLException {
		List<String> ids = new ArrayList<>();
		PreparedStatement ps = con.prepareStatement("WITH vlist AS (SELECT video_id, group_id FROM video_group JOIN group_gitem USING (gitem_id)) "
				+ "SELECT video_id FROM videos WHERE video_id IN (SELECT video_id FROM vlist WHERE group_id = ?) "
				+ "AND video_id NOT IN (SELECT video_id FROM vlist WHERE group_id != ?)");
		ps.setInt(1, groupId);
		ps.setInt(2, groupId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			ids.add(rs.getString("video_id"));
		}
		rs.close();
		ps.close();
		return ids;
	}
	
	public void dropTables() throws SQLException {
		if(!con.getAutoCommit()) {
			con.commit();
			con.setAutoCommit(true);
		}
		for(String table : "gitem_type,gitem_list,groups,group_gitem,video_group,videos,comments,channels".split(","))
			s.executeUpdate("DROP TABLE IF EXISTS "+table);
	}
	
	public void clean() throws SQLException {
		if(!con.getAutoCommit()) {
			con.commit();
			con.setAutoCommit(true);
		}
		s.execute("VACUUM");
	}
	
	private CommentType resultSetToComment(ResultSet rs) throws SQLException {
		return new CommentType(rs.getString("comment_id"), rs.getString("video_id"), rs.getString("channel_id"), rs.getString("comment_text"), new Date(rs.getLong("comment_date")), rs.getInt("comment_likes"), rs.getInt("reply_count"), rs.getBoolean("is_reply"), rs.getString("parent_id"));
	}
	
	private ChannelType resultSetToChannel(ResultSet rs) throws SQLException {
		return new ChannelType(rs.getString("channel_id"), rs.getString("channel_name"), rs.getString("channel_profile_url"), rs.getBoolean("download_profile"));
	}
	
	private VideoType resultSetToVideo(ResultSet rs, boolean fetchThumb) throws SQLException {
		return new VideoType(rs.getString("video_id"), rs.getString("channel_id"), rs.getString("video_title"), rs.getString("thumb_url"), fetchThumb, rs.getString("video_desc"), 
				rs.getLong("total_comments"), rs.getLong("total_likes"), rs.getLong("total_dislikes"), rs.getLong("total_views"), new Date(rs.getLong("publish_date")), 
				new Date(rs.getLong("grab_date")), rs.getInt("http_code"));
	}
	
	private GitemType resultSetToGitem(ResultSet rs, boolean fetchThumb) throws SQLException {
		return new GitemType(rs.getInt("type_id"), rs.getInt("gitem_id"), rs.getString("youtube_id"), rs.getString("title"), rs.getString("channel_title"), rs.getString("thumb_url"), fetchThumb, new Date(rs.getLong("published")), new Date(rs.getLong("last_checked")));
	}
	
	private Group resultSetToGroup(ResultSet rs) throws SQLException {
		return new Group(rs.getInt("group_id"), rs.getString("group_name"));
	}
	
	private VideoGroup resultSetToVideoGroup(ResultSet rs) throws SQLException {
		return new VideoGroup(rs.getInt("gitem_id"), rs.getString("video_id"));
	}
	
	public CommentQuery newCommentQuery() {
		return new CommentQuery();
	}
	
	public List<CommentType> getCommentTree(String parentId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM comments JOIN channels USING (channel_id) WHERE comment_id = ? OR parent_id = ? ORDER BY is_reply ASC, comment_date ASC");
		ps.setString(1, parentId);
		ps.setString(2, parentId);
		ResultSet rs = ps.executeQuery();
		List<CommentType> list = new ArrayList<>();
		String channelId;
		while(rs.next()) {
			channelId = rs.getString("channel_id");
			if(!channelCache.containsKey(channelId)) channelCache.put(channelId, resultSetToChannel(rs));
			list.add(resultSetToComment(rs));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	class CommentQuery {
		private String fullQuery = "";
		private long totalResults = 0;
		
		private int groupId = 0;
		private GitemType gitem = null;
		private int orderBy = 0;
		private int cType = 0;
		private int limit = 500;
		private boolean random = false;
		// private boolean fairRandom = false;
		private String nameLike = "";
		private String textLike = "";
		private long before = Long.MAX_VALUE, after = 0;
		
		final private String[] order = {
			"comment_date DESC ",
			"comment_date ASC ",
			"comment_likes DESC ",
			"reply_count DESC ",
			"LENGTH(comment_text) DESC ",
			"channel_name ASC, comment_date DESC ",
			"comment_text ASC "
		};
		
		public CommentQuery group(int groupId) {
			this.groupId = groupId;
			return this;
		}
		
		public CommentQuery groupItem(GitemType gitem) {
			this.gitem = gitem;
			return this;
		}
		
		public CommentQuery orderBy(int order) {
			this.orderBy = order;
			return this;
		}
		
		public CommentQuery cType(int cType) {
			this.cType = cType;
			return this;
		}
		
		public CommentQuery limit(int limit) {
			this.limit = limit;
			return this;
		}
		
		public CommentQuery random(boolean random) {
			this.random = random;
			return this;
		}
		
		public CommentQuery after(Date date) {
			after = date.getTime();
			return this;
		}
		
		public CommentQuery before(Date date) {
			before = date.getTime();
			return this;
		}
		
		/*public CommentQuery isFair(boolean isFair) {
			this.fairRandom = isFair;
			return this;
		}*/
		
		public CommentQuery textLike(String like) {
			this.textLike = like;
			return this;
		}
		
		public CommentQuery nameLike(String like) {
			this.nameLike = like;
			return this;
		}
		
		public int getPageCount() {
			return (int) ((getTotalResults()*1.0) / limit) + 1;
		}
		
		public long getTotalResults() {
			return totalResults;
		}
		
		public List<CommentType> get(int page) throws SQLException {
			String query = "SELECT * FROM comments "
					+ "LEFT JOIN channels USING (channel_id) "
					+ "WHERE ";
			if(random) {
				query += "comments.comment_id IN ("
						+ "SELECT comment_id FROM comments "
						+ "JOIN video_group USING (video_id) "
						+ "JOIN group_gitem USING (gitem_id) "
						+ "JOIN groups USING (group_id) "
						+ "WHERE "+(gitem != null ? "gitem_id = ? " : "group_id = ? ")
						+ "AND channel_name LIKE ? AND comment_text LIKE ? "
						+ "AND comment_date > ? AND comment_date < ? "+(cType != 0 ? "AND is_reply = ? ":"")
						+ ") ORDER BY "+order[orderBy];
			} else {
				query += "comments.video_id IN ("
						+ "SELECT video_id FROM videos "
						+ "JOIN video_group USING (video_id) "
						+ "JOIN group_gitem USING (gitem_id) "
						+ "JOIN groups USING (group_id) "
						+ "WHERE "+(gitem != null ? "gitem_id = ?" : "group_id = ?")
						+ ") AND channel_name LIKE ? AND comment_text LIKE ? AND comment_date > ? AND comment_date < ? "+(cType != 0 ? "AND is_reply = ? ":"")+"ORDER BY "+order[orderBy];
			}
			fullQuery = query;
			System.out.println(fullQuery);
			PreparedStatement ps = con.prepareStatement(fullQuery);
			if(gitem != null) ps.setInt(1, gitem.getGitemId()); else ps.setInt(1, groupId);
			ps.setString(2, "%"+nameLike+"%");
			ps.setString(3, "%"+textLike+"%");
			ps.setLong(4, after);
			ps.setLong(5, before);
			if(cType != 0) ps.setBoolean(6, cType == 2);
			
			ResultSet rs = ps.executeQuery();
			
			long start = limit * (page-1); // Page starts from 0
			long end = limit * page;
			long pos = 0;
			String channelId;
			List<CommentType> list = new ArrayList<>();
			System.out.println(start+", "+end);
			while(rs.next()) {
				if(pos >= start && pos < end) {
					channelId = rs.getString("channel_id");
					if(!channelCache.containsKey(channelId)) channelCache.put(channelId, resultSetToChannel(rs));
					list.add(resultSetToComment(rs));
				}
				pos++;
			}
			totalResults = pos;
			ps.close();
			rs.close();
			return list;
		}
	}
	
	public void insertComments(List<CommentType> list) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO comments (comment_id, channel_id, video_id, comment_date, comment_text, comment_likes, reply_count, is_reply, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
		for(CommentType ct : list) {
			if(ct != null) {
				ps.setString(1, ct.getId());
				ps.setString(2, ct.getChannelId());
				ps.setString(3, ct.getVideoId());
				ps.setLong(4, ct.getDate().getTime());
				ps.setString(5, ct.getText());
				ps.setLong(6, ct.getLikes());
				ps.setLong(7, ct.getReplies());
				ps.setBoolean(8, ct.isReply());
				ps.setString(9, ct.getParentId());
				ps.addBatch();
			}
		}
		ps.executeBatch();
		ps.close();
	}
	
	public List<String> getCommentIds(int groupId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT comment_id FROM comments JOIN video_group USING (video_id) JOIN group_gitem USING (gitem_id) WHERE group_id = ?");
		ps.setInt(1, groupId);
		ResultSet rs = ps.executeQuery();
		List<String> list = new ArrayList<>();
		while(rs.next()) {
			list.add(rs.getString("comment_id"));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public Map<String,Integer> getCommentThreadReplyCounts(int groupId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT comment_id, db_replies FROM comments JOIN (SELECT parent_id, COUNT(parent_id) AS db_replies FROM comments WHERE parent_id NOT NULL AND is_reply = 1 GROUP BY parent_id) AS cc ON cc.parent_id = comment_id JOIN video_group USING (video_id) JOIN group_gitem USING (gitem_id) WHERE is_reply = ? AND group_id = ?");
		ps.setBoolean(1, false);
		ps.setInt(2, groupId);
		ResultSet rs = ps.executeQuery();
		Map<String,Integer> map = new HashMap<>();
		while(rs.next()) {
			map.put(rs.getString("comment_id"), rs.getInt("db_replies"));
		}
		ps.close();
		rs.close();
		return map;
	}
	
	public void deleteCommentsInGroup(int groupId) throws SQLException { // TODO Comments may be in same video from another group.
		PreparedStatement ps = con.prepareStatement("DELETE * FROM comments WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE group_id = ?)");
		ps.setInt(1, groupId);
		ps.executeUpdate();
		ps.close();
	}
	
	public void deleteCommentsInGitem(int gitemId) throws SQLException { // TODO Comments may be in same video from another group.
		PreparedStatement ps = con.prepareStatement("DELETE * FROM comments WHERE video_id IN (SELECT video_id FROM video_group WHERE gitem_id = ?)");
		ps.setInt(1, gitemId);
		ps.executeUpdate();
		ps.close();
	}
	
	public void insertGroup(String groupName) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT INTO groups (group_name) VALUES (?)");
		ps.setString(1, groupName);
		ps.executeUpdate();
		ps.close();
	}
	
	public Group getGroup(String groupName) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM groups WHERE group_name = ?");
		ps.setString(1, groupName);
		ResultSet rs = ps.executeQuery();
		Group group = null;
		if(rs.next()) {
			group = resultSetToGroup(rs);
		}
		ps.close();
		rs.close();
		return group;
	}
	
	public Group getGroup(int group_id) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM groups WHERE group_id = ?");
		ps.setInt(1, group_id);
		ResultSet rs = ps.executeQuery();
		Group group = null;
		if(rs.next()) {
			group = resultSetToGroup(rs);
		}
		ps.close();
		rs.close();
		return group;
	}
	
	public List<Group> getGroups() throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM groups");
		ResultSet rs = ps.executeQuery();
		List<Group> list = new ArrayList<>();
		while(rs.next()) {
			list.add(resultSetToGroup(rs));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public void updateGroupName(int groupId, String newName) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE groups SET group_name = ? WHERE group_id = ?");
		ps.setString(1, newName);
		ps.setInt(2, groupId);
		ps.executeUpdate();
	}
	
	private void deleteGroup(int groupId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("DELETE FROM groups WHERE group_id = ?");
		ps.setInt(1, groupId);
		ps.executeUpdate();
		ps.close();
	}
	
	
	public List<GitemType> getGitems(int groupId, boolean fetchThumb) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM gitem_list JOIN group_gitem USING (gitem_id) WHERE group_id = ?");
		ps.setInt(1, groupId);
		ResultSet rs = ps.executeQuery();
		List<GitemType> list = new ArrayList<>();
		while(rs.next()) {
			list.add(resultSetToGitem(rs, fetchThumb));
		}
		rs.close();
		ps.close();
		return list;
	}
	
	public void insertGitems(int groupId, List<GitemType> list) throws SQLException {
		int next_id = 0;
		ResultSet rs = s.executeQuery("SELECT MAX(gitem_id) AS max_gitem FROM gitem_list");
		if(rs.next()) {next_id = rs.getInt("max_gitem");}
		PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO gitem_list (gitem_id, type_id, youtube_id, title, channel_title, published, last_checked, thumb_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
		for(GitemType gitem : list) {
			next_id++;
			gitem.setGitemId(next_id);
			ps.setInt(1, next_id);
			ps.setInt(2, gitem.typeId);
			ps.setString(3, gitem.getId());
			ps.setString(4, gitem.getTitle());
			ps.setString(5, gitem.getChannelTitle());
			ps.setLong(6, gitem.getPublished());
			ps.setLong(7, gitem.getLastChecked());
			ps.addBatch();
		}
		insertGroupGitem(groupId, list);
		ps.executeBatch();
		ps.close();
		System.out.println("Inserting "+list+" gitems");
	}
	
	private void insertGroupGitem(int groupId, List<GitemType> list) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT INTO group_gitem (group_id, gitem_id) VALUES (?, ?)");
		for(GitemType gitem : list) {
			ps.setInt(1, groupId);
			ps.setInt(2, gitem.getGitemId());
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
		System.out.println("Inserting "+list+" group-gitems");
	}
	
	public void updateGitems(List<GitemType> list) throws SQLException {
		Date now = new Date();
		PreparedStatement ps = con.prepareStatement("UPDATE gitem_list SET last_checked = ? WHERE gitem_id = ?");
		for(GitemType gitem : list) {
			ps.setLong(1, now.getTime());
			ps.setInt(2, gitem.getGitemId());
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
	}
	
	public VideoType getVideo(String videoId, boolean fetchImage) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos JOIN channels USING (channel_id) WHERE video_id = ?");
		ps.setString(1, videoId);
		ResultSet rs = ps.executeQuery();
		VideoType video = null;
		if(rs.next()) {
			String channelId = rs.getString("channel_id");
			if(!channelCache.containsKey(channelId)) channelCache.put(channelId, resultSetToChannel(rs));
			 video = resultSetToVideo(rs, fetchImage);
		}
		ps.close();
		rs.close();
		return video;
	}
	
	public List<String> getVideoIds() throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT video_id FROM videos");
		ResultSet rs = ps.executeQuery();
		List<String> list = new ArrayList<>();
		while(rs.next()) {
			list.add(rs.getString("video_id"));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public List<String> getVideoIds(int groupId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT video_id FROM videos WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE group_id = ?) ORDER BY publish_date DESC");
		ps.setInt(1, groupId);
		ResultSet rs = ps.executeQuery();
		List<String> list = new ArrayList<>();
		while(rs.next()) {
			list.add(rs.getString("video_id"));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public List<VideoType> getVideos(int groupId, boolean fetchImage) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos JOIN channels USING(channel_id) WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE group_id = ?) ORDER BY publish_date DESC");
		ps.setInt(1, groupId);
		ResultSet rs = ps.executeQuery();
		List<VideoType> list = new ArrayList<>();
		String channelId;
		while(rs.next()) {
			channelId = rs.getString("channel_id");
			if(!channelCache.containsKey(channelId)) channelCache.put(channelId, resultSetToChannel(rs));
			list.add(resultSetToVideo(rs, fetchImage));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public void deleteVideosInGroup(int groupId) throws SQLException { // TODO videos may be in other groups.
		PreparedStatement ps = con.prepareStatement("DELETE * FROM videos WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE group_id = ?");
		ps.setInt(1, groupId);
		ps.executeUpdate();
		ps.close();
	}
	
	public void deleteVideosInGitem(int gitemId) throws SQLException { // TODO  videos may be in other gitems/groups.
		PreparedStatement ps = con.prepareStatement("DELETE * FROM videos WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE group_id = ?");
		ps.setInt(1, gitemId);
		ps.executeUpdate();
		ps.close();
	}
	
	public void insertVideos(List<VideoType> list) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT INTO videos (video_id, channel_id, grab_date, publish_date, video_title, total_comments, total_views, total_likes, total_dislikes, video_desc, thumb_url, http_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
		for(VideoType video : list) {
			ps.setString(1, video.getId());
			ps.setString(2, video.getChannelId());
			ps.setLong(3, video.getGrabDate());
			ps.setLong(4, video.getPublishDate());
			ps.setString(5, video.getTitle());
			ps.setLong(6, video.getComments());
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
	
	public void updateVideos(List<VideoType> list) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE videos SET grab_date = ?, "
				+ "video_title = ?, "
				+ "total_comments = ?, "
				+ "total_views = ?, "
				+ "total_likes = ?, "
				+ "total_dislikes = ?, "
				+ "video_desc = ?, "
				+ "thumb_url = ? "
				+ "WHERE video_id = ?");
		for(VideoType video : list) {
			ps.setLong(1, video.getGrabDate());
			ps.setString(2, video.getTitle());
			ps.setLong(3, video.getComments());
			ps.setLong(4, video.getViews());
			ps.setLong(5, video.getLikes());
			ps.setLong(6, video.getDislikes());
			ps.setString(7, video.getDescription());
			ps.setString(8, video.getThumbUrl());
			ps.setString(9, video.getId());
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
	}
	
	public void updateVideoHttpCode(String videoId, int httpCode) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE videos SET http_code = ? WHERE video_id = ?");
		ps.setInt(1, httpCode);
		ps.setString(2, videoId);
		ps.executeUpdate();
		ps.close();
	}
	
	public void insertChannels(List<ChannelType> list) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT OR IGNORE INTO channels (channel_id, channel_name, channel_profile_url, download_profile) VALUES (?, ?, ?, ?)");
		for(ChannelType c : list) {
			if(c != null) {
				ps.setString(1, c.getId());
				ps.setString(2, c.getTitle());
				ps.setString(3, c.getThumbUrl());
				ps.setBoolean(4, c.willFetchThumb());
				ps.addBatch();
			} else {
				System.err.println("Error: Null value on channel insert.");
			}
		}
		ps.executeBatch();
		ps.close();
	}
	
	public ChannelType getChannelById(String channelId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM channels WHERE channel_id = ?");
		ps.setString(1, channelId);
		ResultSet rs = ps.executeQuery();
		ChannelType channel = null;
		if(rs.next()) {
			channel = resultSetToChannel(rs);
		}
		ps.close();
		rs.close();
		return channel;
	}
	
	public void updateChannelFetchThumb(String channelId, boolean willFetch) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE channels SET download_profile = ? WHERE channel_id = ?");
		ps.setBoolean(1, willFetch);
		ps.setString(2, channelId);
		ps.executeUpdate();
		ps.close();
		
		PreparedStatement get = con.prepareStatement("SELECT * FROM channels WHERE channel_id = ?");
		get.setString(1, channelId);
		ResultSet rs = get.executeQuery();
		channelCache.put(channelId, resultSetToChannel(rs));
		get.close();
		rs.close();
	}
	
	public void updateChannels(List<ChannelType> list) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE channels SET channel_name = ?, channel_profile_url = ?, download_profile = ? WHERE channel_id = ?");
		for(ChannelType c : list) {
			ps.setString(1, c.getTitle());
			ps.setString(2, c.getThumbUrl());
			ps.setBoolean(3, c.willFetchThumb());
			ps.setString(4, c.getId());
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
	}
	
	public List<String> getChannelIds() throws SQLException {
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
	
	public List<VideoGroup> getVideoGroups() throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * from video_group");
		ResultSet rs = ps.executeQuery();
		List<VideoGroup> list = new ArrayList<>();
		while(rs.next()) {
			list.add(resultSetToVideoGroup(rs));
		}
		rs.close();
		ps.close();
		return list;
	}
	
	public void insertVideoGroups(List<VideoGroup> list) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT INTO video_group (gitem_id, video_id) VALUES (?, ?)");
		for(VideoGroup vg : list) {
			ps.setInt(1, vg.gitem_id);
			ps.setString(2, vg.video_id);
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
		ps.close();
	}
	
	/*
	 * Analytics
	 */
	
	public Map<Long,Long> getWeekByWeekCommentHistogram(int group_id, int gitem_id) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT CAST(comment_date/604800000.00 AS INTEGER)*604800000 AS week, count(*) AS count FROM comments WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) GROUP BY week ORDER BY week");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ResultSet rs = ps.executeQuery();
		Map<Long,Long> data = new LinkedHashMap<>();
		while(rs.next()) {
			data.put(rs.getLong("week"), rs.getLong("count"));
		}
		ps.close();
		rs.close();
		return data;
	}
	
	public class Viewer {
		final String channelId;
		final long lastCommentOn;
		final long totalComments;
		final long totalLikes;
		public Viewer(String channelId, long lastCommentTime, long totalComments, long totalLikes) {
			this.channelId = channelId;
			this.lastCommentOn = lastCommentTime;
			this.totalComments = totalComments;
			this.totalLikes = totalLikes;
		}
	}
	
	public class Comment {
		final int occurances;
		final long lastCommentOn;
		final String commentText;
		public Comment(int occurances, long lastCommentTime, String commentText) {
			this.occurances = occurances;
			this.lastCommentOn = lastCommentTime;
			this.commentText = commentText;
		}
	}
	
	public List<Viewer> getMostActiveViewers(int group_id, int gitem_id, int limit) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT *, count(channel_id) AS count, MAX(comment_date) AS last_comment_on FROM channels JOIN comments USING (channel_id) WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) GROUP BY channel_id ORDER BY count DESC LIMIT ?");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ps.setInt(2, limit);
		ResultSet rs = ps.executeQuery();
		List<Viewer> list = new ArrayList<>();
		String channelId;
		while(rs.next()) {
			channelId = rs.getString("channel_id");
			if(!channelCache.containsKey(channelId)) channelCache.put(channelId, resultSetToChannel(rs));
			if(!getChannel(channelId).willFetchThumb()) {
				updateChannelFetchThumb(channelId, true);
			}
			list.add(new Viewer(channelId, rs.getLong("last_comment_on"), rs.getLong("count"), 0));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public List<Viewer> getMostPopularViewers(int group_id, int gitem_id, int limit) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT *, SUM(comment_likes) AS total_likes, MAX(comment_date) AS last_comment_on FROM channels JOIN comments USING (channel_id) WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) GROUP BY channel_id ORDER BY total_likes DESC LIMIT ?");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ps.setInt(2, limit);
		ResultSet rs = ps.executeQuery();
		List<Viewer> list = new ArrayList<>();
		String channelId;
		while(rs.next()) {
			channelId = rs.getString("channel_id");
			if(!channelCache.containsKey(channelId)) channelCache.put(channelId, resultSetToChannel(rs));
			if(!getChannel(channelId).willFetchThumb()) {
				updateChannelFetchThumb(channelId, true);
			}
			list.add(new Viewer(channelId, rs.getLong("last_comment_on"), 0, rs.getLong("total_likes")));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public List<Comment> getMostCommonComments(int group_id, int gitem_id, int limit) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT count(lower(comment_text)) AS count, MAX(comment_date) AS last_appearance, comment_text FROM comments WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) GROUP BY lower(comment_text) ORDER BY count DESC LIMIT ?");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ps.setInt(2, limit);
		ResultSet rs = ps.executeQuery();
		List<Comment> list = new ArrayList<>();
		while(rs.next()) {
			list.add(new Comment(rs.getInt("count"), rs.getLong("last_appearance"), rs.getString("comment_text")));
		}
		ps.close();
		rs.close();
		return list;
	}
	
	public Map<Long,Long> getWeekByWeekVideoHistogram(int group_id, int gitem_id) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT CAST(publish_date/604800000.00 AS INTEGER)*604800000 AS week, count(*) AS count FROM videos WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) GROUP BY week ORDER BY week");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ResultSet rs = ps.executeQuery();
		Map<Long,Long> data = new LinkedHashMap<>();
		while(rs.next()) {
			data.put(rs.getLong("week"), rs.getLong("count"));
		}
		ps.close();
		rs.close();
		return data;
	}
	
	public List<VideoType> getMostPopularVideos(int group_id, int gitem_id, int limit) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos  WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) ORDER BY total_views DESC LIMIT ?");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ps.setInt(2, limit);
		ResultSet rs = ps.executeQuery();
		List<VideoType> list = new ArrayList<>();
		while(rs.next()) {
			list.add(resultSetToVideo(rs, true));
		}
		rs.close();
		ps.close();
		return list;
	}
	
	public List<VideoType> getMostLikedVideos(int group_id, int gitem_id, int limit) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos  WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) ORDER BY total_likes DESC LIMIT ?");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ps.setInt(2, limit);
		ResultSet rs = ps.executeQuery();
		List<VideoType> list = new ArrayList<>();
		while(rs.next()) {
			list.add(resultSetToVideo(rs, true));
		}
		rs.close();
		ps.close();
		return list;
	}
	
	public List<VideoType> getMostDislikedVideos(int group_id, int gitem_id, int limit) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos  WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) ORDER BY total_dislikes DESC LIMIT ?");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ps.setInt(2, limit);
		ResultSet rs = ps.executeQuery();
		List<VideoType> list = new ArrayList<>();
		while(rs.next()) {
			list.add(resultSetToVideo(rs, true));
		}
		rs.close();
		ps.close();
		return list;
	}
	
	public List<VideoType> getMostCommentedVideos(int group_id, int gitem_id, int limit) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos  WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) ORDER BY total_comments DESC LIMIT ?");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ps.setInt(2, limit);
		ResultSet rs = ps.executeQuery();
		List<VideoType> list = new ArrayList<>();
		while(rs.next()) {
			list.add(resultSetToVideo(rs, true));
		}
		rs.close();
		ps.close();
		return list;
	}
	
	public List<VideoType> getDisabledVideos(int group_id, int gitem_id) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT * FROM videos  WHERE video_id IN (SELECT video_id FROM video_group JOIN group_gitem USING (gitem_id) WHERE "+(gitem_id != -1 ? "gitem_id":"group_id")+" = ?) AND http_code != 200 ORDER BY publish_date DESC");
		if(gitem_id != -1) ps.setInt(1, gitem_id); else ps.setInt(1, group_id);
		ResultSet rs = ps.executeQuery();
		List<VideoType> list = new ArrayList<>();
		while(rs.next()) {
			list.add(resultSetToVideo(rs, true));
		}
		rs.close();
		ps.close();
		return list;
	}
	
}
