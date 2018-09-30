package mattw.youtube.commentsuite.db;

import mattw.youtube.commentsuite.FXMLSuite;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommentQuery {

    private long totalResults = 0;
    private int page = 1;

    private int orderBy = 0;
    private int ctype = 0;
    private int limit = 500;
    private String nameLike = "";
    private String textLike = "";
    private long before = Long.MAX_VALUE;
    private long after = Long.MIN_VALUE;

    private CommentDatabase db;

    public CommentQuery() {
        db = FXMLSuite.getDatabase();
    }

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

    public int getPage() { return page; }
    public int getPageCount() { return (int) ((totalResults*1.0) / limit) + 1; }
    public long getTotalResults() {
        return totalResults;
    }

    public List<YouTubeComment> get(int page, Group group, GroupItem gitem, List<YouTubeVideo> videos) throws SQLException {
        this.page = Math.abs(page);
        List<YouTubeComment> items = new ArrayList<>();
        String query = "SELECT * FROM comments LEFT JOIN channels USING (channel_id) WHERE comments.video_id IN ";
        Map<String,Object> map = new HashMap<>();
        if(gitem != null) {
            query += "(SELECT video_id FROM videos JOIN gitem_video USING (video_id) JOIN group_gitem USING (gitem_id) WHERE gitem_id = :gitem ) ";
            map.put("gitem", gitem.getYoutubeId());
        } else if(videos != null) {
            List<String> vl = new ArrayList<>();
            for(int i=0; i<videos.size(); i++) {
                vl.add(":v"+i);
                map.put("v"+i,videos.get(i).getYoutubeId());
            }
            query += "("+vl.stream().collect(Collectors.joining(","))+") ";
        } else {
            query += "(SELECT video_id FROM videos JOIN gitem_video USING (video_id) JOIN group_gitem USING (gitem_id) WHERE group_id = :group ) ";
            map.put("group", group.getId());
        }
        query += "AND channel_name LIKE :cname AND comment_text LIKE :ctext AND comment_date > :dateafter AND comment_date < :datebefore "+(ctype != 0 ? "AND is_reply = :isreply ":"")+"ORDER BY "+order[orderBy];
        map.put("cname", "%"+nameLike+"%");
        map.put("ctext", "%"+textLike+"%");
        map.put("dateafter", new Long(after));
        map.put("datebefore", new Long(before));
        if(ctype != 0) map.put("isreply", Boolean.valueOf(ctype == 2));
        System.out.println(query);
        NamedParameterStatement nps = new NamedParameterStatement(db.getConnection(), query);
        for(String key : map.keySet()) {
            Object value = map.get(key);
            if(value instanceof Integer) {
                nps.setInt(key, ((Integer) value));
            } else if(value instanceof Long) {
                nps.setLong(key, ((Long) value));
            } else if(value instanceof String) {
                nps.setString(key, ((String) value));
            } else if(value instanceof Boolean) {
                nps.setInt(key, ((Boolean) value) ? 1 : 0);
            } else {
                nps.setObject(key, value);
            }
        }
        ResultSet rs = nps.executeQuery();
        long start = limit * (page-1);
        long end = limit * page;
        long pos = 0;
        System.out.format("Page %s (%s, %s)\r\n", page, start, end);
        while(rs.next()) {
            if(pos >= start && pos < end) {
                db.checkChannel(rs);
                items.add(db.resultSetToComment(rs));
            }
            pos++;
        }
        totalResults = pos;
        nps.close();
        return items;
    }
}
