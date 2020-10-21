package io.mattw.youtube.commentsuite.guice;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class YouTubeProvider implements Provider<YouTube> {

    private static final Logger logger = LogManager.getLogger();

    private static YouTube instance;

    @Override
    public YouTube get() {
        logger.info("YouTubeProvider.get()");

        if (instance == null) {
            try {
                instance = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null)
                        .setApplicationName("youtube-comment-suite")
                        .build();
            } catch (GeneralSecurityException | IOException e) {
                logger.error(e);
            }
        }

        return instance;
    }

}
