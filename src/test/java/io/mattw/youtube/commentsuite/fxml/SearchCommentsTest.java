package io.mattw.youtube.commentsuite.fxml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class SearchCommentsTest {

    private SearchComments controller = new SearchComments();

    @Test
    public void testReadableNumber() {
        assertReadableNumber(123L, "123.0");
        assertReadableNumber(1234L, "1.2k");
        assertReadableNumber(12345L, "12.3k");
        assertReadableNumber(123456L, "123.5k");
        assertReadableNumber(1234567L, "1.2m");
        assertReadableNumber(123000000L, "123.0m");
        assertReadableNumber(2050000000L, "2.1b");
    }

    private void assertReadableNumber(long value, String expected) {
        String result = controller.readableNumber(value);

        assertEquals(expected, result);
    }

}
