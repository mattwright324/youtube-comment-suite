package io.mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import io.mattw.youtube.commentsuite.ImageCache;
import io.mattw.youtube.commentsuite.db.GroupItem;
import io.mattw.youtube.commentsuite.util.BrowserUtil;

import java.io.IOException;

import static javafx.application.Platform.runLater;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class MGMVGroupItemView extends HBox {

    private GroupItem groupItem;

    private @FXML ImageView icon;
    private @FXML Label title;
    private @FXML Label author;
    private @FXML Label type;

    private BrowserUtil browserUtil = new BrowserUtil();

    public MGMVGroupItemView(GroupItem groupItem) {
        this.groupItem = groupItem;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVGroupItemView.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
            icon.setImage(groupItem.getDefaultThumb());
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
