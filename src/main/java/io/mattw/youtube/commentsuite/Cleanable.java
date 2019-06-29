package io.mattw.youtube.commentsuite;

/**
 * For use in instances where UI components can "clean up" and reset their state instead of creating an
 * entirely new instance of the component each time.
 *
 * Most applicable to modals.
 *
 * @author mattwright324
 */
public interface Cleanable {
    void cleanUp();
}
