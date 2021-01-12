package io.mattw.youtube.commentsuite.fxml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class SearchCommentsTest {

    private SearchComments controller = new SearchComments();

    @Test
    public void testTrunc() {
        assertTrunc(123L, "123.0");
        assertTrunc(1234L, "1.2k");
        assertTrunc(12345L, "12.3k");
        assertTrunc(123456L, "123.5k");
        assertTrunc(1234567L, "1.2m");
        assertTrunc(123000000L, "123.0m");
        assertTrunc(2050000000L, "2.1b");
    }

    private void assertTrunc(long value, String expected) {
        String result = controller.trunc(value);

        assertEquals(expected, result);
    }

}
