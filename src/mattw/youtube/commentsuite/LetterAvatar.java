package mattw.youtube.commentsuite;

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
 * Generates images with a letter to mimic the style of default YouTube generated letter-avatars.
 *
 * @author mattwright324
 */
public class LetterAvatar extends WritableImage {

    static int BG_SQUARE = 2;

    private int scale = 32;
    private char letter;
    private Color background;
    private int bgStyle;

    public LetterAvatar(char letter) {
        this(letter, Color.LIGHTGRAY);
    }

    public LetterAvatar(char letter, Color bg) {
        this(letter, bg, BG_SQUARE);
    }

    public LetterAvatar(char letter, Color bg, int bgStyle) {
        this(letter, bg, bgStyle, 32);
    }

    public LetterAvatar(char letter, Color bg, int bgStyle, int scale) {
        super(scale, scale);
        this.letter = letter;
        this.background = bg;
        this.bgStyle = bgStyle;
        draw();
    }

    /**
     * Returns a new image rescaled to a new width and height.
     * @param scale width and height of new image
     */
    public Image rescaleTo(int scale) {
        return new LetterAvatar(letter, background, bgStyle, scale);
    }

    public int getScale() {
        return this.scale;
    }

    public void setLetter(char letter) {
        this.letter = letter;
        draw();
    }

    public void setColor(Color bg) {
        this.background = bg;
        draw();
    }

    public void setBackgroundStyle(int bgStyle) {
        this.bgStyle = bgStyle;
        draw();
    }

    private void draw() {
        Canvas canvas = new Canvas(scale, scale);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(0,0, scale, scale);
        gc.setFill(background);
        if(bgStyle == BG_SQUARE) {
            gc.fillRect(0,0, scale, scale);
        } else {
            gc.fillOval(0,0, scale, scale);
        }
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, scale*0.6));
        gc.fillText(letter+"", Math.round(scale/2.0), Math.round(scale/2.0));
        Platform.runLater(() -> canvas.snapshot(null, this));
    }
}
