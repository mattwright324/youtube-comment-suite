package io.mattw.youtube.commentsuite.db;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static io.mattw.youtube.commentsuite.db.CommentQuery.CommentsType.*;

/**
 * Queries the database for comments.
 *
 */
public class CommentQuery implements Serializable {

    private static final Logger logger = LogManager.getLogger();
    private final transient CommentDatabase database;

    private Group group;
    private Optional<GroupItem> groupItem = Optional.empty();
    private Optional<List<YouTubeVideo>> videos = Optional.empty();
    private int pageSize = 500;
    private int pageNum = 0;
    private Map<String, Object> queryParams = new HashMap<>();

    // Search params
    private Order order;
    private CommentsType commentsType;
    private String nameLike;
    private String textLike;
    private String hasTags;
    private LocalDate dateFrom = LocalDate.MIN;
    private LocalDate dateTo = LocalDate.MAX;

    private long totalResults;

    public CommentQuery(CommentDatabase database) {
        this.database = database;
    }

    /**
     * Builds the named parameter query string and parameter map.
     */
    private String buildQueryStringAndParams() {
        Objects.requireNonNull(group);
        Objects.requireNonNull(dateFrom);
        Objects.requireNonNull(dateTo);

        queryParams.clear();

        String videoSubquery;
        if (videos.isPresent() && !videos.get().isEmpty()) {
            List<YouTubeVideo> videoList = videos.get();

            List<String> videoIds = new ArrayList<>();
            for (int i = 0; i < videoList.size(); i++) {
                videoIds.add(":v" + i);

                queryParams.put("v" + i, videoList.get(i).getId());
            }
            videoSubquery = String.join(",", videoIds);
        } else {
            videoSubquery = "SELECT video_id FROM videos JOIN gitem_video USING (video_id) JOIN group_gitem USING (gitem_id) ";

            if (groupItem.isPresent()) {
                videoSubquery += "WHERE gitem_id = :gitemId";

                queryParams.put("gitemId", groupItem.get().getId());
            } else {
                videoSubquery += "WHERE group_id = :groupId";

                queryParams.put("groupId", group.getGroupId());
            }
        }

        final String commentsTable = commentsType == MODERATED_ONLY ? "comments_moderated" : "comments";
        final List<String> queryLines = new ArrayList<>();
        queryLines.add("WITH tag2 AS (");
        queryLines.add("SELECT t.comment_id, group_concat(t.tag, ',') AS tags");
        queryLines.add("FROM comment_tags t");
        queryLines.add("GROUP BY t.comment_id");
        queryLines.add(")");
        queryLines.add("SELECT *, tag2.tags FROM");
        queryLines.add(commentsTable);
        queryLines.add("LEFT JOIN channels USING (channel_id)");
        queryLines.add("LEFT JOIN tag2 USING (comment_id)");
        queryLines.add("WHERE " + commentsTable + ".video_id IN (:videoSubquery)".replace(":videoSubquery", videoSubquery));
        if (StringUtils.isNotEmpty(nameLike)) {
            queryLines.add("AND (channels.channel_name LIKE :nameLike OR channels.channel_id = :channelId)");

            queryParams.put("nameLike", '%' + nameLike + '%');
            queryParams.put("channelId", nameLike);
        }
        if (StringUtils.isNotEmpty(textLike)) {
            queryLines.add("AND (" + commentsTable + ".comment_text LIKE :textLike OR " + commentsTable + ".comment_id = :commentId)");

            queryParams.put("textLike", '%' + textLike + '%');
            queryParams.put("commentId", textLike);
        }
        if (StringUtils.isNotEmpty(hasTags)) {
            final String[] tags = hasTags.split(",");
            final List<String> named = new ArrayList<>();

            for (int i = 0; i < tags.length; i++) {
                final String param = "tag" + i;
                final String trimmed = tags[i].trim();

                named.add("(tag2.tags LIKE :" + param + "0 OR tag2.tags LIKE :" + param + "1 OR tag2.tags LIKE :" + param + "2 OR tag2.tags LIKE :" + param + "3)");
                queryParams.put(param + "0", trimmed);
                queryParams.put(param + "1", trimmed + ",%");
                queryParams.put(param + "2", "%," + trimmed + ",%");
                queryParams.put(param + "3", "%," + trimmed);
            }

            queryLines.add("AND (" + String.join(" OR ", named) + ")");
        }
        if (commentsType == REPLIES_ONLY || commentsType == COMMENTS_ONLY) {
            queryLines.add("AND is_reply = :isReply");

            queryParams.put("isReply", commentsType == REPLIES_ONLY);
        }

        queryLines.add("AND " + commentsTable + ".comment_date > :dateFrom AND " + commentsTable + ".comment_date < :dateTo");
        queryParams.put("dateFrom", localDateToEpochMillis(dateFrom, false));
        queryParams.put("dateTo", localDateToEpochMillis(dateTo, true));

        queryLines.add("ORDER BY :orderBy".replace(":orderBy", order.getSql()));

        return String.join(" ", queryLines);
    }

    /**
     * Converts dateTime to Pacific Time (PDT) as it is where YouTube is headquartered.
     *
     * @param date     LocalDate value
     * @param midnight beginning of day (false) or end of day midnight (true)
     * @return epoch millis of LocalDate in Pacific Time
     */
    private long localDateToEpochMillis(LocalDate date, boolean midnight) {
        LocalDateTime dateTime = midnight ?
                date.atTime(23, 59, 59) :
                date.atTime(0, 0, 0);

        return dateTime.atZone(ZoneId.of("America/Los_Angeles"))
                .toInstant()
                .toEpochMilli();
    }

