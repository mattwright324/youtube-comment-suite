package mattw.youtube.commentsuite.io;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class ClipboardUtil {

	private Clipboard systemClipboard;

	public ClipboardUtil() {}

	private void initSystemClipboard() {
		if(systemClipboard == null) {
			systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		}
	}

	public String getClipboard() throws UnsupportedFlavorException, IOException {
		initSystemClipboard();

        return (String) systemClipboard.getData(DataFlavor.stringFlavor);
	}

	/**
	 * Sets clipboard to string value.
	 * @param string
	 */
	public void setClipboard(String string) {
		initSystemClipboard();

		StringSelection selection = new StringSelection(string);
		systemClipboard.setContents(selection, selection);
	}

	/**
	 * Converts list into a line.separator delimited string and sets to clipboard.
	 * @param list
	 */
	public void setClipboard(List<? extends Object> list) {
		List<String> strList = list.stream().map(obj -> obj.toString()).collect(Collectors.toList());
		setClipboard(strList.stream().collect(Collectors.joining(System.getProperty("line.separator"))));
	}

	/**
	 * Coverts object to string value and sets to clipboard.
	 * @param object
	 */
	public void setClipboard(Object object) {
		setClipboard(object.toString());
	}
}
