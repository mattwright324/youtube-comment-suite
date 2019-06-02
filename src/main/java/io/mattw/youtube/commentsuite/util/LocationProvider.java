package io.mattw.youtube.commentsuite.util;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public interface LocationProvider {
    String getRequestUrl(String ipv4);
}
