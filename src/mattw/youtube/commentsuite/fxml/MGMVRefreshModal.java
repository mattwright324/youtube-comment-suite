package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.MGMVGroupRefresh;
import mattw.youtube.commentsuite.RefreshInterface;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.commentsuite.io.ClipboardUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Modal for group refreshing set as content within an OverlayModal.
 *
 * @author mattwright324
 */
public class MGMVRefreshModal extends HBox {

    private static Logger logger = LogManager.getLogger(MGMVRefreshModal.class.getSimpleName());

    private ClipboardUtil clipboard = new ClipboardUtil();

    private @FXML Label alert;
    private @FXML Label statusStep;
    private @FXML Button btnClose;
    private @FXML Button btnStart;
    private @FXML ProgressBar progressBar;
    private @FXML VBox statusPane;

    private @FXML HBox warningsPane;
    private @FXML Label warnings, elapsedTime, newVideos, newComments, totalVideos, totalComments;
    private @FXML ImageView expandIcon;
    private @FXML ListView<String> errorList;
    private @FXML Hyperlink expand;
    private @FXML ImageView endStatus;
    private @FXML ProgressIndicator statusIndicator;

    private Group group;
    private RefreshInterface refreshThread;
    private boolean running = false;

    private boolean expanded = false;

    public MGMVRefreshModal(Group group) {
        logger.debug(String.format("Initialize for Group [id=%s,name=%s]", group.getId(), group.getName()));

        this.group = group;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MGMVRefreshModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            expandIcon.setImage(ImageLoader.ANGLE_RIGHT.getImage());

            expand.setOnAction(ae -> {
                expanded = !expanded;
                runLater(() -> {
                    if(expanded) {
                        expandIcon.setImage(ImageLoader.ANGLE_LEFT.getImage());
                        errorList.setManaged(true);
                        errorList.setVisible(true);
                    } else {
                        expandIcon.setImage(ImageLoader.ANGLE_RIGHT.getImage());
                        errorList.setManaged(false);
                        errorList.setVisible(false);
                    }
                });
            });

            errorList.setOnKeyPressed(ke -> {
                if(ke.getCode() == KeyCode.C && ke.isControlDown()) {
                    clipboard.setClipboard(errorList.getItems());
                }
            });

            btnStart.setOnAction(ae -> new Thread(() -> {
                if(running) {
                    logger.debug(String.format("Requesting group refresh stopped for group [id=%s,name=%s]", group.getId(), group.getName()));
                    runLater(() -> {
                        btnStart.setDisable(true);
                        endStatus.setImage(ImageLoader.MINUS_CIRCLE.getImage());
                    });
                    refreshThread.hardShutdown();
                    while(refreshThread.isAlive()) {
                        try { Thread.sleep(97); } catch (Exception ignored) {}
                    }
                    running = false;
                    runLater(() -> {
                        btnClose.setDisable(false);
                    });
                } else {
                    running = true;
                    logger.debug(String.format("Starting group refresh for group [id=%s,name=%s]", group.getId(), group.getName()));
                    runLater(() -> {
                        statusPane.setVisible(true);
                        statusPane.setManaged(true);
                        btnClose.setDisable(true);
                        btnStart.setText("Stop");
                        alert.setVisible(false);
                        alert.setManaged(false);
                    });

                    refreshThread = new MGMVGroupRefresh(group);
                    runLater(() -> {
                        refreshThread.getObservableErrorList().addListener((ListChangeListener<String>)(lcl) -> runLater(() -> {
                            int items = lcl.getList().size();
                            warningsPane.setManaged(items > 0);
                            warningsPane.setVisible(items > 0);
                            warnings.setText(items+" message(s)");
                        }));
                        errorList.setItems(refreshThread.getObservableErrorList());
                        elapsedTime.textProperty().bind(refreshThread.elapsedTimeProperty());
                        progressBar.progressProperty().bind(refreshThread.progressProperty());
                        statusStep.textProperty().bind(refreshThread.statusStepProperty());
                        newVideos.textProperty().bind(Bindings.format("%,d", refreshThread.newVideosProperty()));
                        totalVideos.textProperty().bind(
                                Bindings.concat("of ")
                                        .concat(Bindings.format("%,d", refreshThread.totalVideosProperty()))
                                        .concat(" total"));
                        newComments.textProperty().bind(Bindings.format("%,d", refreshThread.newCommentsProperty()));
                        totalComments.textProperty().bind(
                                Bindings.concat("of ")
                                        .concat(Bindings.format("%,d", refreshThread.totalCommentsProperty()))
                                        .concat(" total"));
                    });
                    refreshThread.start();
                    refreshThread.endedProperty().addListener((o, ov, nv) -> {
                        progressBar.progressProperty().unbind();
                        runLater(() -> {
                            btnStart.setVisible(false);
                            btnStart.setManaged(false);
                            btnClose.setDisable(false);
                            endStatus.setImage(refreshThread.isEndedOnError() ?
                                    ImageLoader.TIMES_CIRCLE.getImage() : ImageLoader.CHECK_CIRCLE.getImage());
                            endStatus.setManaged(true);
                            endStatus.setVisible(true);
                            statusIndicator.setManaged(false);
                            statusIndicator.setVisible(false);
                        });
                    });
                }
            }).start());
        } catch (IOException e) { logger.error(e); }
    }

    /**
     * Reset the modal back to its original state when being opened.
     */
    public void reset() {
        logger.debug("Resetting state of Refresh Modal");
        running = false;
        runLater(() -> {
            if(expanded) {
                expand.fire();
            }
            endStatus.setManaged(false);
            endStatus.setVisible(false);
            statusIndicator.setManaged(true);
            statusIndicator.setVisible(true);
            errorList.getItems().clear();
            alert.getStyleClass().remove("alertSuccess");
            alert.getStyleClass().add("alertWarning");
            alert.setVisible(true);
            alert.setManaged(true);
            statusPane.setVisible(false);
            statusPane.setManaged(false);
            btnStart.setVisible(true);
            btnStart.setManaged(true);
            btnStart.setDisable(false);
            btnStart.setText("Start");
            btnStart.getStyleClass().remove("btnWarning");
            btnStart.getStyleClass().add("btnPrimary");
        });
    }

    public Button getBtnClose() { return btnClose; }

    public Button getBtnStart() { return btnStart; }

    public ListView<String> getErrorList() { return errorList; }
}
