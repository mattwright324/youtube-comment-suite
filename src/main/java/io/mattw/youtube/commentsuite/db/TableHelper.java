package io.mattw.youtube.commentsuite.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class TableHelper<T> {

    private static final Logger logger = LogManager.getLogger();

    private final Connection connection;

    public TableHelper(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public Statement statment() throws SQLException {
        return connection.createStatement();
    }

    public PreparedStatement preparedStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public abstract T to(ResultSet resultSet) throws SQLException;

    public List<T> toList(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {

            List<T> list = new ArrayList<>();
            while (rs.next()) {
                list.add(to(rs));
            }

            return list;
        }
    }

    public abstract T get(String id) throws SQLException;

    public abstract List<T> getAll() throws SQLException;

    public abstract boolean exists(String id) throws SQLException;

    public boolean notExists(String id) throws SQLException {
        return !exists(id);
    }

    public long countExists(List<String> ids) throws SQLException {
        long count = 0;

        for (String id : ids) {
            if (exists(id)) {
                count++;
            }
        }

        return count;
    }

    public long countNotExists(List<String> ids) throws SQLException {
        long count = 0;

        for (String id : ids) {
            if (notExists(id)) {
                count++;
            }
        }

        return count;
    }

    public void insert(T object) throws SQLException {
        insertAll(Collections.singletonList(object));
    }

    public abstract void insertAll(List<T> objects) throws SQLException;

    public void delete(T object) throws SQLException {
        deleteAll(Collections.singletonList(object));
    }

    public abstract void deleteAll(List<T> objects) throws SQLException;

    public void update(T object) throws SQLException {
        updateAll(Collections.singletonList(object));
    }

    public abstract void updateAll(List<T> objects) throws SQLException;

    public String columnOrDefault(final ResultSet rs, final String columnName, final String def) throws SQLException {
        final ResultSetMetaData rsmd = rs.getMetaData();
        final int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return rs.getString(columnName);
            }
        }

        return def;
    }

}
