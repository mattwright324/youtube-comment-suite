package io.mattw.youtube.commentsuite.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Helper methods for working with time.
 */
public class DateUtils {

    /**
     * Uses the current timezone.
     */
    public static LocalDateTime epochMillisToDateTime(long epochMillis) {
        return epochMillisToDateTime(epochMillis, ZoneId.systemDefault());
    }

    /**
     * Pass in custom timeZone
     */
    public static LocalDateTime epochMillisToDateTime(long epochMillis, ZoneId zoneId) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId);
    }

}
