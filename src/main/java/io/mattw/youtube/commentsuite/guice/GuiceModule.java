package io.mattw.youtube.commentsuite.guice;

import com.google.api.services.youtube.YouTube;
import com.google.inject.AbstractModule;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import javafx.fxml.FXMLLoader;

public class GuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FXMLLoader.class).toProvider(FXMLLoaderProvider.class);
        bind(YouTube.class).toProvider(YouTubeProvider.class);
        bind(CommentDatabase.class).toProvider(DatabaseProvider.class);
    }

}
