package mattw.youtube.commentsuite;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;

class IconRenderer extends DefaultTableCellRenderer.UIResource {
	private static final long serialVersionUID = 1L;
	public IconRenderer() {
        super();
        setHorizontalAlignment(JLabel.CENTER);
    }

    public void setValue(Object value) {
        setIcon((value instanceof Icon) ? (Icon) value : null);
    }
}