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

public interface ImageCache {

    Logger logger = LogManager.getLogger(ImageCache.class.getSimpleName());

    Cache<String, Image> thumbCache = CacheBuilder.newBuilder()
            .maximumSize(250)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    static Image findOrGetImage(YouTubeObject object) {
        logger.debug(String.format("FindOrGet Image [id=%s]", object.getYoutubeId()));
        ConfigFile<ConfigData> config = FXMLSuite.getConfig();
        ConfigData configData = config.getDataObject();

        String id = object.getYoutubeId();
        Image image = thumbCache.getIfPresent(id);
        if(image == null) {
            logger.debug(String.format("Getting [id=%s]", object.getYoutubeId()));
            String fileFormat = "jpg";
            File thumbFile = new File(String.format("thumbs/%s.%s", id, fileFormat));
            if(configData.getArchiveThumbs() && !thumbFile.exists()) {
                logger.debug(String.format("Archiving [id=%s]", object.getYoutubeId()));
                try {
                    thumbFile.mkdir();
                    thumbFile.createNewFile();
                    ImageIO.write(ImageIO.read(new URL(object.getThumbUrl())), fileFormat, thumbFile);
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }
            }
            if(thumbFile.exists()) {
                logger.debug(String.format("Grabbing File [file=thumbs/%s.%s]", object.getYoutubeId(), fileFormat));
                image = new Image(String.format("file:///%s", thumbFile.getAbsolutePath()));
            } else {
                logger.debug(String.format("Grabbing Image [id=%s]", object.getYoutubeId()));
                image = new Image(object.getThumbUrl());
            }
            thumbCache.put(id, image);
        }
        return image;
    }
}
