package io.mattw.youtube.commentsuite.db;

import io.mattw.youtube.commentsuite.events.GroupAddEvent;
import io.mattw.youtube.commentsuite.events.GroupDeleteEvent;
import io.mattw.youtube.commentsuite.events.GroupRenameEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.mattw.youtube.commentsuite.CommentSuite.postEvent;
import static io.mattw.youtube.commentsuite.db.SQLLoader.*;

public class GroupsTable extends TableHelper<Group> {

    private static final Logger logger = LogManager.getLogger();
    private static final Group DEFAULT_GROUP = new Group("28da132f5f5b48d881264d892aba790a", "Default");

    private CommentDatabase database;
    private List<Group> allGroups = new ArrayList<>();

    public GroupsTable(final Connection connection, final CommentDatabase database) {
        super(connection);
        this.database = database;
    }

    public List<Group> getAllGroups() {
        return allGroups;
    }

    @Override
    public Group to(ResultSet resultSet) throws SQLException {
        return new Group(resultSet.getString("group_id"),
                resultSet.getString("group_name"));
    }

    @Override
    public Group get(String id) throws SQLException {
        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM groups WHERE group_id = ?")) {
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
    public List<Group> getAll() throws SQLException {
        List<Group> results = new ArrayList<>();

        try (PreparedStatement ps = preparedStatement(
                "SELECT * FROM groups")) {
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
                "SELECT group_id FROM groups WHERE group_id = ?")) {
            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void refreshAllGroups() {
        try {
            allGroups.clear();
            allGroups.addAll(getAll());

            if (allGroups.isEmpty()) {
                logger.debug("Creating Default Group");

                insert(DEFAULT_GROUP);
            }
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    public Group create(String name) throws SQLException {
        return create(new Group(name));
    }

    public Group create(Group group) throws SQLException {
        insert(group);

        return group;
    }

    @Override
    public void insertAll(List<Group> objects) throws SQLException {
        try (PreparedStatement ps = preparedStatement(GROUP_CREATE.toString())) {
            for (Group group : objects) {
                ps.setString(1, group.getGroupId());
                ps.setString(2, group.getName());
                ps.addBatch();
            }
            ps.executeBatch();

            database.commit();

            refreshAllGroups();
            postEvent(new GroupAddEvent(objects));
        }
    }

    public void delete(Group group) throws SQLException {
        deleteAll(Collections.singletonList(group));
    }

    @Override
    public void deleteAll(List<Group> objects) throws SQLException {
        try (PreparedStatement ps = preparedStatement(DELETE_GROUP.toString())) {
            for (Group group : objects) {
                ps.setString(1, group.getGroupId());
                ps.addBatch();
            }
            ps.executeBatch();

            logger.debug("Cleaning up after group delete [{}]", objects);
            database.commit();
            database.cleanUp();

            refreshAllGroups();
            postEvent(new GroupDeleteEvent(objects));
        }
    }

    public void rename(Group group, String newName) throws SQLException {
        if (group.getName().equals(newName)) {
            return;
        }

        logger.trace("Renaming Group [id={},name={},newName={}]", group.getGroupId(), group.getName(), newName);

        try (PreparedStatement ps = preparedStatement(GROUP_RENAME.toString())) {
            ps.setString(1, newName);
            ps.setString(2, group.getGroupId());
            ps.executeUpdate();
        }

        database.commit();

        refreshAllGroups();
        postEvent(new GroupRenameEvent(group));
    }

    @Override
    public void updateAll(List<Group> objects) throws SQLException {
        // don't update more than one, use rename() instead
    }

}
