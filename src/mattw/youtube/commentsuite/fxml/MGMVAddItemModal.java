package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.Cleanable;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.db.*;
import mattw.youtube.datav3.Parts;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.entrypoints.ChannelsList;
import mattw.youtube.datav3.entrypoints.PlaylistsList;
import mattw.youtube.datav3.entrypoints.VideosList;
import mattw.youtube.datav3.entrypoints.YouTubeErrorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class MGMVAddItemModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(MGMVAddItemModal.class.getName());

    private CommentDatabase database;
    private YouTubeData3 youtube;

    private @FXML Label alertError;
    private @FXML TextField link;
    private @FXML Button btnClose, btnSubmit;

    private @FXML Label link1, link2, link3, link4, link5;

    private Group group;

    private IntegerProperty itemAdded = new SimpleIntegerProperty(0);

    public MGMVAddItemModal(Group group) {
        this.group = group;

        database = FXMLSuite.getDatabase();
        youtube = FXMLSuite.getYoutubeApi();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVAddItemModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            Label[] links = {link1, link2, link3, link4, link5};
            for(Label l : links) {
                l.setOnMouseClicked(me -> {
                    Object src = me.getSource();
                    if(src instanceof Label) {
                        Label label = (Label) src;
                        runLater(() -> link.setText(label.getText()));
                    }
                });
            }

            btnSubmit.setOnAction(ae -> new Thread(() -> {
                runLater(() -> btnSubmit.setDisable(true));
                Pattern video1 = Pattern.compile("(?:http[s]?://youtu.be/)([\\w_\\-]+)");
                Pattern video2 = Pattern.compile("(?:http[s]?://www.youtube.com/watch\\?v=)([\\w_\\-]+)");
                Pattern playlist = Pattern.compile("(?:http[s]?://www.youtube.com/playlist\\?list=)([\\w_\\-]+)");
                Pattern channel1 = Pattern.compile("(?:http[s]?://www.youtube.com/channel/)([\\w_\\-]+)");
                Pattern channel2 = Pattern.compile("(?:http[s]?://www.youtube.com/user/)([\\w_\\-]+)");

                Matcher m;
                String fullLink = link.getText();
                YType type = YType.UNKNOWN;
                boolean channelUsername = false;
                String result = "";
                if((m = video1.matcher(fullLink)).matches()) {
                    result = m.group(1);
                    type = YType.VIDEO;
                } else if((m = video2.matcher(fullLink)).matches()) {
                    result = m.group(1);
                    type = YType.VIDEO;
                } else if((m = playlist.matcher(fullLink)).matches()) {
                    result = m.group(1);
                    type = YType.PLAYLIST;
                } else if((m = channel1.matcher(fullLink)).matches()) {
                    result = m.group(1);
                    type = YType.CHANNEL;
                } else if((m = channel2.matcher(fullLink)).matches()) {
                    result = m.group(1);
                    type = YType.CHANNEL;
                    channelUsername = true;
                }

                if(result.isEmpty()) {
                    runLater(() -> setError("Input did not match expected formats."));
                } else {
                    try {
                        List<GroupItem> list = new ArrayList<>();
                        if(type == YType.VIDEO) {
                            VideosList vl = ((VideosList) youtube.videosList().part(Parts.SNIPPET))
                                    .getByIds(result, "");
                            if(vl.hasItems()) {
                                VideosList.Item item = vl.getItems()[0];
                                GroupItem gitem = new GroupItem(item);

                                list.add(gitem);
                            }
                        } else if(type == YType.CHANNEL) {
                            ChannelsList cl = youtube.channelsList().part(Parts.SNIPPET);
                            if(!channelUsername) {
                                cl = cl.getByChannel(result, "");
                            } else {
                                cl = cl.getByUsername(result, "");
                            }

                            if(cl.hasItems()) {
                                ChannelsList.Item item = cl.getItems()[0];
                                GroupItem gitem = new GroupItem(item);

                                list.add(gitem);
                            }
                        } else if(type == YType.PLAYLIST) {
                            PlaylistsList pl = ((PlaylistsList) youtube.playlistsList().part(Parts.SNIPPET))
                                    .getByPlaylist(result, "");
                            if(pl.hasItems()) {
                                PlaylistsList.Item item = pl.getItems()[0];
                                GroupItem gitem = new GroupItem(item);

                                list.add(gitem);
                            }
                        } else {
                            runLater(() -> setError("Unexpected result."));
                        }

                        if(!list.isEmpty()) {
                            try {
                                database.insertGroupItems(this.group, list);
                                database.commit();
                                runLater(() -> {
                                    itemAdded.setValue(itemAdded.getValue() + 1);
                                    btnClose.fire();
                                });
                            } catch (SQLException e1) {
                                runLater(() -> setError(e1.getClass().getSimpleName()));
                            }
                        }
                    } catch (IOException e) {
                        runLater(() -> {
                            String message = e.getClass().getSimpleName();
                            if(e instanceof YouTubeErrorException) {
                                message = ((YouTubeErrorException) e).getError().getMessage();
                            }
                            setError(message);
                        });
                    }
                }
                runLater(() -> btnSubmit.setDisable(false));
            }).start());
        } catch (IOException e) { logger.error(e); }
    }

    public void setError(String message) {
        alertError.setText(message);
        alertError.setVisible(true);
        alertError.setManaged(true);
    }

    public IntegerProperty itemAddedProperty() {
        return itemAdded;
    }

    @Override
    public void cleanUp() {
        alertError.setVisible(false);
        alertError.setManaged(false);
        link.setText("");
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }
}
