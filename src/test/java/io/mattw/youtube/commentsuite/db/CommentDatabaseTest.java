package io.mattw.youtube.commentsuite.db;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sqlite.SQLiteConnection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentDatabaseTest {

    @Mock
    private SQLiteConnection sqlite;
    @Mock
    private Statement statement;
    @Mock
    private PreparedStatement prepStatement;

    @InjectMocks
    private CommentDatabase database;

    private void prepStatement() throws SQLException {
        when(sqlite.createStatement()).thenReturn(statement);
    }

    @Test
    public void testGetConnection() {
        assertEquals(sqlite, database.getConnection());
    }

    @Test
    public void testCommit() throws SQLException {
        database.commit();

        verify(sqlite, times(1)).commit();
    }

    @Test
    public void testCreate() throws SQLException {
        prepStatement();

        database.create();

        verify(statement, times(1)).executeUpdate(anyString());
        verify(statement, times(1)).close();
        verify(sqlite, times(1)).commit();
    }

    @Test
    public void testVacuum() throws SQLException {
        prepStatement();

        database.vacuum();

        verify(statement, times(1)).execute(anyString());
        verify(statement, times(1)).close();
        verify(sqlite, times(1)).setAutoCommit(true);
        verify(sqlite, times(1)).setAutoCommit(false);
    }

    @Test
    public void testReset() throws SQLException {
        prepStatement();

        database.reset();

        // commit() and create()
        verify(statement, times(2)).executeUpdate(anyString());
        verify(statement, times(3)).close();
        verify(sqlite, times(2)).commit();
        // vacuum()
        verify(statement, times(1)).execute(anyString());
        verify(sqlite, times(1)).setAutoCommit(true);
        verify(sqlite, times(1)).setAutoCommit(false);
    }

    @Test
    public void testCleanUp() throws SQLException {
        prepStatement();

        database.cleanUp();

        // commit() and create()
        verify(statement, times(1)).executeUpdate(anyString());
        verify(statement, times(1)).close();
        verify(sqlite, times(1)).commit();
    }

}
