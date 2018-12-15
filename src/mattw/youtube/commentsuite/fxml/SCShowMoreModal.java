package mattw.youtube.commentsuite.fxml;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.Cleanable;
import mattw.youtube.commentsuite.db.YouTubeComment;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static javafx.application.Platform.runLater;

public class SCShowMoreModal extends VBox implements Cleanable {

    private static Logger logger = LogManager.getLogger(SCShowMoreModal.class.getSimpleName());

    private @FXML TextArea commentText;

    private @FXML Button btnClose;
    private @FXML Button btnSubmit;

    private SimpleBooleanProperty replyMode = new SimpleBooleanProperty(false);

    public SCShowMoreModal() {
        logger.debug("Initialize SCShowMoreModal");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SCShowMoreModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException e) { logger.error(e); }
    }

    public void loadComment(YouTubeComment comment, boolean replyMode) {
        String processedText = StringEscapeUtils.unescapeHtml4(comment.getText())
                .replace("<br />", "\r\n")
                .replaceAll("[̀-ͯ᪰-᫿᷀-᷿⃐-⃿︠-︯]","");

        runLater(() -> {
           enableReplyMode(replyMode);

           commentText.setText(processedText);
        });
    }

    public void enableReplyMode(boolean enable) {
        replyMode.setValue(enable);
    }

    public BooleanProperty replyModeProperty() {
        return replyMode;
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public Button getBtnSubmit() {
        return btnSubmit;
    }

    @Override
    public void cleanUp() {

    }
}
