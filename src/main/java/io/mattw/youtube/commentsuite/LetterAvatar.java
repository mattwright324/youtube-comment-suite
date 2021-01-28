package io.mattw.youtube.commentsuite;

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Generates images with a character to mimic the style of default YouTube generated letter-avatars.
 *
 */
public class LetterAvatar extends WritableImage {

    private int scale = 32;
    private char character;
    private Color background;

    LetterAvatar(char character) {
        this(character, Color.LIGHTGRAY);
    }

    LetterAvatar(char character, Color bg) {
        this(character, bg, 32);
    }

    LetterAvatar(char character, Color bg, int scale) {
        super(scale, scale);

        this.character = character;
        this.background = bg;

        draw();
    }

    /**
     * Returns a new image rescaled to a new width and height.
     *
     * @param scale width and height of new image
     */
    public Image rescaleTo(int scale) {
        return new LetterAvatar(character, background, scale);
    }

    public int getScale() {
        return this.scale;
    }

    public void setCharacter(char character) {
        this.character = character;
        draw();
    }

    public void setColor(Color bg) {
        this.background = bg;
        draw();
    }

    private void draw() {
        Canvas canvas = new Canvas(scale, scale);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(0, 0, scale, scale);

        gc.setFill(background);
        gc.fillRect(0, 0, scale, scale);

        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, scale * 0.6));

        gc.fillText(String.valueOf(character), Math.round(scale / 2.0), Math.round(scale / 2.0));
        Platform.runLater(() -> canvas.snapshot(null, this));
    }
}
