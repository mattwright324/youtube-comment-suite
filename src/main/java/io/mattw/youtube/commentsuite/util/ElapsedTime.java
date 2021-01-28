package io.mattw.youtube.commentsuite.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Gives a duration from a given date and the current time.
 *
 */
public class ElapsedTime {

    private LocalDateTime time;

    public ElapsedTime() {
        setNow();
    }

    public void setNow() {
        time = LocalDateTime.now();
    }

    public void setAs(LocalDateTime startTime) {
        this.time = startTime;
    }

    public Duration getElapsed() {
        return Duration.between(time, LocalDateTime.now());
    }

    /**
     * @link https://stackoverflow.com/a/40487511/2650847
     */
    public String humanReadableFormat() {
        return getElapsed().toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }

}
