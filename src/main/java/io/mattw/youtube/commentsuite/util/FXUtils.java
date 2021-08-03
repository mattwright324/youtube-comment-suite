package io.mattw.youtube.commentsuite.util;

import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static javafx.application.Platform.runLater;

public class FXUtils {

    private static final Logger logger = LogManager.getLogger();

    private static final Map<TextField, Font> size = new HashMap<>();
    private static final Map<TextField, Double> padding = new HashMap<>();

    public static void registerToSize(final TextField field, final int fontSize) {
        size.put(field, new Font(fontSize));
    }

    public static void registerToPadding(final TextField field, final double paddingSize) {
        padding.put(field, paddingSize);
    }

    /**
     * Modifies a TextField's preferred width based on its text content
     *
     * @param field TextField element
     * @link https://stackoverflow.com/a/25643696/2650847
     */
    public static void adjustTextFieldWidthByContent(final TextField field) {
        runLater(() -> {
            final Text text = new Text(field.getText());
            text.setFont(size.getOrDefault(field, field.getFont()));

            double width = text.getLayoutBounds().getWidth()
                    + padding.getOrDefault(field, field.getPadding().getLeft())
                    + padding.getOrDefault(field, field.getPadding().getRight())
                    + 3d;

            field.setPrefWidth(width);
            field.positionCaret(field.getCaretPosition());
        });
    }
}
