package mattw.youtube.commentsuite;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

/**
 * Interface for group refreshing.
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public interface RefreshInterface {

    void run();
    void appendError(String error);
    void shutdown();
    void hardShutdown();
    boolean isAlive();
    void start();

    LongProperty newVideosProperty();
    LongProperty totalVideosProperty();
    LongProperty newCommentsProperty();
    LongProperty totalCommentsProperty();

    BooleanProperty endedProperty();
    DoubleProperty progressProperty();
    StringProperty statusStepProperty();
    StringProperty elapsedTimeProperty();
    ObservableList<String> getObservableErrorList();
    Boolean isEndedOnError();
    Boolean isShutdown();
    Boolean isHardShutdown();
}
