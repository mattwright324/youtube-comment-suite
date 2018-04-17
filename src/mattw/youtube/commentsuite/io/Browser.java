package mattw.youtube.commentsuite.io;

import java.awt.*;
import java.net.URL;

/**
 * Opens URLs on the system.
 */
public class Browser {

    public static void open(URL url) {
        open(url.toString());
    }

    public static void open(String link) {
        link = link.replace(" ", "%20");
        try {
            URL url = new URL(link);
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(url.toURI());
            } else {
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("xdg-open "+url.getPath());
            }
        } catch (Throwable e2) {
            e2.printStackTrace();
        }
    }
}
