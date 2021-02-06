package io.mattw.youtube.commentsuite.util;

public class Threads {

    public static void awaitMillis(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

}
