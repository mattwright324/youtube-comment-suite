package io.mattw.youtube.commentsuite.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.mattw.youtube.commentsuite.db.SQLLoader.INSERT_IGNORE_COMMENTS;

public class CommentsTable extends TableHelper<YouTubeComment> {

    private CommentDatabase database;

    public CommentsTable(final Connection connection, final CommentDatabase database) {
        super(connection);
        this.database = database;
    }

    @Override
    public YouTubeComment to(ResultSet resultSet) throws SQLException {
        return new YouTubeComment(resultSet.getString("comment_id"))
                .setCommentText(resultSet.getString("comment_text"))
                .setPublished(resultSet.getLong("comment_date"))
                .setVideoId(resultSet.getString("video_id"))
                .setChannelId(resultSet.getString("channel_id"))
                .setLikes(resultSet.getInt("comment_likes"))
                .setReplyCount(resultSet.getLong("reply_count"))
                .setReply(resultSet.getBoolean("is_reply"))
                .setParentId(resultSet.getString("parent_id"))
                .setModerationStatus(columnOrDefault(resultSet, "moderation_status", null));
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
        try (PreparedStatement ps = preparedStatement(INSERT_IGNORE_COMMENTS.toString())) {
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
        // not updating, ignoring if exists
    }

}
