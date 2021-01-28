package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.ConfigData;
import io.mattw.youtube.commentsuite.ImageCache;
import io.mattw.youtube.commentsuite.db.GroupItem;
import io.mattw.youtube.commentsuite.util.BrowserUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.IOException;

import static javafx.application.Platform.runLater;

public class MGMVGroupItemView extends HBox {

    private GroupItem groupItem;

    @FXML private ImageView icon;
    @FXML private Label title;
    @FXML private Label author;
    @FXML private Label type;

    private BrowserUtil browserUtil = new BrowserUtil();

    public MGMVGroupItemView(final GroupItem groupItem) {
        this.groupItem = groupItem;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVGroupItemView.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            if (ConfigData.FAST_GROUP_ADD_THUMB_PLACEHOLDER.equals(groupItem.getThumbUrl())) {
                icon.setManaged(false);
                icon.setVisible(false);
            } else {
                icon.setImage(groupItem.getDefaultThumb());
                icon.setManaged(true);
                icon.setVisible(true);
            }

            title.setText(groupItem.getTitle());
            author.setText(groupItem.getChannelTitle());
            type.setText(groupItem.getTypeName());

            icon.setCursor(Cursor.HAND);
            icon.setOnMouseClicked(me -> browserUtil.open(groupItem.buildYouTubeLink()));

            new Thread(() -> {
                Image image = ImageCache.findOrGetImage(groupItem);
                runLater(() -> icon.setImage(image));
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public GroupItem getGroupItem() {
        return this.groupItem;
    }
}
