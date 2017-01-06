package mattw.youtube.commentsuite;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jdesktop.swingx.JXTextField;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import mattw.youtube.datav3.YoutubeData;
import mattw.youtube.datav3.list.ChannelsList;
import mattw.youtube.datav3.list.CommentThreadsList;
import mattw.youtube.datav3.list.CommentsList;
import mattw.youtube.datav3.list.PlaylistItemsList;
import mattw.youtube.datav3.list.SearchList;
import mattw.youtube.datav3.list.VideosList;

public class CommentSuite extends JFrame implements ActionListener {
	
	/*
	 * YoutubeData . getJson() uses 95% of the CPU time in VisualVM
	 */
	
	private static final long serialVersionUID = -7765160087637880819L;
	
	public static CommentSuite window;
	public static SuiteDatabase db = new SuiteDatabase("commentsuite.db");
	public static String title = "Youtube Comment Suite";
	
	public String youtubeDataKey;
	public Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.PRIVATE).create();
	public YoutubeData data;
	public String dateFormatString = "yyyy-MM-dd hh:mm a";
	public Font youtube_font = new Font("Arial", Font.PLAIN, 12);
	public ElapsedTime timer = new ElapsedTime();
	public Insets margin = new Insets(2, 4, 2, 4);
	
	public JPanel videosPanel, groupsPanel, commentsPanel, settingsPanel;
	public JTabbedPane display;
	
	public ImageIcon imgFind = new ImageIcon(getImageResource("/mattw/youtube/commentsuite/images/find.png"));
	public ImageIcon imgManage = new ImageIcon(getImageResource("/mattw/youtube/commentsuite/images/manage.png"));
	public ImageIcon imgSearch = new ImageIcon(getImageResource("/mattw/youtube/commentsuite/images/search.png"));
	public ImageIcon imgSettings = new ImageIcon(getImageResource("/mattw/youtube/commentsuite/images/settings.png"));
	public ImageIcon imgThumbPlaceholder = new ImageIcon(getImageResource("/mattw/youtube/commentsuite/images/placeholder4.png"));
	public ImageIcon imgThumbPlaceholderBlank = new ImageIcon(getImageResource("/mattw/youtube/commentsuite/images/placeholder3.png"));
	public ImageIcon imgBlankProfile = new ImageIcon(getImageResource("/mattw/youtube/commentsuite/images/blank_profile.jpg"));
	public ImageIcon imgBrowser = new ImageIcon(getImageResource("/mattw/youtube/commentsuite/images/browser.png").getScaledInstance(16, 16, 0));
	
		/** Videos Panel Components **/
	public JButton find, selectall, clearResults, addAsGroup, nextPage;
	public JXTextField videosTerm;
	public JTable vTable;
	public DefaultTableModel vModel;
	public JComboBox<String> findSort, findType;
	public SearchList slr;
	public JMenuItem videoOIB;
	
		/** Groups Panel Components **/
	public JTable gTable;
	public DefaultTableModel gModel;
	public JButton addGroup, deleteGroup, editName, refreshGroup, addItem, deleteItem;
	public JTabbedPane tabs;
	public JTable groupItemTable;
	public DefaultTableModel groupItemModel;
	public JTable groupVideoTable;
	public DefaultTableModel groupVideoModel;
	public JProgressBar groupLoad;
	public JPanel groups;
	public JPanel analytics;
	public JButton analyze;
	public JComboBox<GroupItem> gitems;
	public JComboBox<String> aType;
	public JProgressBar progress;
	public JTextPane results;
	
	
		/** Comments Panel Components **/
	public JPanel comment_list;
	public JLabel videoThumb;
	public JLabel videoAuthorProfile;
	public JTextField videoTitle, videoAuthorName;
	public JLabel videoStats;
	public JTextPane videoDesc;
	public String defaultDescription = "<html>"
			+ "Come visit the <a href='https://github.com/mattwright324'>Github Page<a> for the latest release!"
			+ "<br>"
			+ "<br>This program makes use of the following libraries:"
			+ "<ol>"
			+ "<li><a href='https://github.com/google/gson'>Google Gson 2.8.0</a></li>"
			+ "<li><a href='https://java.net/projects/swingx/downloads/directory/releases'>SwingX 1.6.4</a></li>"
			+ "<li><a href='https://bitbucket.org/xerial/sqlite-jdbc/downloads'>Sqlite JDBC 3.8.11.1<a></li>"
			+ "<li><a href='https://github.com/mattwright324/youtube-data-list'>youtube-data-list<a></li>"
			+ "</ol>"
			+ "</html>";
	public JTable cTable;
	public Rectangle lastRect;
	public int selectedRow = 0;
	public DefaultTableModel cModel;
	public JXTextField fieldName, fieldText;
	public JComboBox<String> orderBy, type;
	public JButton findComments;
	public JComboBox<Group> commentGroup;
	public JComboBox<GroupItem> itemGroup;
	public JMenuItem viewFullComment, viewCommentTree, openVideo, openProfile, downloadProfiles;
	public List<Comment> foundComments;
	public List<Comment> commentTree;
	public JButton backToResults;
	public JPanel searchComments;
	public JButton random;
	public JSpinner randomCount;
	public SpinnerNumberModel numberModel = new SpinnerNumberModel(10,1,5000,1);
	public JCheckBox isFair;
	
	
		/** Settings Panel Components **/
	public JButton saveKey;
	public JXTextField keyField;
	public JButton reset, clean;
	public File keyFile = new File("youtubedata.key");
	
	public String getKey() throws IOException {
		if(!keyFile.exists()) keyFile.createNewFile();
		BufferedReader br = new BufferedReader(new FileReader(keyFile));
		String key = br.readLine();
		br.close();
		return key;
	}
	
	public void setKey(String key) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(keyFile));
		bw.write(key);
		bw.close();
		youtubeDataKey = key;
		data = new YoutubeData(youtubeDataKey);
		if(youtubeDataKey.equals("")) {
			setTitle(title+" - Data key not set.");
		} else {
			setTitle(title);
		}
	}
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Throwable e) {}
		try {
			window = new CommentSuite();
			window.setVisible(true);
			new Thread(new Runnable(){
				public void run() {
					try {
						db.create();
						window.setupFromDatabase();
					} catch (ClassNotFoundException | SQLException | ParseException e) {
						window.setTitle(title+" - "+e.getMessage());
						e.printStackTrace();
					}
				}
			}).start();
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, e1.getClass().getName()+": "+e1.getLocalizedMessage(), "Problem on Run", JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace();
		}
	}
	
	public CommentSuite() throws IOException {
		setTitle("Youtube Comment Suite");
		setSize(1200, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setIconImage(getImageResource("/mattw/youtube/commentsuite/images/icon.png"));
		
		youtubeDataKey = getKey();
		data = new YoutubeData(youtubeDataKey);
		
		buildVideosPanel();
		buildGroupsPanel();
		buildCommentsPanel();
		buildSettingsPanel();
		
		display = new JTabbedPane();
		display.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		display.addTab("Find Videos", imgFind, videosPanel, "Find channels and videos. Add them to groups.");
		display.addTab("Manage Groups", imgManage, groupsPanel, "Manage video/channel groups you are searching.");
		display.addTab("Search Comments", imgSearch, commentsPanel, "Search the currently selected video group.");
		display.addTab("View Settings", imgSettings, settingsPanel, "Change to your liking.");
		
		add(display, BorderLayout.CENTER);
	}
	
	public void setupFromDatabase() throws SQLException, ParseException {
		videosTerm.setText("");
		vModel.setRowCount(0);
		nextPage.setEnabled(false);
		slr = null;
		
		refreshGroupTable();
		
		videoThumb.setIcon(imgThumbPlaceholder);
		videoThumb.setToolTipText("");
		videoAuthorName.setText("Hello World!");
		videoAuthorProfile.setIcon(imgBlankProfile);
		videoAuthorProfile.setToolTipText("");
		videoDesc.setText(defaultDescription);
		cModel.setRowCount(0);
		searchComments.setBorder(BorderFactory.createTitledBorder("Comments"));
		fieldName.setText("");
		fieldText.setText("");
	}
	
	public void buildVideosPanel() {
		videosPanel = new JPanel();
		videosPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = margin;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1.0;
		
		gbc.weightx = 0.1;
		gbc.gridx = 0;
		videosPanel.add(new JLabel(), gbc);
		gbc.gridx = 2;
		videosPanel.add(new JLabel(), gbc);
		gbc.weightx = 1;
		gbc.gridx = 1;
		
		JPanel first = new JPanel(new GridBagLayout());
		videosPanel.add(first, gbc);
		first.setBackground(new Color(220, 220, 220));
		
		gbc.ipady = 8;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.gridx = 0;
		first.add(find = new JButton(getScaledImageIcon(imgFind, 16, 16)), gbc);
		find.addActionListener(this);
		find.setBackground(Color.getHSBColor(0.99F, 0.20F, 0.8F));
		
		gbc.gridx++;
		gbc.weightx = 1.0;
		first.add(videosTerm = new JXTextField("Enter search term here."), gbc);
		
		gbc.gridx++;
		gbc.weightx = 0;
		first.add(findSort = new JComboBox<String>(new String[]{"Relevance", "Date", "Rating", "Title", "Views"}), gbc);
		
		gbc.gridx++;
		gbc.weightx = 0;
		first.add(findType = new JComboBox<String>(new String[]{"Everything", "Videos", "Channels", "Playlists"}), gbc);
		
		gbc.gridx++;
		gbc.weightx = 0;
		first.add(nextPage = new JButton("Next Page"), gbc);
		nextPage.addActionListener(this);
		nextPage.setEnabled(false);
		
		videoOIB = new JMenuItem("Open In Browser");
		videoOIB.addActionListener(this);
		
		vModel = new DefaultTableModel(new String[]{"Add", "Type", "Thumb", "Title", "Published"}, 0);
		vTable = new JTable(vModel) {
			private static final long serialVersionUID = 1L;
			public boolean isCellEditable(int row, int col) {
				return col == 0;
			}
			public Class<?> getColumnClass(int c) {
		        return getValueAt(0, c).getClass();
		    }
		};
		vTable.setRowHeight(90);
		vTable.setShowGrid(false);
		vTable.setCellSelectionEnabled(true);
		vTable.getColumnModel().getColumn(0).setCellRenderer(vTable.getDefaultRenderer(Boolean.class));
		vTable.getColumnModel().getColumn(2).setCellRenderer(new IconRenderer());
		vTable.getTableHeader().setReorderingAllowed(false);
		int[] cols = new int[]{55, 75, 130, -1, 180};
		for(int i=0; i<cols.length; i++) {
			if(cols[i] != -1) {
				vTable.getColumnModel().getColumn(i).setMinWidth(cols[i]);
				vTable.getColumnModel().getColumn(i).setPreferredWidth(cols[i]);
				vTable.getColumnModel().getColumn(i).setMaxWidth(cols[i]);
			}
		}
		vTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			private static final long serialVersionUID = 1L;
			public Font ytfont = new Font("Arial", Font.PLAIN, 12);
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				if(col == 1) {
					setHorizontalAlignment(SwingConstants.CENTER);
				} else if(col == 4) {
					setHorizontalAlignment(SwingConstants.TRAILING);
				} else {
					setHorizontalAlignment(SwingConstants.LEADING);
				}
				setFont(ytfont);
				return this;
			}
		});
		vTable.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {
				if(e.isPopupTrigger())
					doPopup(e);
			}
			public void mouseReleased(MouseEvent e) {
				if(e.isPopupTrigger())
					doPopup(e);
			}
			public void doPopup(MouseEvent e) {
				final JTable source = (JTable) e.getSource();
				final int row = source.rowAtPoint(e.getPoint());
				final int col = source.columnAtPoint(e.getPoint());
				if(!source.isCellSelected(row, col))
					source.setRowSelectionInterval(row, row);
				
				final JPopupMenu popup = new JPopupMenu();
				popup.add(videoOIB);
				popup.show(source, e.getX(), e.getY());
			}
		});
		
		GridBagConstraints gbca = new GridBagConstraints();
		gbca.gridy = 1;
		gbca.gridwidth = 5;
		JPanel second = new JPanel(new GridBagLayout());
		second.setOpaque(false);
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		first.add(second, gbca);
		
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.insets = margin;
		gbc1.gridx = 0;
		gbc1.gridy = 0;
		gbc1.ipady = 8;
		second.add(selectall = new JButton("(De)Select All"), gbc1);
		selectall.addActionListener(this);
		gbc1.gridx = 1;
		second.add(clearResults = new JButton("Clear Results"), gbc1);
		clearResults.addActionListener(this);
		gbc1.gridx = 2;
		second.add(addAsGroup = new JButton("Add To Group"), gbc1);
		addAsGroup.addActionListener(this);
		
		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.fill = GridBagConstraints.BOTH;
		gbc2.weightx = 1;
		gbc2.weighty = 1;
		gbc2.gridwidth = 8;
		gbc2.gridy = 2;
		gbc2.insets = margin;
		first.add(new JScrollPane(vTable), gbc2);
	}
	
	public void buildGroupsPanel() { // TODO
		groupsPanel = new JPanel();
		groupsPanel.setLayout(new BorderLayout());
		
		JSplitPane split = new JSplitPane();
		groupsPanel.add(split, BorderLayout.CENTER);
		
		JPanel first = new JPanel(new GridBagLayout());
		split.setLeftComponent(first);
		
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.insets = margin;
		gbc1.fill = GridBagConstraints.BOTH;
		gbc1.weightx = 0.05;
		gbc1.weighty = 1.0;
		
		
		groups = new JPanel(new GridBagLayout());
		groups.setBorder(BorderFactory.createTitledBorder("Select a Group"));
		groups.setOpaque(false);
		first.add(groups, gbc1);
		
		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.insets = margin;
		gbc2.fill = GridBagConstraints.HORIZONTAL;
		
		addGroup = new JButton("Add Group");
		addGroup.addActionListener(this);
		gbc2.gridy = 0;
		gbc2.gridx = 0;
		gbc2.weightx = 1.0;
		groups.add(addGroup, gbc2);
		
		deleteGroup = new JButton("Clear Group");
		deleteGroup.addActionListener(this);
		gbc2.gridx = 1;
		groups.add(deleteGroup, gbc2);
		
		editName = new JButton("Edit Name");
		editName.addActionListener(this);
		editName.setEnabled(false);
		gbc2.gridx = 2;
		groups.add(editName, gbc2);
		
		refreshGroup = new JButton("Refresh Group");
		refreshGroup.addActionListener(this);
		gbc2.gridx = 0;
		gbc2.gridwidth = 3;
		gbc2.gridy = 1;
		groups.add(refreshGroup, gbc2);
		
		gModel = new DefaultTableModel(new String[]{"Groups",""}, 0);
		gTable = new JTable(gModel){
			private static final long serialVersionUID = 8033433213952718226L;
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
		gTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		gTable.setRowHeight(45);
		gTable.setEnabled(false);
		gTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			private static final long serialVersionUID = 1L;
			public Font ytfont = new Font("Arial", Font.PLAIN, 12);
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				if(col == 0) {
					setHorizontalAlignment(SwingConstants.LEADING);
				} else {
					setHorizontalAlignment(SwingConstants.CENTER);
				}
				setFont(ytfont);
				return this;
			}
		});
		gTable.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {
				// loadGroup(e);
			}
			public void mouseReleased(MouseEvent e) {
				loadGroup(e);
			}
			public void loadGroup(MouseEvent e) {
				if(e.getClickCount() == 1) {
					Thread load = new Thread(new Runnable(){
						public void run() {
							gTable.setEnabled(false);
							int row = gTable.getSelectedRow();
							Group g = (Group) gTable.getValueAt(row, 0);
							setComponentsEnabled(row != 0 && !g.isRefreshing(), editName);
							setComponentsEnabled(!g.isRefreshing(), deleteGroup, refreshGroup);
							if(row != 0) {
								deleteGroup.setText("Delete Group");
							} else {
								deleteGroup.setText("Clear Group");
							}
							try {
								tabs.setTitleAt(0, "Group Items [0]");
								tabs.setTitleAt(1, "Related Videos [0]");
								groupItemModel.setRowCount(0);
								groupVideoModel.setRowCount(0);
								loadSelectedGroup(g);
							} catch (SQLException e1) {
								e1.printStackTrace();
							} catch (ParseException e1) {
								e1.printStackTrace();
							}
							gTable.setEnabled(true);
						}
					});
					load.start();
				}
			}
		});
		
		gbc2.gridy = 2;
		gbc2.weighty = 1;
		gbc2.fill = GridBagConstraints.BOTH;
		groups.add(new JScrollPane(gTable), gbc2);
		
		tabs = new JTabbedPane();
		gbc1.weightx = 1;
		split.setRightComponent(tabs);
		
		JPanel groupitems = new JPanel(new GridBagLayout());
		tabs.addTab("Group Items [0]", groupitems);
		
		GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.insets = margin;
		gbc3.weightx = 0;
		gbc3.fill = GridBagConstraints.HORIZONTAL;
		
		addItem = new JButton("Add Item");
		addItem.addActionListener(this);
		gbc3.gridx = 0;
		gbc3.gridy = 0;
		// groupitems.add(addItem, gbc3);
		
		deleteItem = new JButton("Delete Item(s)");
		deleteItem.addActionListener(this);
		gbc3.gridx = 1;
		gbc3.gridy = 0;
		// groupitems.add(deleteItem, gbc3);
		
		gbc3.weightx = 1;
		gbc3.gridx = 3;
		groupitems.add(new JLabel(), gbc3);
		
		groupItemModel = new DefaultTableModel(new String[]{"Type", "Thumb", "Title", "Published", "Last Checked"}, 0);
		groupItemTable = new JTable(groupItemModel) {
			private static final long serialVersionUID = 1L;
			public boolean isCellEditable(int row, int col) {
				return col == 0;
			}
			public Class<?> getColumnClass(int c) {
				if(getValueAt(0, c) != null)
					return getValueAt(0, c).getClass();
				else
					return Object.class;
		    }
		};
		groupItemTable.setRowHeight(45);
		groupItemTable.setShowGrid(false);
		groupItemTable.setCellSelectionEnabled(true);
		groupItemTable.getColumnModel().getColumn(1).setCellRenderer(new IconRenderer());
		groupItemTable.getTableHeader().setReorderingAllowed(false);
		int[] cols = new int[]{75, 95, -1, 180};
		for(int i=0; i<cols.length; i++) {
			if(cols[i] != -1) {
				groupItemTable.getColumnModel().getColumn(i).setMinWidth(cols[i]);
				groupItemTable.getColumnModel().getColumn(i).setPreferredWidth(cols[i]);
				groupItemTable.getColumnModel().getColumn(i).setMaxWidth(cols[i]);
			}
		}
		groupItemTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			private static final long serialVersionUID = 1L;
			public Font ytfont = new Font("Arial", Font.PLAIN, 12);
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				if(col == 0) {
					setHorizontalAlignment(SwingConstants.CENTER);
				} else if(col == 3) {
					setHorizontalAlignment(SwingConstants.TRAILING);
				} else {
					setHorizontalAlignment(SwingConstants.LEADING);
				}
				setFont(ytfont);
				return this;
			}
		});
		
		gbc3.fill = GridBagConstraints.BOTH;
		gbc3.gridy = 0;
		gbc3.gridx = 0;
		gbc3.weightx = 1;
		gbc3.weighty = 1;
		gbc3.gridwidth = 4;
		groupitems.add(new JScrollPane(groupItemTable), gbc3);
		
		JPanel videolist = new JPanel(new GridBagLayout());
		tabs.addTab("Related Videos [0]", videolist);
		
		GridBagConstraints gbc4 = new GridBagConstraints();
		gbc4.insets = margin;
		
		groupVideoModel = new DefaultTableModel(new String[]{"Type", "Thumb", "Title", "Published", "Last Checked"}, 0);
		groupVideoTable = new JTable(groupVideoModel) {
			private static final long serialVersionUID = 1L;
			public boolean isCellEditable(int row, int col) {
				return col == 0;
			}
			public Class<?> getColumnClass(int c) {
				if(getValueAt(0, c) != null)
					return getValueAt(0, c).getClass();
				else
					return Object.class;
		    }
		};
		groupVideoTable.setRowHeight(45);
		groupVideoTable.setShowGrid(false);
		groupVideoTable.setCellSelectionEnabled(true);
		groupVideoTable.getColumnModel().getColumn(1).setCellRenderer(new IconRenderer());
		groupVideoTable.getTableHeader().setReorderingAllowed(false);
		int[] cols2 = new int[]{75, 95, -1, 180};
		for(int i=0; i<cols2.length; i++) {
			if(cols2[i] != -1) {
				groupVideoTable.getColumnModel().getColumn(i).setMinWidth(cols2[i]);
				groupVideoTable.getColumnModel().getColumn(i).setPreferredWidth(cols2[i]);
				groupVideoTable.getColumnModel().getColumn(i).setMaxWidth(cols2[i]);
			}
		}
		groupVideoTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			private static final long serialVersionUID = 1L;
			public Font ytfont = new Font("Arial", Font.PLAIN, 12);
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				if(col == 0 || col == 4) {
					setHorizontalAlignment(SwingConstants.CENTER);
				} else if(col == 3) {
					setHorizontalAlignment(SwingConstants.TRAILING);
				} else {
					setHorizontalAlignment(SwingConstants.LEADING);
				}
				setFont(ytfont);
				return this;
			}
		});
		
		groupLoad = new JProgressBar();
		groupLoad.setStringPainted(true);
		groupLoad.setString("");
		groupLoad.setIndeterminate(true);
		
		gbc4.fill = GridBagConstraints.BOTH;
		gbc4.weightx = 1;
		gbc4.weighty = 1;
		gbc4.gridwidth = 4;
		videolist.add(new JScrollPane(groupVideoTable), gbc4);
		
		analytics = new JPanel(new GridBagLayout());
		tabs.addTab("Analytics", analytics);
		
		GridBagConstraints gbc5 = new GridBagConstraints();
		gbc5.insets = margin;
		gbc5.fill = GridBagConstraints.HORIZONTAL;
		gbc5.weightx = 0.2;
		
		analyze = new JButton("Analyze");
		analyze.addActionListener(this);
		gbc5.gridx = 0;
		gbc5.gridy = 0;
		analytics.add(analyze, gbc5);
		
		gitems = new JComboBox<GroupItem>();
		gbc5.gridx = 1;
		gbc5.gridy = 0;
		analytics.add(gitems, gbc5);
		
		aType = new JComboBox<String>(new String[]{"Comments", "Videos"});
		gbc5.gridx = 2;
		gbc5.gridy = 0;
		analytics.add(aType, gbc5);
		
		progress = new JProgressBar();
		progress.setStringPainted(true);
		progress.setString("");
		progress.setIndeterminate(false);
		gbc5.gridx = 3;
		gbc5.gridy = 0;
		gbc5.ipady = 5;
		gbc5.weightx = 1;
		analytics.add(progress, gbc5);
		
		results = new JTextPane();
		results.setContentType("text/html");
		results.setEditable(false);
		results.setText("<html><body></body></html>");
		results.setFont(youtube_font);
		results.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		results.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					openInBrowser(e.getURL().toString());
				}
			}
		});
		HTMLEditorKit editor = new HTMLEditorKit();
		HTMLDocument doc = (HTMLDocument) editor.createDefaultDocument();
		StyleSheet ss = doc.getStyleSheet();
		ss.addRule("table {}");
		ss.addRule("table tr:odd {background-color:rgb(120,120,120)}");
		ss.addRule("table td:last-child {width: 100%;}");
		editor.setStyleSheet(ss);
		results.setEditorKit(editor);
		gbc5.gridx = 0;
		gbc5.gridy = 1;
		gbc5.fill = GridBagConstraints.BOTH;
		gbc5.weightx = 1;
		gbc5.weighty = 1;
		gbc5.gridwidth = 5;
		analytics.add(new JScrollPane(results), gbc5);
		
		split.setResizeWeight(0.0);
		split.setDividerLocation(split.getMaximumDividerLocation());
	}
	
	public void refreshGroupTable() throws SQLException, ParseException {
		gTable.setEnabled(false);
		int selection = gTable.getSelectedRow();
		gModel.setRowCount(0);
		commentGroup.removeAllItems();
		for(Group g : db.getGroups()) {
			gModel.addRow(new Object[]{g});
			commentGroup.addItem(g);
		}
		if(selection == -1 || selection >= gTable.getRowCount()) {
			gTable.setRowSelectionInterval(0, 0);
		} else {
			gTable.setRowSelectionInterval(selection, selection);
		}
		if(gTable.getRowCount() > 0) {
			loadSelectedGroup((Group) gTable.getValueAt(0, 0));
			gTable.setEnabled(true);
		}
	}
	
	public void loadSelectedGroup(Group g) throws SQLException, ParseException {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.ipady = 7;
		gbc.weightx = 1;
		gbc.insets = margin;
		groups.add(groupLoad, gbc);
		groups.validate();
		
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString);
		System.out.println("Loading Group ["+g.group_name+"] ("+g.group_id+")");
		List<GroupItem> gi = db.getGroupItems(g.group_name, true);
		List<Video> videos = db.getVideos(g.group_name, true);
		System.out.println(gi.size()+" group items / "+videos.size()+" videos");
		tabs.setTitleAt(0, "Group Items ["+gi.size()+"]");
		tabs.setTitleAt(1, "Related Videos ["+videos.size()+"]");
		groupItemModel.setRowCount(0);
		groupVideoModel.setRowCount(0);
		results.setText("");
		gitems.removeAllItems();
		gitems.addItem(new GroupItem(-1, null, null, "All Items ("+gi.size()+")", null, null, new Date(0), null, false));
		for(GroupItem item : gi) {
			String html = "<html><div style='text-align:right'>"+item.channel_title+"<br><div style='color:rgb(140,140,140)'>"+sdf.format(item.published)+"</div></div></html>";
			groupItemModel.addRow(new Object[]{item.type, item.thumbnail, item, html, item.last_checked.getTime() != 0 ? sdf.format(item.last_checked):""});
			gitems.addItem(item);
		}
		for(Video v : videos) {
			String html = "<html><div style='text-align:right'>"+v.channel.channel_name+"<br><div style='color:rgb(140,140,140)'>"+sdf.format(v.publish_date)+"</div></div></html>";
			String check_status = v.http_code == 200 ? v.comment_count+" comments" : "<div style='color:red'>"+(v.http_code == 403 ? "Comments Disabled" : "HTTP "+v.http_code)+"</div>";
			String checked_html = "<html><div style='text-align:center'>"+sdf.format(v.grab_date)+"<br>"+check_status+"</div></html>";
			groupVideoModel.addRow(new Object[]{"video", v.small_thumb, v, html, checked_html});
		}
		
		groups.remove(groupLoad);
		groups.validate();
	}
	
	public void buildCommentsPanel() { // TODO
		commentsPanel = new JPanel();
		commentsPanel.setLayout(new BorderLayout());
		
		JSplitPane split1 = new JSplitPane();
		
		JSplitPane split2 = new JSplitPane();
		commentsPanel.add(split1, BorderLayout.CENTER);
		split1.setLeftComponent(split2);
		
		split2.setBorder(BorderFactory.createEmptyBorder());
		
		JPanel video_info = new JPanel(new GridBagLayout());
		split2.setLeftComponent(video_info);
		
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.insets = margin;
		gbc1.gridx = 0;
		gbc1.gridy = 0;
		gbc1.fill = GridBagConstraints.BOTH;
		gbc1.weightx = 1;
		gbc1.gridwidth = 3;
		
		videoThumb = new JLabel(imgThumbPlaceholder);
		videoThumb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		videoThumb.setToolTipText("Click to open video in browser.");
		videoThumb.setPreferredSize(new Dimension(320, 180));
		videoThumb.setBackground(Color.GRAY);
		videoThumb.setBorder(BorderFactory.createDashedBorder(Color.LIGHT_GRAY, 5, 15));
		videoThumb.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent arg0) {
				String videoId = videoThumb.getToolTipText();
				if(!videoId.equals("")) {
					openInBrowser("https://youtu.be/"+videoId);
				}
			}
			public void mouseEntered(MouseEvent arg0) {}
			public void mouseExited(MouseEvent arg0) {}
			public void mousePressed(MouseEvent arg0) {}
			public void mouseReleased(MouseEvent arg0) {}
			
		});
		video_info.add(videoThumb, gbc1);
		
		videoTitle = new JTextField("Welcome to the Suite!");
		videoTitle.setEditable(false);
		videoTitle.setOpaque(false);
		videoTitle.setFont(new Font("Arial", Font.PLAIN, 14));
		videoTitle.setBackground(new Color(0,0,0,0));
		videoTitle.setBorder(BorderFactory.createEmptyBorder());
		gbc1.gridy = 1;
		video_info.add(videoTitle, gbc1);
		
		GridBagConstraints gbca2 = new GridBagConstraints();
		gbca2.insets = margin;
		gbca2.gridy = 2;
		gbca2.gridx = 0;
		videoAuthorProfile = new JLabel(imgBlankProfile);
		videoAuthorProfile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		videoAuthorProfile.setPreferredSize(new Dimension(24, 24));
		videoAuthorProfile.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent arg0) {
				String channelId = videoAuthorProfile.getToolTipText();
				if(!channelId.equals("")) {
					openInBrowser("https://youtube.com/channel/"+channelId);
				}
			}
			public void mouseEntered(MouseEvent arg0) {}
			public void mouseExited(MouseEvent arg0) {}
			public void mousePressed(MouseEvent arg0) {}
			public void mouseReleased(MouseEvent arg0) {}
			
		});
		video_info.add(videoAuthorProfile, gbca2);
		
		gbca2.gridx = 1;
		gbca2.fill = GridBagConstraints.HORIZONTAL;
		gbca2.weightx = 1;
		gbca2.ipady = 5;
		videoAuthorName = new JTextField("Hello World!");
		videoAuthorName.setEditable(false);
		videoAuthorName.setOpaque(false);
		videoAuthorName.setFont(youtube_font);
		videoAuthorName.setBackground(new Color(0,0,0,0));
		videoAuthorName.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		video_info.add(videoAuthorName, gbca2);
		
		gbca2.ipady = 0;
		gbca2.gridx = 2;
		gbca2.weightx = 0.35;
		videoStats = new JLabel("<html><div style='text-align=center'>0 views<br><a color=green>+0</a> / <a color=red>-0</a></div></html>");
		videoStats.setFont(youtube_font);
		videoStats.setHorizontalAlignment(SwingConstants.TRAILING);
		video_info.add(videoStats, gbca2);
		
		gbc1.fill = GridBagConstraints.BOTH;
		gbc1.gridy = 3;
		gbc1.gridx = 0;
		gbc1.weighty = 1;
		videoDesc = new JTextPane();
		videoDesc.setEditable(false);
		videoDesc.setContentType("text/html");
		videoDesc.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					openInBrowser(e.getURL().toString());
				}
			}
		});
		
		videoDesc.setFont(youtube_font);
		videoDesc.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		videoDesc.setText(defaultDescription);
		video_info.add(new JScrollPane(videoDesc), gbc1);
		
		split2.setResizeWeight(0.0);
		split2.setDividerLocation(split2.getMinimumDividerLocation());
		
		comment_list = new JPanel(new GridBagLayout());
		split2.setRightComponent(comment_list);
		
		viewFullComment = new JMenuItem("View Full Comment");
		Font f = viewFullComment.getFont();
		viewFullComment.setFont(new Font(f.getFontName(), Font.BOLD, f.getSize()));
		viewFullComment.addActionListener(this);
		
		viewCommentTree = new JMenuItem("View Comment Tree");
		viewCommentTree.addActionListener(this);
		
		openVideo = new JMenuItem("Open Video");
		openVideo.addActionListener(this);
		openVideo.setIcon(imgBrowser);
		
		openProfile = new JMenuItem("Open Profile");
		openProfile.addActionListener(this);
		openProfile.setIcon(imgBrowser);
		
		downloadProfiles = new JMenuItem("Download Profiles");
		downloadProfiles.addActionListener(this);
		
		backToResults = new JButton("Back to Results");
		backToResults.addActionListener(this);
		backToResults.setBackground(Color.getHSBColor(0.55F, 0.5F, 0.5F));
		
		cModel = new DefaultTableModel(new Object[]{"Type", "Author", "Comment", "Date", "Likes", "Replies", "VideoID"}, 0);
		cTable = new JTable(cModel) {
			private static final long serialVersionUID = -5774908665841108434L;
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
		cTable.setRowHeight(25);
		cTable.setShowGrid(false);
		cTable.setCellSelectionEnabled(true);
		cTable.getTableHeader().setReorderingAllowed(false);
		int[] cols2 = new int[]{75, -1, -1, 135, 75, 55, -1};
		for(int i=0; i<cols2.length; i++) {
			if(cols2[i] > 0) {
				cTable.getColumnModel().getColumn(i).setMinWidth(cols2[i]);
				cTable.getColumnModel().getColumn(i).setPreferredWidth(cols2[i]);
				cTable.getColumnModel().getColumn(i).setMaxWidth(cols2[i]);
			}
		}
		cTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			private static final long serialVersionUID = 1L;
			public Font ytfont = new Font("Arial", Font.PLAIN, 12);
			public Color likesColor = new Color(18, 142, 233);
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				if(col == 0 || col == 3 || col == 4) {
					setHorizontalAlignment(SwingConstants.CENTER);
				} else if(col == 3) {
					setHorizontalAlignment(SwingConstants.TRAILING);
				} else {
					setHorizontalAlignment(SwingConstants.LEADING);
				}
				if(value instanceof Channel) {
					Channel c = (Channel) value;
					if(c.channel_profile != null) {
						setIcon(c.channel_profile);
					} else {
						setIcon(imgBlankProfile);
					}
				} else {
					setIcon(null);
				}
				if(col == 6) {
					setForeground(Color.LIGHT_GRAY);
				} else if(col == 4) {
					if(Integer.parseInt(value.toString()) > 0) {
						setForeground(likesColor);
					} else {
						setForeground(Color.BLACK);
					}
				} else {
					setForeground(Color.BLACK);
				}
				setFont(ytfont);
				return this;
			}
		});
		cTable.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {
				selectCellWhereClicked(e);
				loadVideo(e);
				if(e.isPopupTrigger())
					doPopup(e);
				if(e.getClickCount() >= 2 && e.getButton() == MouseEvent.BUTTON1)
					viewFullComment((Comment) cTable.getValueAt(cTable.getSelectedRow(), 2));
			}
			public void mouseReleased(MouseEvent e) {
				selectCellWhereClicked(e);
				loadVideo(e);
				if(e.isPopupTrigger())
					doPopup(e);
			}
			public void selectCellWhereClicked(MouseEvent e) {
				final JTable source = (JTable) e.getSource();
				final int row = source.rowAtPoint(e.getPoint());
				final int col = source.columnAtPoint(e.getPoint());
				if(!source.isCellSelected(row, col))
					source.setRowSelectionInterval(row, row);
			}
			public void doPopup(MouseEvent e) {
				final JTable source = (JTable) e.getSource();
				final JPopupMenu popup = new JPopupMenu();
				popup.add(viewFullComment);
				popup.add(viewCommentTree);
				popup.add(openVideo);
				popup.add(openProfile);
				popup.addSeparator();
				popup.add(downloadProfiles);
				popup.show(source, e.getX(), e.getY());
			}
			public void loadVideo(MouseEvent e) {
				Comment c = (Comment) cTable.getValueAt(cTable.getSelectedRow(), 2);
				try {
					Video v = db.getVideo(c.video_id, true);
					ImageIcon thumb = v.thumbnail;
					if(thumb != null && (thumb.getIconWidth() != 320 || thumb.getIconHeight() != 180)) {
						thumb = new ImageIcon(thumb.getImage().getScaledInstance(320, 180, Image.SCALE_SMOOTH));
					}
					ImageIcon profile = v.channel.channel_profile;
					if(profile != null) {
						if(profile.getIconWidth() != 24 || profile.getIconHeight() != 24) {
							profile = new ImageIcon(profile.getImage().getScaledInstance(24, 24, 0));
						}
					} else {
						profile = imgBlankProfile;
					}
					videoThumb.setIcon(thumb);
					videoThumb.setToolTipText(v.video_id);
					videoTitle.setText(v.video_title);
					videoTitle.setCaretPosition(0);
					videoAuthorName.setText(v.channel.channel_name);
					videoAuthorName.setCaretPosition(0);
					videoAuthorProfile.setIcon(profile);
					videoAuthorProfile.setToolTipText(v.channel.channel_id);
					videoStats.setText("<html><div style='text-align=center'>"+v.total_views+" views<br><a color=green>+"+v.total_likes+"</a> / <a color=red>-"+v.total_dislikes+"</a></div></html>");
					SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
					videoDesc.setText("<b>Published on "+sdf.format(v.publish_date)+"</b><br>"+v.video_desc);
					videoDesc.setCaretPosition(0);
				} catch (SQLException | ParseException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		GridBagConstraints gbcc = new GridBagConstraints();
		gbcc.fill = GridBagConstraints.BOTH;
		gbcc.gridx = 0;
		gbcc.gridy = 0;
		gbcc.weightx = 1;
		gbcc.weighty = 1;
		JScrollPane scroll = new JScrollPane(cTable);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		comment_list.add(scroll, gbcc);
		
		JPanel search = new JPanel(new GridBagLayout());
		split1.setRightComponent(search);
		
		GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.insets = new Insets(4, 4, 4, 4);
		gbc3.fill = GridBagConstraints.HORIZONTAL;
		gbc3.gridy = 0;
		gbc3.gridx = 0;
		gbc3.weightx = 1;
		
		JPanel searchGroup = new JPanel(new GridBagLayout());
		searchGroup.setBorder(BorderFactory.createTitledBorder("Group"));
		search.add(searchGroup, gbc3);
		
		GridBagConstraints gbc4 = new GridBagConstraints();
		gbc4.insets = new Insets(4, 4, 4, 4);
		gbc4.fill = GridBagConstraints.HORIZONTAL;
		
		gbc4.gridx = 0;
		gbc4.gridy = 0;
		gbc4.ipady = 7;
		gbc4.weightx = 1;
		commentGroup = new JComboBox<Group>();
		commentGroup.setBackground(Color.getHSBColor(0.14f, 0.6f, 0.5f));
		commentGroup.addActionListener(new ActionListener(){
			public int last = -1;
			public void actionPerformed(ActionEvent arg0) {
				if(last != commentGroup.getSelectedIndex()) {
					Group g = (Group) commentGroup.getSelectedItem();
					if(g != null) {
						try {
							List<GroupItem> items = db.getGroupItems(g.group_name, false);
							itemGroup.removeAllItems();
							itemGroup.addItem(new GroupItem(-1, null, null, "All Items ("+items.size()+")", null, null, new Date(0), null, false));
							for(GroupItem gi : items) {
								itemGroup.addItem(gi);
							}
						} catch (SQLException e) {}
					}
				}
				last = commentGroup.getSelectedIndex();
			}
		});
		searchGroup.add(commentGroup, gbc4);
		
		itemGroup = new JComboBox<GroupItem>();
		gbc4.gridy = 1;
		searchGroup.add(itemGroup, gbc4);
		
		searchComments = new JPanel(new GridBagLayout());
		searchComments.setBorder(BorderFactory.createTitledBorder("Comments"));
		gbc3.gridy = 1;
		search.add(searchComments, gbc3);
		
		GridBagConstraints gbc5 = new GridBagConstraints();
		gbc5.insets = gbc3.insets;
		gbc5.fill = GridBagConstraints.HORIZONTAL;
		gbc5.gridx = 0;
		gbc5.gridy = 0;
		gbc5.ipady = 7;
		
		type = new JComboBox<String>(new String[]{"Comments and Replies", "Comments Only", "Replies Only"});
		gbc5.gridwidth = 5;
		gbc5.weightx = 1;
		searchComments.add(type, gbc5);
		
		gbc5.gridy = 1;
		gbc5.gridwidth = 1;
		gbc5.weightx = 0;
		searchComments.add(new JLabel("Name"), gbc5);
		gbc5.gridx = 1;
		gbc5.gridwidth = 4;
		gbc5.weightx = 1;
		fieldName = new JXTextField("Name contains...");
		searchComments.add(fieldName, gbc5);
		
		gbc5.gridx = 0;
		gbc5.gridy = 2;
		gbc5.weightx = 0;
		searchComments.add(new JLabel("Text"), gbc5);
		gbc5.gridx = 1;
		gbc5.gridwidth = 4;
		gbc5.weightx = 1;
		fieldText = new JXTextField("Comment contains...");
		searchComments.add(fieldText, gbc5);
		
		gbc5.gridx = 0;
		gbc5.gridy = 3;
		gbc5.gridwidth = 1;
		gbc5.weightx = 0;
		searchComments.add(new JLabel("Sort By"), gbc5);
		gbc5.gridx = 1;
		gbc5.gridwidth = 4;
		gbc5.weightx = 1;
		orderBy = new JComboBox<String>(new String[]{"Most Recent", "Least Recent", "Most Likes", "Most Replies", "Longest Comment", "Names (A to Z)", "Comments (A to Z)"});
		searchComments.add(orderBy, gbc5);
		
		findComments = new JButton("Find Comments");
		findComments.addActionListener(this);
		gbc5.gridy = 4;
		gbc5.gridx = 0;
		gbc5.ipadx = 140;
		gbc5.weightx = 1;
		gbc5.gridwidth = 5;
		searchComments.add(findComments, gbc5);
		
		GridBagConstraints gbc6 = new GridBagConstraints();
		gbc6.fill = GridBagConstraints.HORIZONTAL;
		gbc6.gridy = 5;
		gbc6.gridx = 0;
		gbc6.insets = gbc5.insets;
		gbc6.ipady = gbc5.ipady;
		gbc6.weightx = 1;
		
		random = new JButton("Random");
		random.addActionListener(this);
		searchComments.add(random, gbc6);
		
		randomCount = new JSpinner(numberModel);
		randomCount.setPreferredSize(new Dimension(25, 20));
		randomCount.setToolTipText("How many random comments?");
		gbc6.gridx = 1;
		searchComments.add(randomCount, gbc6);
		
		isFair = new JCheckBox("Fair");
		isFair.setPreferredSize(new Dimension(25, 20));
		isFair.setToolTipText("Multiple comments do not improve chances. Everyone has an equal chance.");
		gbc6.gridx = 2;
		isFair.setSelected(false);
		isFair.setEnabled(false);
		searchComments.add(isFair, gbc6);
		
		
		gbc5.gridx = 0;
		gbc5.gridy = 14;
		gbc5.fill = GridBagConstraints.BOTH;
		gbc5.weightx = 1;
		gbc5.weighty = 1;
		search.add(Box.createVerticalStrut(0), gbc5);
		
		split1.setResizeWeight(1.0);
		split1.setDividerLocation(1.0);
	}
	
	public void buildSettingsPanel() { // TODO
		settingsPanel = new JPanel();
		settingsPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = margin;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1.0;
		
		gbc.weightx = 0.1;
		gbc.gridx = 0;
		settingsPanel.add(new JLabel(), gbc);
		gbc.gridx = 2;
		settingsPanel.add(new JLabel(), gbc);
		gbc.weightx = 0;
		gbc.gridx = 1;
		
		JPanel first = new JPanel(new GridBagLayout());
		settingsPanel.add(first, gbc);
		
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.insets = margin;
		gbc1.fill = GridBagConstraints.HORIZONTAL;
		gbc1.gridx = 0;
		gbc1.gridy = 0;
		gbc1.weightx = 0;
		
		JPanel apikey = new JPanel(new GridBagLayout());
		apikey.setBorder(BorderFactory.createTitledBorder("Google API Key"));
		first.add(apikey, gbc1);
		
		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.insets = margin;
		gbc2.fill = GridBagConstraints.HORIZONTAL;
		gbc2.gridx = 0;
		gbc2.gridy = 0;
		apikey.add(saveKey = new JButton("Save"), gbc2);
		saveKey.addActionListener(this);
		gbc2.gridx = 1;
		gbc2.weightx = 1;
		apikey.add(keyField = new JXTextField("Paste your Youtube Data API key here."), gbc2);
		keyField.setText(youtubeDataKey);
		gbc2.gridx = 0;
		gbc2.gridy = 1;
		gbc2.gridwidth = 2;
		gbc2.ipady = 4;
		
		JEditorPane msg = new JEditorPane();
		apikey.add(msg, gbc2);
		msg.setEditable(false);
		msg.setContentType("text/html");
		msg.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					openInBrowser(e.getURL().toString());
				}
			}
		});
		msg.setText("The Data API key allows you to make queries for data about videos, channels, and more. <a href='https://developers.google.com/youtube/v3/getting-started'>How do I get a Youtube Data API Key?</a>");
		msg.setFont(youtube_font);
		msg.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		msg.setBorder(BorderFactory.createEmptyBorder());
		msg.setOpaque(false);
		msg.setBackground(new Color(0,0,0,0));
		
		JPanel database = new JPanel(new GridBagLayout());
		database.setBorder(BorderFactory.createTitledBorder("Database"));
		gbc1.gridx = 0;
		gbc1.gridy = 1;
		gbc1.weightx = 1;
		first.add(database, gbc1);
		
		GridBagConstraints gbc3 = new GridBagConstraints();
		gbc3.insets = margin;
		gbc3.fill = GridBagConstraints.HORIZONTAL;
		gbc3.weightx = 0;
		database.add(reset = new JButton("Reset"), gbc3);
		reset.addActionListener(this);
		reset.setToolTipText("This will delete all data from the database and downloaded thumbnails. Start from scratch.");
		
		gbc3.gridx = 1;
		database.add(clean = new JButton("Clean"), gbc3);
		clean.addActionListener(this);
		clean.setToolTipText("This will perform a VACUUM on the database.");
	}
	
	public void addCommentToTable(Comment c) {
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString);
		cModel.addRow(new Object[]{c.is_reply ? "Reply" : "Comment", c.channel, c, sdf.format(c.comment_date), c.comment_likes, c.reply_count > 0 ? c.reply_count : "", c.video_id});
	}
	
	public void setComponentsEnabled(boolean enable, Component... components) {
		for(Component c : components) {
			c.setEnabled(enable);
		}
	}
	
	public Image getImageResource(String path) {
		return Toolkit.getDefaultToolkit().getImage(getClass().getResource(path));
	}
	
	public ImageIcon getScaledImageIcon(ImageIcon img, int width, int height) {
		return new ImageIcon(img.getImage().getScaledInstance(width, height, 0));
	}
	
	public String getJson(URL url) throws IOException {
	    InputStream is = null;
	    BufferedReader br;
	    String line;
	    String text = "";
        is = url.openStream();  // throws an IOException
        br = new BufferedReader(new InputStreamReader(is));
        while ((line = br.readLine()) != null) {
            text += line;
        }
	    return text;
	}
	
	public void openInBrowser(String link) {
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
	
	public void doVideoSearch(String term, String pageToken, int sort, int type) {
		try {
			String order = SearchList.ORDER_RELEVANCE;
			if(sort == 1) order = SearchList.ORDER_DATE;
			if(sort == 2) order = SearchList.ORDER_RATING;
			if(sort == 3) order = SearchList.ORDER_TITLE;
			if(sort == 4) order = SearchList.ORDER_VIEW_COUNT;
			
			String t = SearchList.TYPE_ALL;
			if(type == 1) t = SearchList.TYPE_VIDEO;
			if(type == 2) t = SearchList.TYPE_CHANNEL;
			if(type == 3) t = SearchList.TYPE_PLAYLIST;
			slr = data.getSearch(term, SearchList.MAX_RESULTS, pageToken, order, t);
			
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString);
			for(SearchList.Item item : slr.items) {
				if(item.hasSnippet()) {
					String html = "<html><div style='text-align:right'>"+item.snippet.channelTitle+"<br><div style='color:rgb(140,140,140)'>"+sdf.format(item.snippet.publishedAt)+"</div></div></html>";
					ImageIcon thumb = imgThumbPlaceholderBlank;
					if(item.snippet.thumbnails != null && item.snippet.thumbnails.default_thumb != null) {
						thumb = item.snippet.thumbnails.default_thumb.getImageIcon();
					}
					vModel.addRow(new Object[]{false, item.id.kind.split("#")[1], thumb, item, html});
				}
			}
			if(slr.nextPageToken != null) {
				nextPage.setEnabled(true);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void actionPerformed(ActionEvent e1) {
		Object o = e1.getSource();
		
		/*
		 * Find Videos/Channels
		 */
		if(o.equals(find) || o.equals(nextPage)) {
			Thread find_videos = new Thread(new Runnable(){
				public void run() {
					setComponentsEnabled(false, find, random, nextPage, videosTerm);
					nextPage.setToolTipText(videosTerm.getText());
					String pageToken = slr != null ? (slr.nextPageToken != null ? slr.nextPageToken : "") : "";
					doVideoSearch(videosTerm.getText(), pageToken, findSort.getSelectedIndex(), findType.getSelectedIndex());
					setComponentsEnabled(true, find, random, videosTerm);
				}
			});
			find_videos.start();
		} else if(o.equals(selectall)) {
			boolean allSelected = true;
			for(int i=0; i<vModel.getRowCount(); i++) {
				if(!(boolean) vModel.getValueAt(i, 0)) {
					allSelected = false;
				}
			}
			for(int i=0; i<vModel.getRowCount(); i++) {
				vModel.setValueAt(!allSelected, i, 0);
			}
		} else if(o.equals(clearResults)) {
			vModel.setRowCount(0);
			nextPage.setEnabled(false);
			nextPage.setToolTipText("");
			slr = null;
		} else if(o.equals(addAsGroup)) {
			String group_name = "Unnamed Group";
			JPanel confirm = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.insets = margin;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			
			JRadioButton newGroup = new JRadioButton("New");
			JRadioButton existingGroup = new JRadioButton("Existing");
			JComboBox<Group> grouplist = new JComboBox<Group>();
			try {
				for(Group g : db.getGroups()) grouplist.addItem(g);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			JXTextField name = new JXTextField(group_name);
			ButtonGroup bg = new ButtonGroup();
			bg.add(newGroup);
			bg.add(existingGroup);
			existingGroup.setSelected(true);
			
			confirm.add(newGroup, gbc);
			gbc.gridy++;
			confirm.add(new JLabel("Give the new group a name:"), gbc);
			
			gbc.gridy++;
			confirm.add(name, gbc);
			
			gbc.gridy++;
			confirm.add(existingGroup, gbc);
			
			gbc.gridy++;
			confirm.add(grouplist, gbc);
			
			List<GroupItem> gi = new ArrayList<GroupItem>();
			for(int i=0; i<vModel.getRowCount(); i++) {
				if((boolean) vModel.getValueAt(i, 0)) {
					SearchList.Item item = (SearchList.Item) vModel.getValueAt(i, 3);
					String kind = item.id.kind.split("#")[1];
					int type_id = 0;
					if(kind.equals("video")) type_id = 0;
					if(kind.equals("channel")) type_id = 1;
					if(kind.equals("playlist")) type_id = 2;
					String youtube_id = "";
					if(item.id.videoId != null) youtube_id = item.id.videoId;
					if(item.id.channelId != null) youtube_id = item.id.channelId;
					if(item.id.playlistId != null) youtube_id = item.id.playlistId;
					gi.add(new GroupItem(type_id, kind, youtube_id, item.snippet.title, item.snippet.channelTitle, item.snippet.publishedAt, new Date(0), item.snippet.thumbnails.default_thumb.url.toString(), true));
					try {
						refreshGroupTable();
					} catch (SQLException | ParseException e) {
						e.printStackTrace();
					}
				}
			}
			
			if(gi.size() > 0) {
				int code = 0;
				do {
					code = JOptionPane.showConfirmDialog(this, confirm, "Add Selection to Group ("+gi.size()+" items)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				} while ((name.getText().equals("") && newGroup.isSelected() && !existingGroup.isSelected()) && code == JOptionPane.OK_OPTION);
				if(code == JOptionPane.OK_OPTION) {
					try {
						String group = grouplist.getSelectedItem().toString();
						if(newGroup.isSelected()) {
							db.createGroup(name.getText());
							group = name.getText();
						}
						db.insertGroupItems(group, gi);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				gi.clear();
				try {
					refreshGroupTable();
				} catch (SQLException | ParseException e) {
					e.printStackTrace();
				}
			} else {
				JOptionPane.showMessageDialog(this, "Please select one or more items.");
			}
		} else if(o.equals(videoOIB)) {
			String type = vTable.getValueAt(vTable.getSelectedRow(), 1).toString();
			SearchList.Item item = (SearchList.Item) vTable.getValueAt(vTable.getSelectedRow(), 3);
			if(type.equals("video")) {
				openInBrowser("https://youtu.be/"+item.id.videoId);
			} else if(type.equals("playlist")) {
				openInBrowser("https://www.youtube.com/playlist?list="+item.id.playlistId);
			} else if(type.equals("channel")) {
				openInBrowser("https://www.youtube.com/channel/"+item.id.channelId);
			}
		}
		
		/**
		 * Manage Groups
		 */
		if(o.equals(addGroup)) {
			Thread add = new Thread(new Runnable(){
				public void run() {
					addGroup.setEnabled(false);
					JXTextField name = new JXTextField("");
					int code = JOptionPane.showConfirmDialog(window, name, "Choose a unique group name.", JOptionPane.OK_CANCEL_OPTION);
					if(code == JOptionPane.OK_OPTION) {
						try {
							db.createGroup(name.getText());
						} catch (SQLException e) {
							JOptionPane.showMessageDialog(window, name.getText()+" is already a group.", "Choose a different name.", JOptionPane.ERROR_MESSAGE);
						}
						try {
							refreshGroupTable();
						} catch (SQLException | ParseException e) {
							e.printStackTrace();
						}
					}
					addGroup.setEnabled(true);
				}
			});
			add.start();
		} else if(o.equals(deleteGroup)) { // TODO
			String group_name = gTable.getValueAt(gTable.getSelectedRow(), 0).toString();
			int code = JOptionPane.showConfirmDialog(this, new JLabel("<html>Are you sure you want to delete <b>"+group_name+"</b> and all of its videos?</html>"), "Delete Group", JOptionPane.OK_CANCEL_OPTION);
			try {
				if(code == JOptionPane.OK_OPTION) {
					gTable.setEnabled(false);
					db.deleteGroup(group_name);
					refreshGroupTable();
					gTable.setEnabled(true);
				}
			} catch (SQLException | ParseException e) {
				e.printStackTrace();
			}
		} else if(o.equals(editName)) {
			String group_name = gTable.getValueAt(gTable.getSelectedRow(), 0).toString();
			JTextField newName = new JTextField(group_name);
			int code = JOptionPane.showConfirmDialog(this, newName, "Edit Group Name", JOptionPane.OK_CANCEL_OPTION);
			if(code == JOptionPane.OK_OPTION) {
				if(!group_name.equals(newName.getText())) {
					try {
						gTable.setEnabled(false);
						db.editGroupName(group_name, newName.getText());
						Group g = db.getGroup(newName.getText());
						gTable.setValueAt(g, gTable.getSelectedRow(), 0);
						gTable.setEnabled(true);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		} else if(o.equals(refreshGroup)) {
			refreshGroup.setEnabled(false);
			Thread thread = new Thread(new Runnable(){
				public void run() {
					int row = gTable.getSelectedRow();
					Group g = (Group) gTable.getValueAt(row, 0);
					try {
						GroupChecker gr = new GroupChecker(row, g);
						gr.start();
					} catch (SQLException e) {
						e.printStackTrace();
					}
					System.gc();
				}
			});
			thread.start();
		} else if(o.equals(analyze)) {
			Thread thread = new Thread(new Runnable(){
				public void run() {
					analyze.setEnabled(false);
					gitems.setEnabled(false);
					aType.setEnabled(false);
					progress.setIndeterminate(true);
					List<String> output;
					try {
						output = db.getAnalytics(gTable.getValueAt(gTable.getSelectedRow(), 0).toString(), (GroupItem) gitems.getSelectedItem(), aType.getSelectedIndex());
						results.setText(output.stream().collect(Collectors.joining()));
					} catch (SQLException e) {
						e.printStackTrace();
						results.setText("<div style='color:red'>"+e.getMessage()+"</div>");
					}
					analyze.setEnabled(true);
					gitems.setEnabled(true);
					aType.setEnabled(true);
					progress.setIndeterminate(false);
				}
			});
			thread.start();
		} else if(o.equals(deleteItem)) { // TODO
			Thread thread = new Thread(new Runnable(){
				public void run() {
					deleteItem.setEnabled(false);
					List<GroupItem> list = new ArrayList<GroupItem>();
					for(int i : groupItemTable.getSelectedRows()) {
						list.add((GroupItem) groupItemTable.getValueAt(i, 2));
					}
					deleteItem.setEnabled(true);
				}
			});
			thread.start();
		}
		
		/**
		 * Comments
		 */
		if(o.equals(findComments) || o.equals(random)) {
			Thread find = new Thread(new Runnable(){
				public void run() {
					foundComments = new ArrayList<Comment>();
					setComponentsEnabled(false, findComments, random);
					try {
						GroupItem gi = itemGroup.getSelectedIndex() != 0 ? (GroupItem) itemGroup.getSelectedItem() : null;
						CommentSearch cs = db.getComments(commentGroup.getSelectedItem().toString(), orderBy.getSelectedIndex(), fieldName.getText(), fieldText.getText(), 5000, gi, type.getSelectedIndex(), o.equals(random), Integer.parseInt(numberModel.getValue().toString()), isFair.isSelected());
						foundComments = cs.results;
						cModel.setRowCount(0);
						lastRect = null;
						for(Comment c : foundComments) {
							addCommentToTable(c);
						}
						searchComments.setBorder(BorderFactory.createTitledBorder("Comments (Showing "+(foundComments.size() == 5000 ? "":"")+foundComments.size()+" / "+cs.total_results+")"));
						System.out.println("Found "+foundComments.size()+" comments");
					} catch (SQLException e) {
						e.printStackTrace();
					}
					setComponentsEnabled(true, findComments, random);
				}
			});
			find.start();
		} else if(o.equals(viewFullComment)) {
			Comment c = (Comment) cTable.getValueAt(cTable.getSelectedRow(), 2);
			viewFullComment(c); 
		} else if(o.equals(openVideo)) {
			Comment c = (Comment) cTable.getValueAt(cTable.getSelectedRow(), 2);
			openInBrowser("https://youtu.be/"+c.video_id);
		} else if(o.equals(viewCommentTree)) {
			Thread tree = new Thread(new Runnable(){
				public void run() {
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.fill = GridBagConstraints.BOTH;
					gbc.weightx = 1;
					gbc.gridy = 1;
					gbc.weighty = 0;
					Rectangle vr = cTable.getVisibleRect(); // Get last visible row.
		            int firstRow = cTable.rowAtPoint(vr.getLocation());
		            vr.translate(0, vr.height);
		            int visibleRows = cTable.rowAtPoint(vr.getLocation()) - firstRow;
		            int lastRow = (visibleRows > 0) ? visibleRows+firstRow : cTable.getRowCount();
		            lastRect = cTable.getCellRect(lastRow, 0, true);
		            selectedRow = cTable.getSelectedRow();
					comment_list.add(backToResults, gbc);
					comment_list.validate();
					Comment comment = (Comment) cTable.getValueAt(cTable.getSelectedRow(), 2);
					try {
						commentTree = db.getCommentTree(comment.is_reply ? comment.parent_id : comment.comment_id);
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {
								cModel.setRowCount(0);
								for(Comment c : commentTree) {
									addCommentToTable(c);
								}
							}
						});
						backToResults.setEnabled(true);
					} catch (SQLException e) {
						e.printStackTrace();
					}
					
				}
			});
			tree.start();
		} else if(o.equals(backToResults)) {
			Thread tree = new Thread(new Runnable(){
				public void run() {
					comment_list.remove(backToResults);
					comment_list.validate();
					cModel.setRowCount(0);
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							cModel.setRowCount(0);
							for(Comment c : foundComments) {
								addCommentToTable(c);
							}
							if(lastRect != null) {
								if(selectedRow >= 0)
									cTable.setRowSelectionInterval(selectedRow, selectedRow);
								cTable.scrollRectToVisible(lastRect);
								lastRect = null;
							}
						}
					});
					backToResults.setEnabled(false);
					
				}
			});
			tree.start();
		} else if(o.equals(openProfile)) {
			Comment c = (Comment) cTable.getValueAt(cTable.getSelectedRow(), 2);
			openInBrowser("https://youtube.com/channel/"+c.channel.channel_id);
		} else if(o.equals(downloadProfiles)) {
			Thread thread = new Thread(new Runnable(){
				public void run() {
					downloadProfiles.setEnabled(false);
					List<Channel> channels = new ArrayList<Channel>();
					for(int i : cTable.getSelectedRows()) {
						Channel c = ((Comment) cTable.getValueAt(i, 2)).channel;
						if(c != null) {
							if(!channels.contains(c))
								channels.add(c);
						}
					}
					for(Channel c : channels) {
						System.out.println("Attempting to refresh "+c.channel_name);
						c.download_profile = true;
						c.loadProfile();
					}
					downloadProfiles.setEnabled(true);
				}
			});
			thread.start();
		}
		
		
		/**
		 * Settings
		 */
		if(o.equals(reset)) {
			Thread r = new Thread(new Runnable(){
				public void run() {
					reset.setEnabled(false);
					display.setEnabledAt(0, false);
					display.setEnabledAt(1, false);
					display.setEnabledAt(2, false);
					int code = JOptionPane.showConfirmDialog(window, "You will erase all data. Are you sure?", "Reset Database", JOptionPane.OK_CANCEL_OPTION);
					if(code == JOptionPane.OK_OPTION) {
						try {
							db.dropAllTables();
							try {
								db.create();
							} catch (ClassNotFoundException e) {}
							db.clean();
							setupFromDatabase();
							File thumbs = new File("Thumbs/");
							if(thumbs.exists()) {
								for(File f : thumbs.listFiles()) {
									f.delete();
								}
							}
						} catch (SQLException e) {
							e.printStackTrace();
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					reset.setEnabled(true);
					display.setEnabledAt(0, true);
					display.setEnabledAt(1, true);
					display.setEnabledAt(2, true);
				}
			});
			r.start();
		} else if(o.equals(clean)) {
			clean.setEnabled(false);
			try {
				db.clean();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			clean.setEnabled(true);
		} else if(o.equals(saveKey)) {
			saveKey.setEnabled(false);
			keyField.setEditable(false);
			try {
				setKey(keyField.getText());
			} catch (IOException e) {
				e.printStackTrace();
			}
			saveKey.setEnabled(true);
			keyField.setEditable(true);
		}
	}
	
	public void viewFullComment(Comment c) {
		JPanel view = new JPanel(new GridBagLayout());
		view.setPreferredSize(new Dimension(500, 200));
		
		GridBagConstraints gbc = new GridBagConstraints();
		Insets margin = new Insets(4, 2, 4, 2);
		gbc.insets = margin;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		
		JTextField username = new JTextField(c.channel.channel_name);
		username.setEditable(false);
		username.setOpaque(false);
		username.setFont(youtube_font);
		username.setBackground(new Color(0,0,0,0));
		username.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		username.setCaretPosition(0);
		view.add(username, gbc);
		
		gbc.weighty = 1;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.BOTH;
		JEditorPane commentText = new JEditorPane();
		commentText.setEditable(false);
		commentText.setContentType("text/html");
		commentText.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					openInBrowser(e.getURL().toString());
				}
			}
		});
		commentText.setText(c.comment_text);
		commentText.setCaretPosition(0);
		commentText.setFont(youtube_font);
		commentText.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		view.add(new JScrollPane(commentText), gbc);
		
		JOptionPane.showMessageDialog(window, view, "Full Comment View", JOptionPane.INFORMATION_MESSAGE, c.channel.channel_profile != null ? c.channel.channel_profile : imgBlankProfile);
	}
	
	public class GroupChecker extends Thread {
		
		public int tableRow;
		public Group group;
		public ElapsedTime timer = new ElapsedTime();
		public int commentProgress = 0;
		
		public Set<String> existingVideoIds = new HashSet<String>();
		public Set<String> existingCommentIds = new HashSet<String>();
		public Set<String> existingChannelIds = new HashSet<String>();
		public List<VideoGroup> existingVideoGroups = new ArrayList<VideoGroup>();
		public List<GroupItem> existingGroupItems = new ArrayList<GroupItem>();
		
		public List<CommentThread> commentThreadIds = new ArrayList<CommentThread>();
		
		public class CommentThread {
			public String comment_id;
			public String video_id;
			public CommentThread(String comment_id, String video_id) {
				this.comment_id = comment_id;
				this.video_id = video_id; 
			}
		}
		
		public List<Video> insertVideos = new ArrayList<Video>();
		public List<Video> updateVideos = new ArrayList<Video>();
		public List<Channel> insertChannels = new ArrayList<Channel>();
		public List<Channel> updateChannels = new ArrayList<Channel>();
		public List<VideoGroup> insertVideoGroups = new ArrayList<VideoGroup>();
		
		public List<String> gitemVideos = new ArrayList<String>();
		public List<GroupItem> gitemChannels = new ArrayList<GroupItem>();
		public List<GroupItem> gitemPlaylists = new ArrayList<GroupItem>();
		
		public Map<String, Channel> channels = new HashMap<String, Channel>();
		
		public GroupChecker(int tableRow, Group group) throws SQLException {
			this.tableRow = tableRow;
			this.group = group;
			
			existingVideoIds.addAll(db.getAllVideoIds());
			existingCommentIds.addAll(db.getCommentIDs(group.group_name));
			existingChannelIds.addAll(db.getAllChannelIDs());
			existingVideoGroups.addAll(db.getVideoGroups());
			existingGroupItems.addAll(db.getGroupItems(group.group_name, false));
			for(GroupItem gi : existingGroupItems) {
				if(gi.type_id == 0) {
					VideoGroup vg = new VideoGroup(gi.gitem_id, gi.youtube_id);
					if(!existingVideoGroups.contains(vg)) {
						insertVideoGroups.add(vg);
					}
					gitemVideos.add(gi.youtube_id);
				} else if(gi.type_id == 1) {
					gitemChannels.add(gi);
				} else if(gi.type_id == 2) {
					gitemPlaylists.add(gi);
				}
			}
			db.updateGroupItemsChecked(existingGroupItems, new Date());
		}
		
		public boolean videoListContainsId(List<Video> list, String id) {
			for(Video video : list)
				if(video.video_id.equals(id)) return true;
			return false;
		}
		
		public boolean commentListContainsId(List<Comment> list, String id) {
			for(Comment comment : list)
				if(comment.comment_id.equals(id)) return true;
			return false;
		}
		
		public void setStatus(String status) {
			if(tableRow <= gTable.getRowCount()) {
				gTable.setValueAt("<html><div style='text-align:center'>"+status+"</div></html>", tableRow, 1);
			}
		}
		
		public void clearAll(Collection<?>... lists) {
			for(Collection<?> list : lists) {
				list.clear();
			}
		}
		
		public void run() {
			try {
				timer.set();
				db.con.setAutoCommit(false);
				setStatus("Part 1 of 3. New videos.");
				try {
					parseVideoItems(gitemVideos, -1);
					parseChannelItems(gitemChannels);
					parsePlaylistItems(gitemPlaylists);
				} catch (JsonSyntaxException | IOException e) {
					e.printStackTrace();
				}
				setStatus("Part 1 of 3. Committing "+insertVideos.size()+" new videos.");
				db.insertVideos(insertVideos);
				db.updateVideos(updateVideos);
				db.insertVideoGroups(insertVideoGroups);
				db.con.commit();
				
				clearAll(existingVideoIds, existingVideoGroups);
				clearAll(insertVideos, updateVideos, insertVideoGroups);
				clearAll(gitemVideos, gitemChannels, gitemPlaylists);
				
				setStatus("Part 2 of 3. New comments.");
				commentProgress = 0;
				List<String> videosInGroup = db.getVideoIds(group.group_name);
				setStatus("Part 2 of 3. New comments.<br>Videos 0/"+videosInGroup.size()+" (...)");
				videosInGroup.parallelStream().forEach(videoId -> {
					ElapsedTime clock = new ElapsedTime();
					clock.set();
					try {
						List<Comment> comments = getComments(videoId);
						if(comments.size() > 0)
							db.insertComments(comments);
					} catch (JsonSyntaxException | SQLException e) {
						e.printStackTrace();
					} catch (Throwable e) {
						System.out.println("Something fucked up. "+videoId);
						e.printStackTrace();
					}
					commentProgress++;
					setStatus("Part 2 of 3. New comments.<br>Videos "+commentProgress+"/"+videosInGroup.size()+" ("+clock.getTimeString()+")");
				});
				setStatus("Part 2 of 3. Committing.");
				db.con.commit();
				
				setStatus("Part 3 of 3. New replies.");
				commentProgress = 0;
				commentThreadIds.parallelStream().forEach(thread -> {
					ElapsedTime clock = new ElapsedTime();
					clock.set();
					try {
						List<Comment> replies = getReplies(thread);
						if(replies.size() > 0)
							db.insertComments(replies);
					} catch (JsonSyntaxException e) {
						e.printStackTrace();
					} catch (SQLException e) {
						e.printStackTrace();
					} catch (Throwable e) {
						System.out.println("Something fucked up. "+thread.comment_id);
						e.printStackTrace();
					}
					commentProgress++;
					setStatus("Part 3 of 3. Grabbing replies.<br>Threads "+commentProgress+"/"+commentThreadIds.size()+" ("+clock.getTimeString()+")");
				});
				
				db.insertChannels(insertChannels);
				db.updateChannels(updateChannels);
				setStatus("Part 3 of 3. Commit.");
				db.con.commit();
				setStatus("Finished. "+timer.getTimeString());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			clearAll(insertChannels, updateChannels, existingCommentIds, existingChannelIds);
			channels.clear();
		}
		
		public void parseChannelItems(List<GroupItem> channels) throws JsonSyntaxException, IOException {
			for(GroupItem gi : channels) {
				ChannelsList cl = data.getChannelsByChannelId(ChannelsList.PART_CONTENT_DETAILS, gi.youtube_id, ChannelsList.MAX_RESULTS, "");
				String uploadPlaylistId = cl.items[0].contentDetails.relatedPlaylists.uploads;
				handlePlaylist(uploadPlaylistId, gi.gitem_id);
			}
		}
		
		public void parsePlaylistItems(List<GroupItem> playlists) throws JsonSyntaxException, IOException {
			for(GroupItem gi : playlists) {
				handlePlaylist(gi.youtube_id, gi.gitem_id);
			}
		}
		
		public void handlePlaylist(final String playlistId, int gitem_id) throws JsonSyntaxException, IOException {
			PlaylistItemsList pil = null;
			String pageToken = "";
			List<String> videos = new ArrayList<String>();
			do {
				pil = data.getPlaylistItems(PlaylistItemsList.PART_SNIPPET, playlistId, PlaylistItemsList.MAX_RESULTS, pageToken);
				pageToken = pil.nextPageToken;
				for(PlaylistItemsList.Item item : pil.items) {
					if(item.hasSnippet()) {
						videos.add(item.snippet.resourceId.videoId);
					}
				}
			} while (pil.nextPageToken != null);
			parseVideoItems(videos, gitem_id);
		}
		
		public void parseVideoItems(List<String> videos, int gitem_id) throws JsonSyntaxException, IOException {
			for(int i=0; i<videos.size(); i += 50) {
				List<String> sublist = videos.subList(i, i+50 < videos.size() ? i+50 : videos.size());
				if(gitem_id != -1) {
					for(String v : sublist) {
						VideoGroup vg = new VideoGroup(gitem_id, v);
						if(!existingVideoGroups.contains(vg)) {
							if(!insertVideoGroups.contains(vg)) {
								insertVideoGroups.add(vg);
							}
						}
					}
				}
				String ids = sublist.stream().filter(id -> !videoListContainsId(insertVideos, id) && !videoListContainsId(updateVideos, id)).collect(Collectors.joining(","));
				System.out.println(ids);
				handleVideos(ids);
			}
		}
		
		public void handleVideos(final String ids) throws JsonSyntaxException, IOException {
			VideosList snip = data.getVideosById(VideosList.PART_SNIPPET, ids, VideosList.MAX_RESULTS, "");
			VideosList stats = data.getVideosById(VideosList.PART_STATISTICS, ids, VideosList.MAX_RESULTS, "");
			
			Channel channel;
			for(int i=0; i<snip.items.length; i++) {
				VideosList.Item itemSnip = snip.items[i];
				VideosList.Item itemStat = stats.items[i];
				if(channels.containsKey(itemSnip.snippet.channelId)) {
					channel = channels.get(itemSnip.snippet.channelId);
				} else {
					ChannelsList cl = data.getChannelsByChannelId(ChannelsList.PART_SNIPPET, itemSnip.snippet.channelId, 1, "");
					ChannelsList.Item item = cl.items[0];
					channel = new Channel(itemSnip.snippet.channelId, StringEscapeUtils.unescapeHtml4(item.snippet.title), item.snippet.thumbnails.default_thumb.url.toString(), true);
					channels.put(itemSnip.snippet.channelId, channel);
				}
				if(!existingChannelIds.contains(itemSnip.snippet.channelId)) {
					if(!insertChannels.contains(channel))
						insertChannels.add(channel);
				} else {
					if(!updateChannels.contains(channel))
						updateChannels.add(channel);
				}
				if(channel == null) System.out.println("NULL CHANNEL");
				Video video = new Video(itemSnip.id, channel, new Date(), itemSnip.snippet.publishedAt, itemSnip.snippet.title, itemSnip.snippet.description, itemStat.statistics.commentCount, itemStat.statistics.likeCount, itemStat.statistics.dislikeCount, itemStat.statistics.viewCount, itemSnip.snippet.thumbnails.medium.url.toString(), 200, true);
				if(!existingVideoIds.contains(itemSnip.id) && !videoListContainsId(insertVideos, itemSnip.id) && !videoListContainsId(updateVideos, itemSnip.id)) {
					insertVideos.add(video);
				} else {
					if(!videoListContainsId(updateVideos, itemSnip.id))
						updateVideos.add(video);
				}
			}
			setStatus("Part 1 of 3. New videos.<br>"+insertVideos.size()+" new of "+(insertVideos.size()+updateVideos.size()));
		}
		
		public List<Comment> getComments(final String videoId) throws JsonSyntaxException {
			List<Comment> comments = new ArrayList<Comment>();
			CommentThreadsList snippet = null;
			String snipToken = "";
			int fails = 0;
			do {
				try {
					snippet = data.getCommentThreadsByVideoId(CommentThreadsList.PART_SNIPPET, videoId, CommentThreadsList.MAX_RESULTS, snipToken);
					snipToken = snippet.nextPageToken;
					
					for(CommentThreadsList.Item item : snippet.items) {
						if(item.hasSnippet()) {
							if(item.snippet.totalReplyCount > 0) {
								commentThreadIds.add(new CommentThread(item.id, videoId));
							}
							if(!existingCommentIds.contains(item.id)) {
								CommentsList.Item tlc = item.snippet.topLevelComment;
								Comment c = createComment(tlc, false, item.snippet.totalReplyCount);
								comments.add(c);
							}
						}
					}
				} catch (IOException e) {
					fails++;
					if(e.getMessage().contains("HTTP response code")) {
						Pattern p = Pattern.compile("([0-9]{3}) for URL");
						Matcher m = p.matcher(e.getMessage());
						if(m.find()) {
							int code = Integer.parseInt(m.group(1));
							if(code == 400) { // Retry / Bad request.
								System.err.println("Bad Request (400): Retry #"+fails+"  http://youtu.be/"+videoId);
							} else if(code == 403) { // Comments Disabled or Forbidden
								System.err.println("Comments Disabled (403): http://youtu.be/"+videoId);
								try {
									db.updateVideoHttpCode(videoId, 403);
								} catch (SQLException e1) {}
								break;
							} else if(code == 404) { // Not found.
								System.err.println("Not found (404): http://youtu.be/"+videoId);
								try {
									db.updateVideoHttpCode(videoId, 403);
								} catch (SQLException e1) {}
								break;
							} else { // Unknown error.
								System.err.println("Unknown Error ("+code+"): http://youtu.be/"+videoId);
								try {
									db.updateVideoHttpCode(videoId, code);
								} catch (SQLException e1) {
									e1.printStackTrace();
								}
								break;
							}
						}
					}
				}
			} while ((snippet == null || snippet.nextPageToken != null) && fails < 5);
			return comments;
		}
		
		public List<Comment> getReplies(final CommentThread thread) throws JsonSyntaxException {
			List<Comment> replies = new ArrayList<Comment>();
			CommentsList cl = null;
			String pageToken = "";
			int fails = 0;
			do {
				try {
					cl = data.getCommentsByParentId(thread.comment_id, CommentsList.MAX_RESULTS, pageToken);
					pageToken = cl.nextPageToken;
					for(CommentsList.Item reply : cl.items) {
						if(!existingCommentIds.contains(reply.id)) {
							Comment c = createComment(reply, true, -1);
							c.video_id = thread.video_id;
							replies.add(c);
						}
					}
				} catch (IOException e) {
					fails++;
				}
			} while (cl.nextPageToken != null && fails < 5);
			return replies;
		}
		
		public Comment createComment(CommentsList.Item item, boolean isReply, int replyCount) {
			if(item.hasSnippet()) {
				if(item.snippet.authorChannelId != null && item.snippet.authorChannelId.value != null) {
					Channel channel;
					String channelId = item.snippet.authorChannelId.value;
					if(channelId == null) System.out.println("NULL CHANNELID");
					if(channels.containsKey(channelId)) {
						channel = channels.get(channelId);
					} else {
						channel = new Channel(channelId, StringEscapeUtils.unescapeHtml4(item.snippet.authorDisplayName), item.snippet.authorProfileImageUrl, false);
						channels.put(channelId, channel);
					}
					if(!existingChannelIds.contains(channelId)) {
						if(!insertChannels.contains(channel))
							insertChannels.add(channel);
					} else {
						if(!updateChannels.contains(channel))
							updateChannels.add(channel);
					}
					Comment comment = null;
					if(isReply) {
						comment = new Comment(item.id, channel, item.snippet.videoId, item.snippet.publishedAt, StringEscapeUtils.unescapeHtml4(item.snippet.textDisplay), item.snippet.likeCount, replyCount, isReply, item.snippet.parentId);
					} else {
						comment = new Comment(item.id, channel, item.snippet.videoId, item.snippet.publishedAt, StringEscapeUtils.unescapeHtml4(item.snippet.textDisplay), item.snippet.likeCount, replyCount, isReply, null);
					}
					return comment;
				}
			}
			return null;
		}
	}
}
