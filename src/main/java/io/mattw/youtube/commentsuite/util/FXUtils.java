package io.mattw.youtube.commentsuite.util;

import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import static javafx.application.Platform.runLater;

public class FXUtils {

    /**
     * Modifies a TextField's preferred width based on it's text content
     *
     * @param field TextField element
     * @link https://stackoverflow.com/a/25643696/2650847
     */
    public static void adjustTextFieldWidthByContent(TextField field) {
        runLater(() -> {
            Text text = new Text(field.getText());
            text.setFont(field.getFont());
            double width = text.getLayoutBounds().getWidth()
                    + field.getPadding().getLeft() + field.getPadding().getRight()
                    + 3d;
            field.setPrefWidth(width);
            field.positionCaret(field.getCaretPosition());
        });
    }
}
