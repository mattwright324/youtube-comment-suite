package mattw.youtube.commentsuite.fxml;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestSearchCommentsController {

    SearchCommentsController controller;

    @Before
    public void setup() {
        controller = new SearchCommentsController();
    }

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

    void assertTrunc(long value, String expected) {
        String result = controller.trunc(value);

        assertEquals(expected, result);
    }

}
