package io.mattw.youtube.commentsuite.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Intended for sensitive tokens or values that would be helpful to log but not in their entirety.
 */
public class StringMask {

    public static String maskHalf(String text) {
        final int halfLenth = Optional.ofNullable(text)
                .map(String::length)
                .map(length ->  length / 2)
                .orElse(0);
        return mask(text, halfLenth, '*');
    }

    public static String mask(String text, int length, char maskChar) {
        return Optional.ofNullable(text)
                .orElse(StringUtils.EMPTY)
                .replaceAll(".(?=.{" + length +"})", String.valueOf(maskChar));
    }

}
