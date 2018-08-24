package mattw.youtube.commentsuite.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.commentsuite.db.YType;
import mattw.youtube.datav3.YouTubeData3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MGMVAddItemModal extends VBox {

    private static Logger logger = LogManager.getLogger(MGMVAddItemModal.class.getName());

    private CommentDatabase database;
    private YouTubeData3 youtube;

    private @FXML Label alertError;
    private @FXML TextField link;
    private @FXML Button btnClose;
    private @FXML Button btnSubmit;

    private @FXML Label link1, link2, link3, link4, link5;

    private Group group;

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
                        Platform.runLater(() -> link.setText(label.getText()));
                    }
                });
            }

            btnSubmit.setOnAction(ae -> new Thread(() -> {
                Platform.runLater(() -> {
                    btnSubmit.setDisable(true);
                });
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
                    Platform.runLater(() -> setError("Input did not match expected formats."));
                } else {
                    // TODO: Submit URL on addItem.
                    String msg = String.format("%s / %s", type.getDisplay(), result);
                    Platform.runLater(() -> setError(msg));
                }
                Platform.runLater(() -> btnSubmit.setDisable(false));
            }).start());
        } catch (IOException e) { logger.error(e); }
    }

    public void setError(String message) {
        alertError.setText(message);
        alertError.setVisible(true);
        alertError.setManaged(true);
    }

    public void reset() {
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
