package io.mattw.youtube.commentsuite.db;

import io.mattw.youtube.commentsuite.ImageCache;
import javafx.scene.image.Image;

public interface HasImage extends ImageCache {

    String getId();

    String getThumbUrl();

    default Image findOrGetThumb() {
        return ImageCache.findOrGetImage(getId(), getThumbUrl());
    }

    default boolean isThumbLoaded() { return ImageCache.hasImageCached(getId()); }

}
