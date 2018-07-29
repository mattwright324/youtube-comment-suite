package mattw.youtube.commentsuite;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import javafx.scene.image.Image;

import java.util.concurrent.TimeUnit;

public interface ImageCache {

    Cache<String, Image> thumbCache = CacheBuilder.newBuilder()
            .maximumSize(250)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

}