    /**
     * Open statement to be used for more than just paginated comment search. This allows streaming
     * results directly to a JSON file without storing it in memory in the event of a massive result.
     */
    public NamedParameterStatement toStatement() throws SQLException {
        final String queryString = buildQueryStringAndParams();
        logger.debug(queryString);

        final NamedParameterStatement namedParamStatement = new NamedParameterStatement(database.getConnection(), queryString);

        for (String key : queryParams.keySet()) {
            Object value = queryParams.get(key);
            if (value instanceof Integer) {
                namedParamStatement.setInt(key, ((Integer) value));
            } else if (value instanceof Long) {
                namedParamStatement.setLong(key, ((Long) value));
            } else if (value instanceof String) {
                namedParamStatement.setString(key, ((String) value));
            } else if (value instanceof Boolean) {
                namedParamStatement.setInt(key, ((Boolean) value) ? 1 : 0);
            } else {
                namedParamStatement.setObject(key, value);
            }
        }

        return namedParamStatement;
    }

    /**
     * @param page     page starting of index 0
     * @param pageSize number of comments to return
     */
    public List<YouTubeComment> getByPage(int page, int pageSize) throws SQLException {
        final List<YouTubeComment> comments = new ArrayList<>();

        logger.debug("Searching Comments [page={},pageSize={}] {}", page, pageSize, queryParams);

        setPageNum(page);
        setPageSize(pageSize);

        totalResults = 0;

        try (final NamedParameterStatement statement = toStatement();
             final ResultSet resultSet = statement.executeQuery()) {

            int indexStart = pageSize * page;
            int indexEnd = indexStart + pageSize;
            int index = 0;

            logger.trace("Searching Comments [indexStart={},indexEnd={}]", indexStart, indexEnd);

            while (resultSet.next()) {
                if (index >= indexStart && index < indexEnd) {
                    database.channels().check(resultSet.getString("channel_id"));

                    comments.add(database.comments().to(resultSet));
                }

                index++;
                totalResults++;
            }
        }

        return comments;
    }

    public Group getGroup() {
        return group;
    }

    public CommentQuery setGroup(Group group) {
        this.group = group;
        return this;
    }

    public Optional<GroupItem> getGroupItem() {
        return groupItem;
    }

    public CommentQuery setGroupItem(Optional<GroupItem> groupItem) {
        this.groupItem = groupItem;
        return this;
    }

    public int getPageSize() {
        return pageSize;
    }

    public CommentQuery setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public Order getOrder() {
        return order;
    }

    public CommentQuery setOrder(Order order) {
        this.order = order;
        return this;
    }

    public String getNameLike() {
        return nameLike;
    }

    public CommentQuery setNameLike(String nameLike) {
        this.nameLike = nameLike;
        return this;
    }

    public String getTextLike() {
        return textLike;
    }

    public CommentQuery setTextLike(String textLike) {
        this.textLike = textLike;
        return this;
    }

    public String getHasTags() {
        return hasTags;
    }

    public CommentQuery setHasTags(String hasTags) {
        this.hasTags = hasTags;
        return this;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public CommentQuery setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
        return this;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public CommentQuery setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
        return this;
    }

    public Optional<List<YouTubeVideo>> getVideos() {
        return videos;
    }

    public CommentQuery setVideos(Optional<List<YouTubeVideo>> videos) {
        this.videos = videos;
        return this;
    }

    public CommentsType getCommentsType() {
        return commentsType;
    }

    public CommentQuery setCommentsType(CommentsType commentsType) {
        this.commentsType = commentsType;
        return this;
    }

    protected CommentQuery setTotalResults(long totalResults) {
        this.totalResults = totalResults;
        return this;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public int getPageCount() {
        return (int) ((totalResults * 1.0) / pageSize) + 1;
    }

    public int getPageNum() {
        return pageNum;
    }

    public CommentQuery setPageNum(int pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    public enum Order {
        MOST_RECENT("Most Recent", "comment_date DESC"),
        LEAST_RECENT("Least Recent", "comment_date ASC"),
        MOST_LIKES("Most Likes", "comment_likes DESC"),
        MOST_REPLIES("Most Replies", "reply_count DESC"),
        LONGEST_COMMENT("Longest Comment", "LENGTH(comment_text) DESC"),
        NAMES_A_TO_Z("Names (A to Z)", "channel_name ASC, comment_date DESC"),
        COMMENTS_A_TO_Z("Comments (A to Z)", "comment_text ASC"),
        ;

        private String title, sql;

        Order(String title, String sql) {
            this.title = title;
            this.sql = sql;
        }

        public String getTitle() {
            return title;
        }

        public String getSql() {
            return sql;
        }

        public String toString() {
            return getTitle();
        }
    }

    public enum CommentsType {
        ALL("Comments and Replies"),
        COMMENTS_ONLY("Comments Only"),
        REPLIES_ONLY("Replies Only"),
        MODERATED_ONLY("Moderated Only")
        ;

        private String title;

        CommentsType(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public String toString() {
            return getTitle();
        }
    }

}
