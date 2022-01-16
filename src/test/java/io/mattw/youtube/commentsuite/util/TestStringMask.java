package io.mattw.youtube.commentsuite.util;

import org.junit.jupiter.api.Test;

public class TestStringMask {

    @Test
    public void testMask() {
        System.out.println(StringMask.maskHalf(null));
        System.out.println(StringMask.maskHalf(""));
        System.out.println(StringMask.maskHalf("123456"));
        System.out.println(StringMask.maskHalf("123456789"));
    }

}
