package io.mattw.youtube.commentsuite.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Helper to encode strings to UTF-8 and ignoring the UnsupportedEncodingException.
 *
 * That exception should never ever be thrown, if it does, the world must be ending.
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public class UTF8UrlEncoder {

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("For some reason, we couldn't find UTF-8. This should never happen.");
        }
    }

}
