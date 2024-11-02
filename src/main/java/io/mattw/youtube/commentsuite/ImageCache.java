package io.mattw.youtube.commentsuite;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Cache for images and letter avatars.
 *
 */
public interface ImageCache {

    Logger logger = LogManager.getLogger();

    File thumbsDir = new File("thumbs/");
    String thumbFormat = "jpg";

    Cache<Object, Image> thumbCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    static Image toLetterAvatar(final String s) {
        if (s == null || s.isEmpty()) {
            return toLetterAvatar(" ");
        } else {
            return toLetterAvatar(s.charAt(0));
        }
    }

    static Image toLetterAvatar(final char letter) {
        Image image = thumbCache.getIfPresent(letter);
        if (image == null) {
            image = new LetterAvatar(letter);
            thumbCache.put(letter, image);
        }
        return image;
    }

    static Image findOrGetImage(final String id, final String imageUrl) {
        final ConfigFile<ConfigData> config = CommentSuite.getConfig();
        final ConfigData configData = config.getDataObject();

        if (ConfigData.FAST_GROUP_ADD_THUMB_PLACEHOLDER.equals(imageUrl)) {
            return null;
        }

        Image image = thumbCache.getIfPresent(id);
        if (image == null) {
            final File thumbFile = new File(thumbsDir, String.format("%s.%s", id, thumbFormat));
            if (configData.isArchiveThumbs() && !thumbFile.exists()) {
                thumbsDir.mkdir();

                logger.debug("Archiving [id={}]", id);
                try {
                    final BufferedImage bufferedImage = ImageIO.read(new URL(imageUrl));

                    thumbFile.createNewFile();

                    ImageIO.write(bufferedImage, thumbFormat, thumbFile);
                } catch (IOException e) {
                    logger.error("Failed to archive image [id={}]", id, e);
                }
            }

            if (thumbFile.exists()) {
                image = new Image(String.format("file:///%s", thumbFile.getAbsolutePath()));
            } else if(imageUrl == null) {
                return toLetterAvatar("?");
            } else {
                image = new Image(imageUrl);
            }

            if(!image.isError()) {
                thumbCache.put(id, image);
            }
        }
        return image;
    }

    static boolean hasImageCached(final String id) {
        return thumbCache.getIfPresent(id) != null;
    }

}
