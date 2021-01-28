package io.mattw.youtube.commentsuite.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Base template for modal format overlaying parent StackPane.
 *
 * @param <T> Custom modal content fxml controller class
 */
public class OverlayModal<T extends Pane> extends StackPane {

    @FXML private Label title;
    @FXML private Label divider;
    @FXML private Label topSpacer, bottomSpacer;
    @FXML private StackPane content;
    @FXML private VBox modalContainer;

    public OverlayModal() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("OverlayModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        this.managedProperty().bindBidirectional(this.visibleProperty());
    }

    public String getTitle() {
        return this.title.getText();
    }

    public void setTitle(final String title) {
        this.title.setText(title);
    }

    public void setContent(final T content) {
        this.content.getChildren().clear();
        this.content.getChildren().add(content);
    }

    public void setDividerClass(final String cssClass) {
        divider.getStyleClass().clear();
        divider.getStyleClass().addAll("divider", cssClass);
    }

    public T getContent() {
        return (T) this.content.getChildren().get(0);
    }

    public VBox getModalContainer() {
        return this.modalContainer;
    }

    public Label getTopSpacer() {
        return topSpacer;
    }

    public Label getBottomSpacer() {
        return bottomSpacer;
    }

    /**
     * Enable/disable the spacers. The spacers make the modal content shrink vertically to its minimum height.
     *
     * @param show show/hide the spacers
     */
    void showSpacers(final boolean show) {
        topSpacer.setVisible(show);
        topSpacer.setManaged(show);
        bottomSpacer.setVisible(show);
        bottomSpacer.setManaged(show);
    }
}
