package io.mattw.youtube.commentsuite.guice;

import com.google.inject.Provider;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

public class DatabaseProvider implements Provider<CommentDatabase> {

    private static final Logger logger = LogManager.getLogger();

    private static CommentDatabase instance;

    @Override
    public CommentDatabase get() {
        logger.info("DatabaseProvider.get()");

        if (instance == null) {
            try {
                instance = new CommentDatabase("commentsuite.sqlite3");
            } catch (SQLException e) {
                logger.error(e);
            }
        }

        return instance;
    }

}
