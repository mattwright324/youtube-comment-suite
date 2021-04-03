package io.mattw.youtube.commentsuite.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Helper to encode strings to UTF-8 and ignoring the UnsupportedEncodingException.
 * <p>
 * That exception should never ever be thrown, if it does, the world must be ending.
 *
 */
public class UTF8UrlEncoder {

    public static String encode(String string) {
        return URLEncoder.encode(string, StandardCharsets.UTF_8);
    }

}
