package mattw.youtube.commentsuite;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import javafx.scene.image.Image;
import mattw.youtube.commentsuite.db.YouTubeObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Cache for images and letter avatars.
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public interface ImageCache {

    Logger logger = LogManager.getLogger(ImageCache.class.getSimpleName());

    Cache<Object, Image> thumbCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    static Image toLetterAvatar(YouTubeObject object) {
        return toLetterAvatar(object.getTitle());
    }

    static Image toLetterAvatar(String s) {
        if(s == null || s.isEmpty()) {
            return toLetterAvatar(" ");
        } else {
            return toLetterAvatar(s.charAt(0));
        }
    }

    static Image toLetterAvatar(char letter) {
        Image image = thumbCache.getIfPresent(letter);
        if(image == null) {
            image = new LetterAvatar(letter);
            thumbCache.put(letter, image);
        }
        return image;
    }

    static Image findOrGetImage(String id, String imageUrl) {
        ConfigFile<ConfigData> config = FXMLSuite.getConfig();
        ConfigData configData = config.getDataObject();

        Image image = thumbCache.getIfPresent(id);
        if(image == null) {
            String fileFormat = "jpg";
            File thumbFile = new File(String.format("./thumbs/%s.%s", id, fileFormat));
            if(configData.getArchiveThumbs() && !thumbFile.exists()) {
                logger.debug(String.format("Archiving [id=%s]", id));
                try {
                    thumbFile.mkdirs();
                    thumbFile.createNewFile();
                    ImageIO.write(ImageIO.read(new URL(imageUrl)), fileFormat, thumbFile);
                } catch (IOException e) {
                    logger.error("Failed to archive image.", e);
                }
            }
            if(thumbFile.exists()) {
                image = new Image(String.format("file:///%s", thumbFile.getAbsolutePath()));
            } else {
                image = new Image(imageUrl);
            }
            thumbCache.put(id, image);
        }
        return image;
    }

    static Image findOrGetImage(YouTubeObject object) {
        return findOrGetImage(object.getYoutubeId(), object.getThumbUrl());
    }

    static Image findOrGetImage(YouTubeAccount account) {
        return findOrGetImage(account.getChannelId(), account.getThumbUrl());
    }

    static boolean hasImageCached(YouTubeObject object) {
        return thumbCache.getIfPresent(object.getYoutubeId()) != null;
    }
}
