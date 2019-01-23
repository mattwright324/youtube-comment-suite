package mattw.youtube.commentsuite.db;

import mattw.youtube.commentsuite.FXMLSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL query builder to query comments in database
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public class CommentQuery {

    private Logger logger = LogManager.getLogger(this.toString());

    private int orderBy = 0;
    private int ctype = 0;
    private int limit = 500;
    private String nameLike = "";
    private String commentLike = "";
    private long before = Long.MAX_VALUE;
    private long after = Long.MIN_VALUE;

    private int page = 1;
    private long totalResults = 0;

    private CommentDatabase database;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    final private String[] order = {
            "comment_date DESC",
            "comment_date ASC",
            "comment_likes DESC",
            "reply_count DESC",
            "LENGTH(comment_text) DESC",
            "channel_name ASC, comment_date DESC",
            "comment_text ASC"
    };

    public CommentQuery() {
        database = FXMLSuite.getDatabase();
    }

    public List<YouTubeComment> get(int page, Group group, GroupItem gitem, List<YouTubeVideo> videos) throws SQLException {
        logger.debug(String.format("get [page=%s, group=%s, groupItem=%s, videos=%s, orderBy=%s, ctype=%s, " +
                        "limit=%s, nameLike=%s, commentLike=%s, before=%s, after=%s]",
                page, group.getId(), String.valueOf(gitem), String.valueOf(videos),
                order[orderBy], ctype, limit, nameLike, commentLike,
                sdf.format(new Date(before)), sdf.format(new Date(after))));

        this.page = Math.abs(page);

        List<YouTubeComment> items = new ArrayList<>();

        String subquery;
        Map<String,Object> map = new HashMap<>();
        if(videos != null && !videos.stream().filter(Objects::nonNull).collect(Collectors.toList()).isEmpty()) {
            List<String> vl = new ArrayList<>();
            for(int i=0; i<videos.size(); i++) {
                vl.add(":v"+i);
                map.put("v"+i,videos.get(i).getYoutubeId());
            }
            subquery = String.join(" ", vl);
        } else if(gitem != null) {
            subquery = "SELECT video_id FROM videos JOIN gitem_video USING (video_id) JOIN group_gitem USING (gitem_id) WHERE gitem_id = :gitem";
            map.put("gitem", gitem.getYoutubeId());
        } else {
            subquery = "SELECT video_id FROM videos JOIN gitem_video USING (video_id) JOIN group_gitem USING (gitem_id) WHERE group_id = :group";
            map.put("group", group.getId());
        }
        map.put("cname", "%" + nameLike + "%");
        map.put("ctext", "%" + commentLike + "%");
        map.put("dateafter",  after);
        map.put("datebefore", before);
        if(ctype != 0) {
            map.put("isreply", ctype == 2);
        }

        List<String> queryParts = new ArrayList<>();
        queryParts.add("SELECT * FROM comments");
        queryParts.add("LEFT JOIN channels USING (channel_id)");
        queryParts.add("WHERE comments.video_id IN (:subquery)".replace(":subquery", subquery));
        queryParts.add("AND channel_name LIKE :cname");
        queryParts.add("AND comment_text LIKE :ctext");
        queryParts.add("AND comment_date > :dateafter AND comment_date < :datebefore");
        if(ctype != 0) {
            queryParts.add("AND is_reply = :isreply");
        }
        queryParts.add("ORDER BY " + order[orderBy]);

        String query = String.join(" ", queryParts);

        logger.debug("Building query");
        try(NamedParameterStatement nps = new NamedParameterStatement(database.getConnection(), query)) {
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
            logger.debug("Executing query");
            try(ResultSet rs = nps.executeQuery()) {
                long start = limit * (page-1);
                long end = limit * page;
                long pos = 0;
                logger.debug(String.format("Page %s (%s, %s)",
                        page, start, end));
                while(rs.next()) {
                    if(pos >= start && pos < end) {
                        database.checkChannel(rs);
                        items.add(database.resultSetToComment(rs));
                    }
                    totalResults = pos++;
                }
            }
        }
        return items;
    }

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
        this.commentLike = textLike;
        return this;
    }

    public int getPage() {
        return page;
    }

    public int getPageCount() {
        return (int) ((totalResults*1.0) / limit) + 1;
    }

    public long getTotalResults() {
        return totalResults;
    }
}
