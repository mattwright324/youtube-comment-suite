package io.mattw.youtube.commentsuite.util;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ClipboardUtil {

    private Clipboard systemClipboard;

    public ClipboardUtil() {
    }

    private void initSystemClipboard() {
        if (systemClipboard == null) {
            systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        }
    }

    private void setSystemClipboard(Clipboard clipboard) {
        this.systemClipboard = clipboard;
    }

    public String getClipboard() throws UnsupportedFlavorException, IOException {
        return (String) systemClipboard.getData(DataFlavor.stringFlavor);
    }

    /**
     * Sets clipboard to string value.
     *
     * @param string text to set clipboard as
     */
    public void setClipboard(String string) {
        initSystemClipboard();
        StringSelection selection = new StringSelection(string);
        systemClipboard.setContents(selection, selection);
    }

    /**
     * Converts list into a line.separator delimited string and sets to clipboard.
     *
     * @param list list of objects converted to line separated toString()
     */
    public void setClipboard(List<?> list) {
        List<String> strList = list.stream().map(Object::toString).collect(Collectors.toList());
        setClipboard(strList.stream().collect(Collectors.joining(System.getProperty("line.separator"))));
    }


    /**
     * Coverts object to string value and sets to clipboard.
     *
     * @param object uses toString() for clipboard
     */
    public void setClipboard(Object object) {
        setClipboard(object.toString());
    }
}
