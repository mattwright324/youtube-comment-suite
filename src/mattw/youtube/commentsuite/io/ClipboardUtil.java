package mattw.youtube.commentsuite.io;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mattwright324
 */
public class ClipboardUtil {

	private Clipboard systemClipboard;

	public ClipboardUtil() {}

	@PostConstruct
	private void initSystemClipboard() {
		systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	}

	public String getClipboard() throws UnsupportedFlavorException, IOException {
        return (String) systemClipboard.getData(DataFlavor.stringFlavor);
	}

	/**
	 * Sets clipboard to string value.
	 * @param string
	 */
	public void setClipboard(String string) {
		StringSelection selection = new StringSelection(string);
		systemClipboard.setContents(selection, selection);
	}

	/**
	 * Converts list into a line.separator delimited string and sets to clipboard.
	 * @param list
	 */
	public void setClipboard(List<?> list) {
		List<String> strList = list.stream().map(Object::toString).collect(Collectors.toList());
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
