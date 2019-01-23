package mattw.youtube.commentsuite.db;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sqlite.SQLiteConnection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestCommentDatabase {

    @Mock SQLiteConnection sqlite;
    @Mock Statement statement;
    @Mock PreparedStatement prepStatement;

    @InjectMocks CommentDatabase database;

    @Before
    public void setup() throws SQLException {
        when(sqlite.createStatement()).thenReturn(statement);
        // when(sqlite.prepareStatement(anyString())).thenReturn(prepStatement);
    }

    @Test
    public void testGetConnection() {
        assertEquals(sqlite, database.getConnection());
    }

    @Test
    public void testCommit() throws SQLException  {
        database.commit();

        verify(sqlite, times(1)).commit();
    }

    @Test
    public void testCreate() throws SQLException {
        database.create();

        verify(statement, times(1)).executeUpdate(anyString());
        verify(statement, times(1)).close();
        verify(sqlite, times(1)).commit();
    }

    @Test
    public void testVacuum() throws SQLException {
        database.vacuum();

        verify(statement, times(1)).execute(anyString());
        verify(statement, times(1)).close();
        verify(sqlite, times(1)).setAutoCommit(true);
        verify(sqlite, times(1)).setAutoCommit(false);
    }

    @Test
    public void testReset() throws SQLException {
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
        database.cleanUp();

        // commit() and create()
        verify(statement, times(1)).executeUpdate(anyString());
        verify(statement, times(1)).close();
        verify(sqlite, times(1)).commit();
    }

}
