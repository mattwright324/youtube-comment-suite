package io.mattw.youtube.commentsuite.db;

import io.mattw.youtube.commentsuite.util.DateUtils;
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
import java.util.stream.Collectors;

/**
 * Queries the database for comments.
 *
 * @author mattwright324
 */
public class CommentQuery implements Serializable, Exportable {

    private static final transient Logger logger = LogManager.getLogger();
    private final transient CommentDatabase database;

    // We don't want these in export file.
    private transient Group group;
    private transient Optional<GroupItem> groupItem = Optional.empty();
    private transient Optional<List<YouTubeVideo>> videos = Optional.empty();
    private transient int pageSize = 500;
    private transient int pageNum = 0;
    private transient Map<String, Object> queryParams = new HashMap<>();

    // Formatted values for export file.
    private String withGroup;
    private String groupLastRefreshed;
    private String withGroupItem;
    private String withVideos;

    // Search params
    private Order order;
    private CommentsType commentsType;
    private String nameLike;
    private String textLike;
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

        List<String> queryLines = new ArrayList<>();
        queryLines.add("SELECT * FROM comments");
        queryLines.add("LEFT JOIN channels USING (channel_id)");
        queryLines.add("WHERE comments.video_id IN (:videoSubquery)".replace(":videoSubquery", videoSubquery));
        if (StringUtils.isNotEmpty(nameLike)) {
            queryLines.add("AND (channels.channel_name LIKE :nameLike OR channels.channel_id = :channelId)");

            queryParams.put("nameLike", '%' + nameLike + '%');
            queryParams.put("channelId", nameLike);
        }
        if (StringUtils.isNotEmpty(textLike)) {
            queryLines.add("AND (comments.comment_text LIKE :textLike OR comments.comment_id = :commentId)");

            queryParams.put("textLike", '%' + textLike + '%');
            queryParams.put("commentId", textLike);
        }
        if (commentsType != CommentsType.ALL) {
            queryLines.add("AND is_reply = :isReply");

            queryParams.put("isReply", commentsType == CommentsType.REPLIES_ONLY);
        }

        queryLines.add("AND comments.comment_date > :dateFrom AND comments.comment_date < :dateTo");
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
        String queryString = buildQueryStringAndParams();

        NamedParameterStatement namedParamStatement = new NamedParameterStatement(database.getConnection(), queryString);

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
        List<YouTubeComment> comments = new ArrayList<>();

        logger.debug("Searching Comments [page={},pageSize={}] {}", page, pageSize, queryParams);

        setPageNum(page);
        setPageSize(pageSize);

        totalResults = 0;

        try (NamedParameterStatement statement = toStatement();
             ResultSet resultSet = statement.executeQuery()) {

            int indexStart = pageSize * page;
            int indexEnd = indexStart + pageSize;
            int index = 0;

            logger.trace("Searching Comments [indexStart={},indexEnd={}]", indexStart, indexEnd);

            while (resultSet.next()) {
                if (index >= indexStart && index < indexEnd) {
                    database.checkChannel(resultSet);

                    comments.add(database.resultSetToComment(resultSet));
                }

                index++;
                totalResults++;
            }
        }

        return comments;
    }

    /**
     * Returns a list of unique videoIds relevant to a search for export.
     * <p>
     * {@link io.mattw.youtube.commentsuite.fxml.SCExportModal}
     */
    public Set<String> getUniqueVideoIds() throws SQLException {
        Objects.requireNonNull(group);

        Set<String> items = new HashSet<>();

        if (videos.isPresent()) {
            items.addAll(videos.get()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(YouTubeVideo::getId)
                    .collect(Collectors.toList()));
        } else {
            if (groupItem.isPresent()) {
                items.addAll(database.getVideoIds(groupItem.get()));
            } else {
                items.addAll(database.getVideoIds(group));
            }
        }

        return items;
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

    @Override
    public void prepForExport() {
        if(group != null) {
            this.withGroup = String.format("%s / %s", group.getGroupId(), group.getName());

            long lastRefreshed = database.getLastChecked(this.getGroup());

            this.groupLastRefreshed =
                    lastRefreshed == 0 ?
                            "never refreshed" : DateUtils.epochMillisToDateTime(lastRefreshed).toString();
        }

        if (groupItem.isPresent()) {
            GroupItem item = groupItem.get();

            this.withGroupItem = String.format("%s / %s", item.getId(), item.getTitle());
        } else {
            this.withGroupItem = "All Item(s)";
        }

        this.withVideos = videos.map(youTubeVideos -> youTubeVideos.stream()
                .map(YouTubeVideo::getId)
                .collect(Collectors.joining(",")))
                .orElse("All Video(s)");
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
