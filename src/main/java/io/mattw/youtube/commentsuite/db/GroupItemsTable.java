package io.mattw.youtube.commentsuite.db;

import io.mattw.youtube.commentsuite.events.GroupItemAddEvent;
import io.mattw.youtube.commentsuite.events.GroupItemDeleteEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.mattw.youtube.commentsuite.CommentSuite.postEvent;
import static io.mattw.youtube.commentsuite.db.SQLLoader.*;

public class GroupItemsTable extends TableHelper<GroupItem> {

    public GroupItemsTable(final Connection connection) {
        super(connection);
    }

    @Override
    public GroupItem to(ResultSet resultSet) throws SQLException {
        return new GroupItem()
                .setId(resultSet.getString("gitem_id"))
                .setType(YouTubeType.values()[resultSet.getInt("type_id") + 1])
                .setDisplayName(resultSet.getString("title"))
                .setChannelTitle(resultSet.getString("channel_title"))
                .setThumbUrl(resultSet.getString("thumb_url"))
                .setPublished(resultSet.getLong("published"))
                .setLastChecked(resultSet.getLong("last_checked"));
    }

    @Override
    public GroupItem get(String id) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM gitem_list WHERE gitem_id = ?")) {
            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return to(rs);
                }
            }
        }

        return null;
    }

    public List<GroupItem> byGroup(Group group) {
        List<GroupItem> items = new ArrayList<>();

        try (PreparedStatement ps = preparedStatement(GET_GROUPITEMS.toString())) {
            ps.setString(1, group.getGroupId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(to(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    @Override
    public List<GroupItem> getAll() throws SQLException {
        List<GroupItem> results = new ArrayList<>();

        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM gitem_list")) {
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
                "SELECT gitem_list FROM videos WHERE gitem_id = ?")) {
            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertAll(Group group, List<GroupItem> objects) throws SQLException {
        insertAll(objects);
        associateGroup(group, objects);

        postEvent(new GroupItemAddEvent(group));
    }

    @Override
    public void insertAll(List<GroupItem> objects) throws SQLException {
        try (PreparedStatement psCG = preparedStatement(CREATE_GITEM.toString())) {
            for (GroupItem gi : objects) {
                psCG.setString(1, gi.getId());
                psCG.setInt(2, gi.getType().id());
                psCG.setString(3, gi.getDisplayName());
                psCG.setString(4, gi.getChannelTitle());
                psCG.setLong(5, gi.getPublished());
                psCG.setLong(6, gi.getLastChecked());
                psCG.setString(7, gi.getThumbUrl());
                psCG.addBatch();
            }

            psCG.executeBatch();
        }
    }

    public void deleteAssociations(Group group, List<GroupItem> groupItems) throws SQLException {
        try (PreparedStatement ps = preparedStatement(DELETE_GROUP_GITEM.toString())) {
            for (GroupItem gi : groupItems) {
                ps.setString(1, gi.getId());
                ps.setString(2, group.getGroupId());
                ps.addBatch();
            }

            ps.executeBatch();

            postEvent(new GroupItemDeleteEvent(group));
        }
    }

    @Override
    public void deleteAll(List<GroupItem> objects) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "DELETE FROM gitem_list WHERE gitem_id = ?")) {
            for (GroupItem gitem : objects) {
                ps.setString(1, gitem.getId());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    public void update(GroupItem object) throws SQLException {
        updateAll(Collections.singletonList(object));
    }

    @Override
    public void updateAll(List<GroupItem> objects) throws SQLException {
        try (PreparedStatement ps = preparedStatement(UPDATE_GITEM.toString())) {
            for (GroupItem item : objects) {
                ps.setString(1, item.getDisplayName());
                ps.setString(2, item.getChannelTitle());
                ps.setLong(3, item.getPublished());
                ps.setLong(4, System.currentTimeMillis());
                ps.setString(5, item.getThumbUrl());
                ps.setString(6, item.getId());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    /**
     * Associate GroupItems with this Group (group_gitem)
     */
    private void associateGroup(Group group, List<GroupItem> groupItems) throws SQLException {
        try (PreparedStatement psCGG = preparedStatement(CREATE_GROUP_GITEM.toString())) {
            for (GroupItem groupItem : groupItems) {
                psCGG.setString(1, group.getGroupId());
                psCGG.setString(2, groupItem.getId());
                psCGG.addBatch();
            }

            psCGG.executeBatch();
        }
    }

    /**
     * Associate Videos with this GroupItem (gitem_video)
     */
    public void associateVideos(GroupItem groupItem, List<String> videoIds) throws SQLException {
        try (PreparedStatement ps = preparedStatement(INSERT_IGNORE_GITEM_VIDEO.toString())) {
            for (String videoId : videoIds) {
                ps.setString(1, groupItem.getId());
                ps.setString(2, videoId);
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

}
