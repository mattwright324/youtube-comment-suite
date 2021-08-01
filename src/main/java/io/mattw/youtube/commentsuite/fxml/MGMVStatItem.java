package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.ImageCache;
import io.mattw.youtube.commentsuite.ImageLoader;
import io.mattw.youtube.commentsuite.db.*;
import io.mattw.youtube.commentsuite.util.BrowserUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import static javafx.application.Platform.runLater;

public class MGMVStatItem extends HBox implements ImageCache {

    private static final Logger logger = LogManager.getLogger();

    @FXML private ImageView thumbnail;
    @FXML private Label title, subtitle;

    private YouTubeChannel channel;
    private YouTubeVideo video;
    private Long value;
    private String subtitleText;
    private String subtitleSuffix;
    private boolean commentsDisabled = false;
    private boolean isVideo = true;
    private boolean justSubtitle = false;

    private final BrowserUtil browserUtil = new BrowserUtil();

    public MGMVStatItem(final YouTubeVideo video, final String subtitle) {
        this.video = video;
        this.subtitleText = subtitle;
        this.justSubtitle = true;

        initialize();
    }

    public MGMVStatItem(final YouTubeVideo video, final Long value, final String subtitleSuffix) {
        this(video, value, subtitleSuffix, false);
    }

    public MGMVStatItem(final YouTubeVideo video, final Long value, final String subtitleSuffix, final boolean commentsDisabled) {
        this.video = video;
        this.value = value;
        this.subtitleSuffix = subtitleSuffix;
        this.commentsDisabled = commentsDisabled;

        initialize();
    }

    public MGMVStatItem(final YouTubeChannel channel, final Long value, final String subtitleSuffix) {
        this.channel = channel;
        this.value = value;
        this.subtitleSuffix = subtitleSuffix;
        this.isVideo = false;

        initialize();
    }

    private void initialize() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVYouTubeObjectItem.fxml"));
            loader.setController(this);
            loader.setRoot(this);
            loader.load();

            final String youTubeLink = Stream.of(video, channel)
                    .filter(Objects::nonNull)
                    .map(linkable -> linkable.toYouTubeLink())
                    .findFirst()
                    .orElse("https://placehold.it/64x64");

            thumbnail.setImage(ImageLoader.LOADING.getImage());
            thumbnail.setCursor(Cursor.HAND);
            thumbnail.setOnMouseClicked(me -> browserUtil.open(youTubeLink));

            if (isVideo) {
                thumbnail.setFitWidth(89);
            } else {
                thumbnail.setFitWidth(50);
            }

            new Thread(() -> {
                final Image thumb = Stream.of(video, channel)
                        .filter(Objects::nonNull)
                        .map(hasImage -> hasImage.findOrGetThumb())
                        .findFirst()
                        .orElse(ImageCache.toLetterAvatar(' '));

                runLater(() -> thumbnail.setImage(thumb));
            }).start();

            final String titleText = Stream.of(video, channel)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .findFirst()
                    .orElse("Title");

            title.setText(titleText);

            subtitle.setText(justSubtitle ? subtitleText :
                    (commentsDisabled ? String.format("%s", subtitleSuffix) :
                            String.format("%,d %s", value, subtitleSuffix)));

            if (commentsDisabled) {
                subtitle.setStyle("-fx-text-fill:orangered");
            }
        } catch (IOException e) {
            logger.error("Failed to initialize MGMVYouTubeObjectListItem", e);
        }
    }

    public YouTubeChannel getChannel() {
        return channel;
    }

    public YouTubeVideo getVideo() {
        return video;
    }
}
