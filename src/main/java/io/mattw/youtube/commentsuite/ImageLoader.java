package io.mattw.youtube.commentsuite;

import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum ImageLoader {
    ANGLE_DOUBLE_LEFT("angle-double-left.png"),
    ANGLE_DOUBLE_RIGHT("angle-double-right.png"),
    ANGLE_LEFT("angle-left.png"),
    ANGLE_RIGHT("angle-right.png"),
    BLANK_PROFILE("blankProfile.png"),
    BROWSER("browser.png"),
    CHANNEL("channel.png"),
    CHECK_CIRCLE("check-circle.png"),
    CLOSE("close.png"),
    GITHUB("github.png"),
    GROUP("group.png"),
    GROUP_ICON("groupIcon.png"),
    LOADING("loading.png"),
    MANAGE("manage.png"),
    MINUS_CIRCLE("minus-circle.png"),
    OOPS("oops.png"),
    PENCIL("pencil.png"),
    PLAYLIST("playlist.png"),
    PLUS("plus.png"),
    QUOTA("quota.png"),
    REPLY("reply.png"),
    SAVE("save.png"),
    SEARCH("search.png"),
    SETTINGS("settings.png"),
    TAGS("tag.png"),
    THUMBNAIL("thumbnail.png"),
    THUMBS_UP("thumbs-up.png"),
    TIMES_CIRCLE("times-circle.png"),
    TOGGLE_CONTEXT("toggleContext.png"),
    TOGGLE_QUERY("toggleQuery.png"),
    VIDEO("video.png"),
    VIDEO_PLACEHOLDER("videoPlaceholder.png"),
    YCS_ICON("icon.png"),
    YOUTUBE("youtube.png");

    private final Logger logger = LogManager.getLogger();

    private final String basePath = "/io/mattw/youtube/commentsuite/img";
    private Image image;

    ImageLoader(String fileName) {
        try {
            image = new Image(getClass().getResource(String.format("%s/%s", basePath, fileName)).toExternalForm());
        } catch (Exception e) {
            image = ImageCache.toLetterAvatar('X');
            logger.error("Failed to load resource image: " + fileName, e);
        }
    }

    public Image getImage() {
        return image;
    }

}
