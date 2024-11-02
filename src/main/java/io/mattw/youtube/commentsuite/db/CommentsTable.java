package io.mattw.youtube.commentsuite.db;

import io.mattw.youtube.commentsuite.CommentSuite;
import io.mattw.youtube.commentsuite.events.TagsChangeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.mattw.youtube.commentsuite.db.SQLLoader.INSERT_OR_COMMENTS;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CommentsTable extends TableHelper<YouTubeComment> {

    private static final Logger logger = LogManager.getLogger();

    private static final String INSERT_IGNORE = INSERT_OR_COMMENTS.toString().replace(":method", "IGNORE");
    private static final String INSERT_REPLACE = INSERT_OR_COMMENTS.toString().replace(":method", "REPLACE");

    private CommentDatabase database;

    public CommentsTable(final Connection connection, final CommentDatabase database) {
        super(connection);
        this.database = database;
    }

    @Override
    public YouTubeComment to(ResultSet resultSet) throws SQLException {
        return new YouTubeComment()
                .setId(resultSet.getString("comment_id"))
                .setCommentText(resultSet.getString("comment_text"))
                .setPublished(resultSet.getLong("comment_date"))
                .setVideoId(resultSet.getString("video_id"))
                .setChannelId(resultSet.getString("channel_id"))
                .setLikes(resultSet.getInt("comment_likes"))
                .setReplyCount(resultSet.getLong("reply_count"))
                .setReply(resultSet.getBoolean("is_reply"))
                .setParentId(resultSet.getString("parent_id"))
                .setTags(columnOrDefault(resultSet, "tags", null));
    }

    @Override
    public YouTubeComment get(String id) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM comments WHERE comment_id = ?")) {
            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return to(rs);
                }
            }
        }

        return null;
    }

    @Override
    public List<YouTubeComment> getAll() throws SQLException {
        List<YouTubeComment> results = new ArrayList<>();

        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM comments")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(to(rs));
                }
            }
        }

        return results;
    }

    @Override
    public boolean exists(String id) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "SELECT comment_id FROM comments WHERE comment_id = ?")) {
            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void insertAll(List<YouTubeComment> objects) throws SQLException {
        insert(objects, INSERT_IGNORE);
    }

    @Override
    public void deleteAll(List<YouTubeComment> objects) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "DELETE FROM comments WHERE comment_id = ?")) {
            for (YouTubeComment comment : objects) {
                ps.setString(1, comment.getId());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    @Override
    public void updateAll(List<YouTubeComment> objects) throws SQLException {
        insert(objects, INSERT_REPLACE);
    }

    private void insert(List<YouTubeComment> objects, String insertSql) throws SQLException {
        try (PreparedStatement ps = preparedStatement(insertSql)) {
            for (YouTubeComment ct : objects) {
                ps.setString(1, ct.getId());
                ps.setString(2, ct.getChannelId());
                ps.setString(3, ct.getVideoId());
                ps.setLong(4, ct.getPublished());
                ps.setString(5, ct.getCommentText());
                ps.setLong(6, ct.getLikes());
                ps.setLong(7, ct.getReplyCount());
                ps.setBoolean(8, ct.isReply());
                ps.setString(9, ct.getParentId());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Delete from moderated if it is now approved
        database.moderatedComments().deleteAll(objects);
    }

    public void associateTags(List<YouTubeComment> comments, List<String> tags)throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "INSERT OR IGNORE INTO comment_tags (comment_id, tag) VALUES (?, ?)")) {
            for (YouTubeComment comment : comments) {
                for (String tag : tags) {
                    if (isBlank(tag)) {
                        continue;
                    }

                    ps.setString(1, comment.getId());
                    ps.setString(2, tag);
                    ps.addBatch();

                    if (comment.getTags() == null) {
                        comment.setTags(new ArrayList<>());
                    }
                    if (!comment.getTags().contains(tag)) {
                        comment.getTags().add(tag);
                    }
                }
            }

            ps.executeBatch();

            CommentSuite.postEvent(new TagsChangeEvent(comments));
        }
    }

    public void deassociateTags(List<YouTubeComment> comments, List<String> tags) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "DELETE FROM comment_tags WHERE comment_id = ? AND tag = ?")) {
            for (YouTubeComment comment : comments) {
                for (String tag : tags) {
                    ps.setString(1, comment.getId());
                    ps.setString(2, tag);
                    ps.addBatch();

                    if (comment.getTags() != null) {
                        comment.getTags().remove(tag);
                    }
                }
            }

            ps.executeBatch();

            CommentSuite.postEvent(new TagsChangeEvent(comments));
        }
    }

    public List<String> getAllTags() throws SQLException {
        final List<String> tags = new ArrayList<>();
        try (PreparedStatement ps = preparedStatement(
                "SELECT DISTINCT tag FROM comment_tags ORDER BY tag")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(rs.getString("tag"));
                }
            }
        }
        return tags;
    }

}
