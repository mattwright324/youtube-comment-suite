package io.mattw.youtube.commentsuite.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static io.mattw.youtube.commentsuite.db.SQLLoader.*;

public class VideosTable extends TableHelper<YouTubeVideo> {

    public VideosTable(final Connection connection) {
        super(connection);
    }

    @Override
    public YouTubeVideo to(ResultSet resultSet) throws SQLException {
        return new YouTubeVideo()
                .setId(resultSet.getString("video_id"))
                .setTitle(resultSet.getString("video_title"))
                .setThumbUrl(resultSet.getString("thumb_url"))
                .setChannelId(resultSet.getString("channel_id"))
                .setDescription(resultSet.getString("video_desc"))
                .setPublished(resultSet.getLong("publish_date"))
                .setRefreshedOn(resultSet.getLong("grab_date"))
                .setComments(resultSet.getLong("total_comments"))
                .setLikes(resultSet.getLong("total_likes"))
                .setViewCount(resultSet.getLong("total_views"))
                .setResponseCode(resultSet.getInt("http_code"));
    }

    @Override
    public YouTubeVideo get(String id) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM videos WHERE video_id = ?")) {
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
    public List<YouTubeVideo> getAll() throws SQLException {
        List<YouTubeVideo> results = new ArrayList<>();

        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM videos")) {
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
                "SELECT video_id FROM videos WHERE video_id = ?")) {
            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void insertAll(List<YouTubeVideo> objects) throws SQLException {
        try (PreparedStatement ps = preparedStatement(INSERT_REPLACE_VIDEOS.toString())) {
            for (YouTubeVideo video : objects) {
                ps.setString(1, video.getId());
                ps.setString(2, video.getChannelId());
                ps.setLong(3, video.getRefreshedOn());
                ps.setLong(4, video.getPublished());
                ps.setString(5, video.getTitle());
                ps.setLong(6, video.getComments());
                ps.setLong(7, video.getViewCount());
                ps.setLong(8, video.getLikes());
                ps.setLong(9, 0);
                ps.setString(10, video.getDescription());
                ps.setString(11, video.getThumbUrl());
                ps.setInt(12, video.getResponseCode());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    @Override
    public void deleteAll(List<YouTubeVideo> objects) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "DELETE FROM videos WHERE video_id = ?")) {
            for (YouTubeVideo video : objects) {
                ps.setString(1, video.getId());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    @Override
    public void updateAll(List<YouTubeVideo> objects) throws SQLException {
        insertAll(objects);
    }

    public void updateHttpCode(String videoId, int httpCode) throws SQLException {
        try (PreparedStatement ps = preparedStatement(UPDATE_VIDEO_HTTPCODE.toString())) {
            ps.setInt(1, httpCode);
            ps.setString(2, videoId);
            ps.executeUpdate();
        }
    }

    public List<String> idsByGroup(Group group) throws SQLException {
        try (PreparedStatement ps = preparedStatement(GET_ALL_VIDEO_IDS_BY_GROUP.toString())) {
            ps.setString(1, group.getGroupId());

            try (ResultSet rs = ps.executeQuery()) {
                List<String> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(rs.getString("video_id"));
                }

                return list;
            }
        }
    }

    public List<String> idsByGroupItem(GroupItem groupItem) throws SQLException {
        try (PreparedStatement ps = preparedStatement(GET_ALL_VIDEO_IDS_BY_GROUPITEM.toString())) {
            ps.setString(1, groupItem.getId());

            try (ResultSet rs = ps.executeQuery()) {
                List<String> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(rs.getString("video_id"));
                }

                return list;
            }
        }
    }

    public List<YouTubeVideo> byGroup(Group group) throws SQLException {
        return byGroupCriteria(group, "", "publish_date DESC", Integer.MAX_VALUE);
    }

    public List<YouTubeVideo> byGroupCriteria(Group group, String keyword, String order, int limit) throws SQLException {
        try (PreparedStatement ps = preparedStatement(GET_VIDEOS_BY_CRITERIA_GROUP.toString().replace(":order", order))) {
            ps.setString(1, group.getGroupId());
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, keyword);
            ps.setInt(4, limit);

            return toList(ps);
        }
    }

    public List<YouTubeVideo> byGroupItem(GroupItem groupItem) throws SQLException {
        return byGroupItemCriteria(groupItem, "", "publish_date DESC", Integer.MAX_VALUE);
    }

    public List<YouTubeVideo> byGroupItemCriteria(GroupItem groupItem, String keyword, String order, int limit) throws SQLException {
        try (PreparedStatement ps = preparedStatement(GET_VIDEOS_BY_CRITERIA_GITEM.toString().replace(":order", order))) {
            ps.setString(1, groupItem.getId());
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, keyword);
            ps.setInt(4, limit);

            return toList(ps);
        }
    }


}
