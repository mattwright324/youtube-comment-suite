package mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Base template for modal format overlaying parent StackPane.
 *
 * @param <T> Custom modal content fxml controller class
 */
public class OverlayModal<T extends Node> extends StackPane {

    @FXML Label title;
    @FXML Label divider;
    @FXML StackPane content;
    @FXML VBox modalContainer;

    public OverlayModal() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("OverlayModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();
    }

    public String getTitle() {
        return this.title.getText();
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    public void setContent(T content) {
        this.content.getChildren().clear();
        this.content.getChildren().add(content);
    }

    public void setDividerClass(String cssClass) {
        divider.getStyleClass().clear();
        divider.getStyleClass().add(cssClass);
    }

    public T getContent() {
        return (T) this.content.getChildren().get(0);
    }

    public VBox getModalContainer() {
        return this.modalContainer;
    }
}
