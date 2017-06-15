package mattw.youtube.commensuitefx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonSyntaxException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import mattw.youtube.commensuitefx.DatabaseManager.CommentQuery;
import mattw.youtube.datav3.YoutubeData;
import mattw.youtube.datav3.list.SearchList;

public class CommentSuiteFX extends Application implements EventHandler<ActionEvent> {

	/**
	 * TODO
	 * More Search Options
	 * 		Random, Random Amount, Random Fairness
	 * 		Choose specific video by text search, display top 5 matches in combobox.
	 */

	private static CommentSuiteFX instance;
	private final YCSConfig config = new YCSConfig();
	public YoutubeData data;
	private Stage stage;
	public static final Image placeholder = new Image(CommentResult.class.getResourceAsStream("/mattw/youtube/commentsuite/images/placeholder4.png"));

	public DatabaseManager database;
	private CommentQuery query;
	private Button nextPageC;
	private Button prevPageC;
	private Button firstPage;
	private Button lastPage;
	private TextField pageNum;
	private int page = 1;

	private StackPane layout;
	private StackPane setup;
	private StackPane addGroup;
	private GridPane main;
	private GridPane menu;
	private GridPane videos;
	private GridPane groups;
	private HBox comments;

	private ImageView header;
	private ToggleButton videoToggle;
	private ToggleButton groupToggle;
	private ToggleButton commentToggle;
	private Label welcome;
	private Hyperlink gotoSetup;

	private Button saveAndSetup;
	private Button exitSetup;
	private Button signin;
	public Label status;
	private VBox accountList = new VBox(8);

	private String pageToken = "";
	private Button search;
	private Button selectAll;
	private Button clearResults;
	private Button addToGroup;
	private Button nextPage;
	private TextField searchField;
	private TextField locField;
	private ComboBox<String> searchMethod;
	private ComboBox<String> searchOrder;
	private ComboBox<String> searchType;
	private ComboBox<String> locDistance;
	private ScrollPane scroll;
	private VBox searchResults;
	private Label resultStatus;

	private GroupManager manager;
	private Button createGroup;
	private Button deleteGroup;
	private Button renameGroup;
	private Button refreshGroup;
	private Button reloadGroup;
	private Button resetDB;
	private Button cleanDB;
	private ChoiceBox<Group> choice;

	private VBox commentResults;
	private VBox searchBox;
	private ScrollPane cscroll;
	private double vValue = 0.0;
	private ImageView thumbnail;
	private ImageView authorThumb;
	private TextField title;
	private TextField author;
	private Label views;
	private Label likes;
	private Label dislikes;
	private Label resultCount;
	private TextArea description;
	private ChoiceBox<Group> cgroup;
	private ChoiceBox<GitemType> citem;
	private ToggleButton videoContext;
	private TextField userLike;
	private TextField textLike;
	private Button find;
	private Button backToResults;
	private Button clearComments;
	private ComboBox<String> type;
	private ComboBox<String> orderby;
	private List<CommentResult> results;
	private List<CommentResult> tree;

	public static void main(String[] args) {
		launch(args);
	}

	public static CommentSuiteFX getApp() { return instance; }

	public YCSConfig getConfig() { return config; }

	public StackPane getMainStackPane() { return instance.layout; }

	public static void addOverlay(StackPane stack) {
		if(!instance.layout.getChildren().contains(stack))
			instance.layout.getChildren().add(stack);
	}

	private void saveConfig() {
		Platform.runLater(() -> {
			if(layout.getChildren().contains(setup)) {
				try {
					config.save();
					loadConfig();
				} catch (IOException ignored) {}
				layout.getChildren().remove(setup);
			}
		});
	}

	private void loadConfig() throws IOException {
		config.load();
		data = new YoutubeData(config.getYoutubeKey());
		Platform.runLater(() -> {
			System.out.println(getConfig().accounts.size());
			welcome.setText(config.getWelcomeStatement());
			accountList.getChildren().clear();
			for(Account acc : getConfig().accounts) {
				accountList.getChildren().add(new AccountPane(acc));
			}
		});
		if(!config.isSetup()) {
			Platform.runLater(() -> layout.getChildren().add(setup));
		}
	}

	class AccountPane extends HBox {
		public final Button signOut = new Button("Sign out");
		public Label name = new Label("...");
		public AccountPane(Account acc) {
			super(10);
			setId("account");
			setPadding(new Insets(2,2,2,2));
			setFillHeight(true);
			acc.getUsername(); // sets stringproperty or get nullpointerexception
			name.textProperty().bind(acc.nameProperty);
			name.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 15));
			signOut.setStyle("-fx-base: firebrick");
			signOut.setOnAction(ae -> {
				acc.signOut();
				Platform.runLater(() -> accountList.getChildren().remove(this));
			});
			getChildren().addAll(signOut, name);
		}
	}

	private void setNodesDisabled(boolean disable, Node... nodes) {
		for(Node n : nodes) n.setDisable(disable);
	}

	public void handle(ActionEvent arg0) {
		Object o = arg0.getSource();
		if(o.equals(saveAndSetup) || o.equals(exitSetup) || o.equals(signin)) {
			if(o.equals(saveAndSetup)) {
				Task<Void> task = new Task<Void>(){
					protected Void call() throws Exception {
						setNodesDisabled(true, saveAndSetup);
						saveConfig();
						return null;
					}
				};
				Thread thread = new Thread(task);
				thread.setDaemon(true);
				thread.start();
			} else if(o.equals(exitSetup)) {
				layout.getChildren().remove(setup);
			} else if(o.equals(signin)) {

			}
		} else if(o.equals(videoToggle) || o.equals(groupToggle) || o.equals(commentToggle)) {
			if(videoToggle.isSelected()) {
				if(main.getChildren().contains(groups)) main.getChildren().remove(groups);
				if(main.getChildren().contains(comments)) main.getChildren().remove(comments);
				if(!main.getChildren().contains(videos)) {
					main.add(videos, 0, 1);
					GridPane.setVgrow(videos, Priority.ALWAYS);
				}
				header.setImage(new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuite/images/youtube.png")));
			} else if(groupToggle.isSelected()) {
				if(main.getChildren().contains(videos)) main.getChildren().remove(videos);
				if(main.getChildren().contains(comments)) main.getChildren().remove(comments);
				if(!main.getChildren().contains(groups)) {
					main.add(groups, 0, 1);
					GridPane.setVgrow(groups, Priority.ALWAYS);
				}
				header.setImage(new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuite/images/manage.png")));
			} else if(commentToggle.isSelected()) {
				if(main.getChildren().contains(videos)) main.getChildren().remove(videos);
				if(main.getChildren().contains(groups)) main.getChildren().remove(groups);
				if(!main.getChildren().contains(comments)) {
					main.add(comments, 0, 1);
					GridPane.setVgrow(comments, Priority.ALWAYS);
				}
				header.setImage(new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuite/images/search.png")));
			} else {
				System.out.println("Menu Toggle: Something broke.");
			}
		} else if(o.equals(search) || o.equals(nextPage)) {
			Task<Void> task = new Task<Void>() {
				protected Void call() {
					setNodesDisabled(true, search, nextPage, searchField, searchMethod, locField, locDistance, searchOrder, searchType);
					try {
						String escaped_search = URLEncoder.encode(searchField.getText(), "UTF-8");
						String order = searchOrder.getSelectionModel().getSelectedItem().toLowerCase();
						String type = "";
						if(searchType.getSelectionModel().getSelectedIndex() != 0) type = searchType.getSelectionModel().getSelectedItem().toLowerCase();
						int method = searchMethod.getSelectionModel().getSelectedIndex();
						String location = locField.getText();
						String distance = locDistance.getSelectionModel().getSelectedItem();
						String token = "";
						if(o.equals(search)) {
							Platform.runLater(() -> searchResults.getChildren().clear());
						} else {
							token = pageToken;
						}
						SearchList sl;
						if(method == 0) {
							sl = data.getSearch(escaped_search, 25, token, order, type);
						} else {
							sl = data.getSearchVideosAtLocation(escaped_search, 25, token, order, location, distance);
						}
						for(SearchList.Item item : sl.items) {
							final SearchResult result = new SearchResult(item);
							Platform.runLater(() -> {
								searchResults.getChildren().add(result);
								setNodesDisabled(false, selectAll, clearResults, addToGroup);
								resultStatus.setText("Showing "+searchResults.getChildren().size()+" out of "+sl.pageInfo.totalResults+" results.");
							});
						}
						if(sl.nextPageToken != null) {
							setNodesDisabled(false, nextPage);
							pageToken = sl.nextPageToken;
						}
					} catch (JsonSyntaxException | IOException e) {
						Platform.runLater(() -> resultStatus.setText(e.getMessage()));
						e.printStackTrace();
					}
					setNodesDisabled(false, search, searchField, searchMethod, locField, locDistance, searchOrder);
					if(searchMethod.getSelectionModel().getSelectedIndex() == 0) searchType.setDisable(false);
					return null;
				}
			};
			Thread thread = new Thread(task);
			thread.setDaemon(true);
			thread.start();
		} else if(o.equals(selectAll)) {
			ObservableList<Node> list = searchResults.getChildren();
			boolean allSelected = true;
			for(Node n : list) {
				if(n instanceof SearchResult) {
					SearchResult r = (SearchResult) n;
					if(!r.isSelected()) {
						allSelected = false;
						break;
					}
				}
			}
			for(Node n : list) {
				if(n instanceof SearchResult) {
					SearchResult r = (SearchResult) n;
					r.setSelected(!allSelected);
				}
			}
		} else if(o.equals(clearResults)) {
			setNodesDisabled(true, selectAll, clearResults, addToGroup, nextPage, resultStatus);
			searchResults.getChildren().clear();
		} else if(o.equals(addToGroup)) {
			if(!layout.getChildren().contains(addGroup)) {
				addGroup = createAddToGroupPane();
				layout.getChildren().add(addGroup);
			}
		} else if(o.equals(createGroup) || o.equals(deleteGroup) || o.equals(renameGroup) || o.equals(refreshGroup) || o.equals(reloadGroup)) {
			if(o.equals(createGroup)) {
				List<Group> allGroups = choice.getItems();
				TextInputDialog input = new TextInputDialog("");
				input.setTitle("Create Group");
				input.setContentText("Pick a unique nameProperty: ");
				input.showAndWait().ifPresent(result -> {
					boolean unique = true;
					for(Group g : allGroups) {
                        if(g.group_name.equals(result))
                            unique = false;
                    }
					if(unique) {
                        try {
                            database.insertGroup(result);
                            reloadGroups();
                        } catch (SQLException ignored) {}
                    }
				});
			} else if(o.equals(deleteGroup)) {
				Group current = choice.getSelectionModel().getSelectedItem();
				Dialog<ButtonType> dialog = new Dialog<>();
				dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
				dialog.setContentText("Are you sure you want to delete '"+current.group_name+"' and all of its data?");
				dialog.showAndWait().ifPresent(result -> {
					if(result == ButtonType.OK) {
						Task<Void> task = new Task<Void>(){
							protected Void call() throws Exception {
								setNodesDisabled(true, choice, createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB);
								try {
									database.removeGroupAndData(current);
									Platform.runLater(() -> {
										try {
											reloadGroups();
										} catch (SQLException e) {
											e.printStackTrace();
										}
									});
								} catch (SQLException e) {
									e.printStackTrace();
								}
								setNodesDisabled(false, choice, createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB);
								return null;
							}
						};
						Thread thread = new Thread(task);
						thread.setDaemon(true);
						thread.start();
					}
				});
			} else if(o.equals(renameGroup)) {
				Group current = choice.getSelectionModel().getSelectedItem();
				TextInputDialog input = new TextInputDialog(current.group_name);
				input.setTitle("Rename Group");
				input.setContentText("Pick a new nameProperty: ");
				input.showAndWait().ifPresent(result -> {
					if(!current.group_name.equals(result)) {
						try {
							database.updateGroupName(current.group_id, result);
							reloadGroups();
						} catch (SQLException ignored) {}
					}
				});
			} else if(o.equals(refreshGroup)) {
				Task<Void> task = new Task<Void>() {
					protected Void call() throws Exception {
						if(manager != null) {
							Platform.runLater(() -> {
								manager.refresh();
								setNodesDisabled(true, deleteGroup, renameGroup, refreshGroup, reloadGroup);
							});
						}
						return null;
					}
				};
				new Thread(task).start();
			} else if(o.equals(reloadGroup)) {
				Platform.runLater(() -> {
					reloadGroup.setDisable(true);
					manager.reloadGroupData();
					reloadGroup.setDisable(false);
				});

			}
		} else if(o.equals(cleanDB) || o.equals(resetDB)) {
			if(o.equals(cleanDB)) {
				layout.getChildren().add(createCleanDbPane());
			} else if(o.equals(resetDB)) {
				layout.getChildren().add(createResetDbPane());
			}
		}
	}

	private StackPane createCleanDbPane() {
		Label title = new Label("Clean Database");
		title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

		Label about = new Label("Repacks database into smallest size (VACUUM).");
		Label warn = new Label("Warning: This may take some time.");

		ProgressIndicator pi = new ProgressIndicator();
		pi.setMaxHeight(20);
		pi.setMaxWidth(20);
		pi.setVisible(false);

		Button cancel = new Button("Cancel");
		Button clean = new Button("Clean");
		clean.setStyle("-fx-base: derive(cornflowerblue, 80%);");

		HBox hbox = new HBox(10);
		hbox.setAlignment(Pos.CENTER_RIGHT);
		hbox.getChildren().addAll(pi, cancel, clean);

		VBox vbox = new VBox(10);
		vbox.setId("stackMenu");
		vbox.setPadding(new Insets(25,25,25,25));
		vbox.setMaxWidth(350);
		vbox.setMaxHeight(0);
		vbox.getChildren().addAll(title, about, warn, hbox);

		StackPane glass = new StackPane();
		glass.setStyle("-fx-background-color: rgba(127,127,127,0.5);");
		glass.setMaxHeight(Double.MAX_VALUE);
		glass.setMaxWidth(Double.MAX_VALUE);
		glass.setAlignment(Pos.CENTER);
		glass.getChildren().add(vbox);
		cancel.setOnAction(ae -> layout.getChildren().remove(glass));
		clean.setOnAction(ae -> {
			Task<Void> task = new Task<Void>() {
				protected Void call() throws Exception {
					setNodesDisabled(true, cancel, clean);
					pi.setVisible(true);
					try {
						database.clean();
					} catch (SQLException e) {
						e.printStackTrace();
					}
					Platform.runLater(() -> {
						cancel.setText("Close");
						clean.setVisible(false);
						clean.setManaged(false);
						pi.setVisible(false);
					});
					setNodesDisabled(false, cancel, clean);
					if(manager != null) {
						setupWithManager(manager);
					}
					return null;
				}
			};
			Thread thread = new Thread(task);
			thread.setDaemon(true);
			thread.start();
		});
		return glass;
	}

	private StackPane createResetDbPane() {
		Label title = new Label("Reset Database");
		title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

		Label warn = new Label("Warning: There is no going back!");
		warn.setStyle("-fx-text-fill: red;");

		ProgressIndicator pi = new ProgressIndicator();
		pi.setMaxHeight(20);
		pi.setMaxWidth(20);
		pi.setVisible(false);

		Button cancel = new Button("Cancel");
		Button reset = new Button("Yes, delete everything.");
		reset.setStyle("-fx-base: firebrick;");

		HBox hbox = new HBox(10);
		hbox.setAlignment(Pos.CENTER_RIGHT);
		hbox.getChildren().addAll(pi, reset, cancel);

		VBox vbox = new VBox(10);
		vbox.setId("stackMenu");
		vbox.setPadding(new Insets(25,25,25,25));
		vbox.setMaxWidth(350);
		vbox.setMaxHeight(0);
		vbox.getChildren().addAll(title, new Label("Delete all data and thumbnails."), new Label("Does not remove Youtube sign-ins."), warn, hbox);

		StackPane glass = new StackPane();
		glass.setStyle("-fx-background-color: rgba(127,127,127,0.5);");
		glass.setMaxHeight(Double.MAX_VALUE);
		glass.setMaxWidth(Double.MAX_VALUE);
		glass.setAlignment(Pos.CENTER);
		glass.getChildren().add(vbox);
		cancel.setOnAction(ae -> layout.getChildren().remove(glass));
		reset.setOnAction(ae -> {
			Task<Void> task = new Task<Void>(){
				protected Void call() throws Exception {
					setNodesDisabled(true, reset, cancel);
					pi.setVisible(true);
					try {
						database.dropTables();
						database.setup();
						database.clean();
						Platform.runLater(() -> {
							try {
								reloadGroups();
							} catch (SQLException e) {
								e.printStackTrace();
							}
						});
						File thumbs = new File("Thumbs/");
						if(thumbs.exists()) {
							for(File f : thumbs.listFiles()) {
								f.delete();
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					Platform.runLater(() -> {
						cancel.setText("Close");
						reset.setVisible(false);
						reset.setManaged(false);
						pi.setVisible(false);
					});
					setNodesDisabled(false, reset, cancel);
					if(manager != null) {
						setupWithManager(manager);
					}
					return null;
				}
			};
			Thread thread = new Thread(task);
			thread.setDaemon(true);
			thread.start();
		});
		return glass;
	}

	public void start(Stage arg0) throws Exception {
		instance = this;
		stage = arg0;
		loadConfig();

		database = new DatabaseManager();
		database.setup();

		layout = new StackPane();
		setup = createSetupPane();
		menu = createMenuPane();
		videos = createVideosPane();
		groups = createGroupsPane();
		comments = createCommentsPane();

		main = new GridPane();
		main.setAlignment(Pos.TOP_LEFT);
		main.setVgap(10);
		ColumnConstraints col = new ColumnConstraints();
		col.setPercentWidth(100);
		main.getColumnConstraints().add(col);

		main.add(menu, 0, 0);
		GridPane.setHgrow(menu, Priority.ALWAYS);

		layout.getChildren().addAll(main);

		reloadGroups();
		videoToggle.fire();

		Scene scene = new Scene(layout, 900, 550);
		scene.getStylesheets().add(
				getClass().getResource("commentsuitefx.css").toExternalForm()
		);
		stage.setScene(scene);
		stage.setTitle("Youtube Comment Suite");
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuite/images/fxicon.png")));
		stage.setOnCloseRequest(e -> {
			Platform.exit();
			System.exit(0);
		});
		stage.show();
	}

	private StackPane createAddToGroupPane() {
		ToggleGroup toggle = new ToggleGroup();
		RadioButton existing = new RadioButton("Existing group.");
		existing.setToggleGroup(toggle);
		RadioButton newGroup = new RadioButton("Make new group.");
		newGroup.setToggleGroup(toggle);

		ChoiceBox<Group> groupList = new ChoiceBox<>();
		groupList.getItems().addAll(choice.getItems());

		TextField field = new TextField();
		field.setMinWidth(200);
		field.setPromptText("Choose a unique nameProperty.");


		existing.setOnAction(e -> {
			if(existing.isSelected()) {
				groupList.setDisable(false);
				field.setDisable(true);
			}
		});
		newGroup.setOnAction(e -> {
			if(newGroup.isSelected()) {
				groupList.setDisable(true);
				field.setDisable(false);
			}
		});

		if(groupList.getItems().isEmpty()) {
			existing.setDisable(true);
			newGroup.fire();
		} else {
			existing.fire();
			groupList.getSelectionModel().select(0);
		}

		List<GitemType> items = new ArrayList<>();
		searchResults.getChildren().stream().filter(object -> object instanceof SearchResult && ((SearchResult) object).isSelected()).forEach(result -> items.add(((SearchResult) result).gitem));
		
		/*dialog.showAndWait().ifPresent(choice -> {
			if(choice == ButtonType.OK) {
				
			}
		});*/

		Button ok = new Button("Finish");
		Button cancel = new Button("Cancel");
		HBox hbox = new HBox(10);
		hbox.getChildren().addAll(cancel, ok);
		hbox.setAlignment(Pos.CENTER_RIGHT);

		Label title = new Label("Add "+items.size()+" items to group: ");
		title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 16));

		VBox vbox = new VBox(10);
		vbox.setId("stackMenu");
		vbox.setMaxHeight(0);
		vbox.setMaxWidth(0);
		vbox.setFillWidth(true);
		vbox.setPadding(new Insets(25,25,25,25));
		vbox.setAlignment(Pos.CENTER_LEFT);
		vbox.getChildren().addAll(title, existing, groupList, newGroup, field, hbox);

		StackPane glass = new StackPane();
		glass.setStyle("-fx-background-color: rgba(127,127,127,0.5);");
		glass.setMaxHeight(Double.MAX_VALUE);
		glass.setMaxWidth(Double.MAX_VALUE);
		glass.setAlignment(Pos.CENTER);
		glass.getChildren().add(vbox);
		cancel.setOnAction(ae -> layout.getChildren().remove(glass));
		ok.setOnAction(ae -> {
			String group_name;
			try {
				Group group;
				if(newGroup.isSelected()) {
					boolean unique = true;
					group_name = field.getText();
					for(Group g : groupList.getItems()) if(g.group_name.equals(group_name)) {
						unique = false;
						break;
					}
					if(unique) {
						database.insertGroup(group_name);
						reloadGroups();
					}
					group = database.getGroup(group_name);
				} else {
					group = groupList.getSelectionModel().getSelectedItem();
				}
				List<String> currentItems = database.getGitems(group.group_id, false).stream().map(YoutubeObject::getId).collect(Collectors.toList());
				database.insertGitems(group.group_id, items.stream().filter(gitem -> !currentItems.contains(gitem.getId())).collect(Collectors.toList()));
				layout.getChildren().remove(glass);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
		return glass;
	}

	private HBox createCommentsPane() {
		HBox hbox = new HBox();
		hbox.setAlignment(Pos.TOP_LEFT);

		VBox context = new VBox(5);
		context.setPadding(new Insets(5,5,5,5));
		context.setFillWidth(true);
		context.setAlignment(Pos.TOP_CENTER);
		context.setMinWidth(330);
		context.setMaxWidth(330);
		context.setPrefWidth(330);

		thumbnail = new ImageView(placeholder);
		thumbnail.setFitHeight(180);
		thumbnail.setFitWidth(320);

		title = new TextField("Youtube Comment Suite");
		title.setId("context");
		title.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
		title.setEditable(false);

		HBox publisher = new HBox(5);
		publisher.setAlignment(Pos.CENTER_LEFT);
		authorThumb = new ImageView(CommentResult.BLANK_PROFILE);
		authorThumb.setFitHeight(24);
		authorThumb.setFitWidth(24);

		author = new TextField("Guest");
		author.setId("context");
		author.setEditable(false);
		HBox.setHgrow(author, Priority.ALWAYS);

		views = new Label("8675309 views");
		views.setAlignment(Pos.CENTER_RIGHT);

		likes = new Label("+ 8675");
		likes.setAlignment(Pos.CENTER_RIGHT);
		likes.setStyle("-fx-text-fill: green");
		dislikes = new Label("- 309");
		dislikes.setAlignment(Pos.CENTER_RIGHT);
		dislikes.setStyle("-fx-text-fill: red");

		description = new TextArea("Published Nov 18, 1918  This is an example description. You may select this text, the title, and author's nameProperty. Right click to copy or select all."
				+ "\n\nThe thumbnail and author's picture are clickable to open either the video or channel in your browser."
				+ "\n\nComments may be replied to if you are signed in. Commentor names may be clicked to open their channel in browser."
				+ "\n\nNote that grabbed comment numbers may be slightly off due to Youtube spam detection and the channel's user and phrase filters.");
		description.setEditable(false);
		description.setWrapText(true);
		description.setMaxWidth(320);
		description.setMaxHeight(Double.MAX_VALUE);
		description.setPrefWidth(320);
		VBox.setVgrow(description, Priority.ALWAYS);

		HBox likeDislike = new HBox(5);
		likeDislike.getChildren().addAll(likes, dislikes);

		VBox stats = new VBox();
		stats.setFillWidth(true);
		stats.getChildren().addAll(views, likeDislike);

		publisher.getChildren().addAll(authorThumb, author, stats);

		context.getChildren().addAll(thumbnail, title, publisher, description);

		VBox resultBox = new VBox(5);
		resultBox.setFillWidth(true);
		resultBox.setAlignment(Pos.TOP_CENTER);

		commentResults = new VBox(5);
		commentResults.setAlignment(Pos.TOP_CENTER);
		commentResults.setFillWidth(true);

		cscroll = new ScrollPane(commentResults);
		cscroll.setMaxWidth(Double.MAX_VALUE);
		cscroll.setFitToWidth(true);

		HBox resultControls = new HBox(5);
		resultControls.setPadding(new Insets(0,0,5,0));
		resultControls.setAlignment(Pos.CENTER);

		clearComments = new Button("Clear Comments");
		clearComments.setOnAction(e -> {
			Platform.runLater(()->{
				commentResults.getChildren().clear();
				backToResults.setDisable(true);
				resultCount.setText("Showing 0 out of 0 results.");
			});
			results.clear();
		});

		nextPageC = new Button(">");
		nextPageC.setDisable(true);
		nextPageC.setOnAction(e -> {
			if(query != null && page+1 <= query.getPageCount()) {
				try {
					loadQueryPage(++page);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
		prevPageC = new Button("<");
		prevPageC.setDisable(true);
		prevPageC.setOnAction(e -> {
			if(query != null && page-1 >= 1) {
				try {
					loadQueryPage(--page);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
		firstPage = new Button("<<");
		firstPage.setDisable(true);
		firstPage.setOnAction(e -> {
			if(query != null && page-1 >= 1) {
				try {
					loadQueryPage(1);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
		lastPage = new Button(">>");
		lastPage.setDisable(true);
		lastPage.setOnAction(e -> {
			if(query != null && page-1 >= 1) {
				try {
					loadQueryPage(query.getPageCount());
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
		pageNum = new TextField();
		pageNum.setEditable(false);
		pageNum.textProperty().addListener((observable, oldValue, newValue) -> pageNum.setPrefWidth(pageNum.getText().length() * 6.5));
		pageNum.setText(" Page 1 of 0 ");

		HBox box = new HBox();
		box.getChildren().addAll(firstPage, prevPageC, pageNum, nextPageC, lastPage);

		resultCount = new Label("Showing 0 out of 0 results.");

		backToResults = new Button("Return to Results");
		backToResults.setStyle("-fx-base: seagreen");
		backToResults.setDisable(true);
		backToResults.setOnAction(e -> returnToResults());

		resultControls.getChildren().addAll(clearComments, backToResults, box, resultCount);
		resultBox.getChildren().addAll(cscroll, resultControls);

		searchBox = new VBox(10);
		searchBox.setMinWidth(320);
		searchBox.setMaxWidth(320);
		searchBox.setPrefWidth(320);
		searchBox.setPadding(new Insets(5,5,5,5));
		searchBox.setAlignment(Pos.TOP_CENTER);
		searchBox.setFillWidth(true);

		Label label1 = new Label("Select Group");
		label1.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 16));
		label1.setAlignment(Pos.CENTER);

		cgroup = new ChoiceBox<>();
		cgroup.setMaxWidth(300);
		cgroup.setPrefWidth(300);
		cgroup.setOnAction(e -> {
			Task<Void> task = new Task<Void>() {
				protected Void call() throws Exception {
					try {
						cgroup.setDisable(true);
						citem.setDisable(true);
						loadCGroup(cgroup.getValue());
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
					cgroup.setDisable(false);
					citem.setDisable(false);
					return null;
				}
			};
			new Thread(task).start();
		});
		citem = new ChoiceBox<>();
		citem.setMaxWidth(300);
		citem.setPrefWidth(300);

		videoContext = new ToggleButton("Show Video Context");
		videoContext.setTooltip(new Tooltip("Context appears when you select a comment."));
		videoContext.setOnAction(e -> {
			if(!videoContext.isSelected()) {
				context.setVisible(false);
				context.setMinWidth(0);
				context.setMaxWidth(0);
				context.setPrefWidth(0);
			} else {
				context.setVisible(true);
				context.setMinWidth(330);
				context.setMaxWidth(330);
				context.setPrefWidth(330);
			}
		});
		videoContext.fire();

		Label label2 = new Label("Restrict Results");
		label2.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 16));
		label2.setAlignment(Pos.CENTER);

		type = new ComboBox<>();
		type.setMaxWidth(Double.MAX_VALUE);
		type.getItems().addAll("Comments and Replies", "Comments Only", "Replies Only");
		type.getSelectionModel().select(0);

		orderby = new ComboBox<>();
		orderby.setMaxWidth(Double.MAX_VALUE);
		orderby.getItems().addAll("Most Recent", "Least Recent", "Most Likes", "Most Replies", "Longest Comment", "Names (A to Z)", "Comments (A to Z)");
		orderby.getSelectionModel().select(0);

		GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_CENTER);
		grid.setVgap(10);
		grid.setHgap(10);
		ColumnConstraints cc1 = new ColumnConstraints();
		ColumnConstraints cc2 = new ColumnConstraints();
		cc2.setFillWidth(true);
		grid.getColumnConstraints().addAll(cc1, cc2);

		grid.addRow(0, new Label("Sort by: "), orderby);

		userLike = new TextField();
		userLike.setPromptText("Username contains...");
		GridPane.setHgrow(userLike, Priority.ALWAYS);
		grid.addRow(1, new Label("Name like: "), userLike);

		textLike = new TextField();
		textLike.setPromptText("Comment contains...");
		GridPane.setHgrow(textLike, Priority.ALWAYS);
		grid.addRow(2, new Label("Text like: "), textLike);

		DatePicker afterDate = new DatePicker();
		setDatePickerTime(afterDate, 0);
		grid.addRow(3, new Label("Date from: "), afterDate);

		DatePicker beforeDate = new DatePicker();
		setDatePickerTime(beforeDate, System.currentTimeMillis());
		grid.addRow(4, new Label("Date To: "), beforeDate);

		find = new Button("Find Comments");
		find.setMaxWidth(Double.MAX_VALUE);
		find.setOnAction(e -> {
			find.setDisable(true);
			Task<Void> task = new Task<Void>() {
				protected Void call() throws Exception {
					try {
						int groupId = cgroup.getValue().group_id;
						int order = orderby.getSelectionModel().getSelectedIndex();
						String user = userLike.getText();
						String text = textLike.getText();
						int limit = 250;
						GitemType gitem = citem.getValue().getGitemId() != -1 ? citem.getValue() : null;
						int comment_type =  type.getSelectionModel().getSelectedIndex();
						query = database.newCommentQuery()
								.group(groupId)
								.groupItem(gitem)
								.orderBy(order)
								.nameLike(user)
								.textLike(text)
								.limit(limit)
								.after(getDatePickerDate(afterDate, false))
								.before(getDatePickerDate(beforeDate, true))
								.cType(comment_type);
						loadQueryPage(1);
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return null;
				}
			};
			new Thread(task).start();
		});

		searchBox.getChildren().addAll(label1, cgroup, citem, videoContext, label2, type, grid, find);
		hbox.getChildren().addAll(context, resultBox, searchBox);
		HBox.setHgrow(resultBox, Priority.ALWAYS);

		return hbox;
	}

	private void setDatePickerTime(DatePicker picker, long time) {
		LocalDate date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();
		picker.setValue(date);
	}

	private Date getDatePickerDate(DatePicker picker, boolean midnightTonight) {
		LocalDate localDate = picker.getValue();
		Instant instant = Instant.from(localDate.atStartOfDay(ZoneId.systemDefault()));

		Calendar cal = new GregorianCalendar();
		cal.setTime(Date.from(instant));
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if(midnightTonight) {
			cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR)+1);
		}
		System.out.println(cal.getTime().toString());
		return cal.getTime();
	}

	private void loadQueryPage(int page) throws SQLException {
		this.page = page;
		Platform.runLater(() -> setNodesDisabled(true, find, prevPageC, nextPageC, firstPage, lastPage));
		final List<CommentResult> list = query.get(page).stream()
				.map(c -> new CommentResult(c, true))
				.collect(Collectors.toList());
		results = list;
		Platform.runLater(() -> {
			prevPageC.setDisable(page == 1);
			nextPageC.setDisable(page == query.getPageCount());
			firstPage.setDisable(page == 1);
			lastPage.setDisable(page == query.getPageCount());
			pageNum.setText(" Page "+page+" of "+query.getPageCount()+" ");
			resultCount.setText("Showing "+list.size()+" out of "+query.getTotalResults()+" results.");
			commentResults.getChildren().clear();
			commentResults.getChildren().addAll(list);
			find.setDisable(false);
			backToResults.setDisable(true);
			vValue = 0;
			cscroll.layout();
			cscroll.setVvalue(vValue);
			setNodesDisabled(false, find);
		});
	}

	private void returnToResults() {
		backToResults.setDisable(true);
		commentResults.getChildren().clear();
		commentResults.getChildren().addAll(results);
		cscroll.layout();
		cscroll.setVvalue(vValue);
	}

	public void viewTree(CommentType comment) throws SQLException {
		vValue = cscroll.getVvalue();
		tree = database.getCommentTree(comment.isReply() ? comment.getParentId() : comment.getId()).stream()
				.map(c -> new CommentResult(c, false))
				.collect(Collectors.toList());
		commentResults.getChildren().clear();
		commentResults.getChildren().addAll(tree);
		find.setDisable(false);
		backToResults.setDisable(false);
		cscroll.layout();
		cscroll.setVvalue(0);
	}

	public void loadContext(String videoId) throws SQLException {
		VideoType video = database.getVideo(videoId, true);
		thumbnail.setImage(video.fetchThumb());
		thumbnail.setCursor(Cursor.HAND);
		thumbnail.setOnMouseClicked(e -> {
			if(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 1) {
				openInBrowser(video.getYoutubeLink());
			}
		});
		ChannelType channel = DatabaseManager.getChannel(video.getChannelId());
		authorThumb.setImage(channel.fetchThumb() != null ? channel.fetchThumb() : CommentResult.BLANK_PROFILE);
		authorThumb.setCursor(Cursor.HAND);
		authorThumb.setOnMouseClicked(e -> {
			if(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 1) {
				openInBrowser(video.getYoutubeLink());
			}
		});
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
		description.setText("Published on "+sdf.format(video.getPublishDate())+"  "+video.getDescription());
		title.setText(video.getTitle());
		author.setText(channel.getTitle());
		likes.setText("+"+video.getLikes());
		dislikes.setText("-"+video.getDislikes());
		views.setText(video.getViews()+" views");
	}

	private void loadCGroup(Group g) throws SQLException {
		final List<GitemType> items = database.getGitems(g.group_id, false);
		Platform.runLater(() -> {
			citem.getItems().clear();
			citem.getItems().add(new GitemType(-1, "All Items ("+items.size()+")"));
			citem.getItems().addAll(items);
			citem.getSelectionModel().select(0);
			// Not smart to load all relevant videos into another ChoiceBox, too slow and list has potential to be gigantic. 
		});
	}

	private GridPane createGroupsPane() {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_CENTER);
		grid.setHgap(5);
		grid.setVgap(5);

		HBox menu = new HBox(5);
		menu.setAlignment(Pos.CENTER);
		grid.add(menu, 0, 0);

		Label label = new Label("Select a group: ");

		choice = new ChoiceBox<>();
		choice.setMaxWidth(250);
		choice.setPrefWidth(150);
		choice.setOnAction(e -> {
			deleteGroup.setDisable(true);
			renameGroup.setDisable(true);
			refreshGroup.setDisable(true);
			Group group = choice.getSelectionModel().getSelectedItem();
			if(group != null) {
				if(GroupManager.managers.containsKey(group.group_id)) {
					manager = GroupManager.managers.get(group.group_id);
				} else {
					manager = new GroupManager(group, database, data);
					GroupManager.managers.put(group.group_id, manager);
				}
				for(GroupManager gm : GroupManager.managers.values()) {
					if(grid.getChildren().contains(gm)) {
						grid.getChildren().remove(gm);
					}
				}
				setupWithManager(manager);
				grid.add(manager, 0, 1);
				GridPane.setHgrow(manager, Priority.ALWAYS);
				GridPane.setVgrow(manager, Priority.ALWAYS);
			}
		});

		createGroup = new Button("Create");
		createGroup.setTooltip(new Tooltip("Create a new, empty group."));
		createGroup.setOnAction(this);

		deleteGroup = new Button("Delete");
		deleteGroup.setTooltip(new Tooltip("Delete this group and all its data."));
		deleteGroup.setOnAction(this);
		deleteGroup.setDisable(true);
		deleteGroup.setStyle("-fx-base: mistyrose");

		renameGroup = new Button("Rename");
		renameGroup.setTooltip(new Tooltip("Rename this group."));
		renameGroup.setOnAction(this);
		renameGroup.setDisable(true);

		refreshGroup = new Button("Refresh");
		refreshGroup.setTooltip(new Tooltip("Check for new videos, comments, and replies."));
		refreshGroup.setOnAction(this);
		refreshGroup.setDisable(true);
		refreshGroup.setStyle("-fx-base: honeydew");

		reloadGroup = new Button("Reload");
		reloadGroup.setTooltip(new Tooltip("Reloads the data displayed."));
		reloadGroup.setOnAction(this);
		reloadGroup.setDisable(true);

		Separator sep = new Separator();
		sep.setOrientation(Orientation.VERTICAL);

		cleanDB = new Button("Clean DB");
		cleanDB.setTooltip(new Tooltip("Perform a VACUUM on the database."));
		cleanDB.setOnAction(this);

		resetDB = new Button("Reset DB");
		resetDB.setTooltip(new Tooltip("Delete everything and start from scratch. Does not affect sign-in or key."));
		resetDB.setOnAction(this);
		resetDB.setStyle("-fx-base: firebrick");

		menu.getChildren().addAll(label, choice, createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup, sep, cleanDB, resetDB);

		return grid;
	}

	public void setupWithManager(GroupManager gm) {
		boolean disable = manager.isRefreshing();
		setNodesDisabled(disable, deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB);
	}

	private void reloadGroups() throws SQLException {
		manager = null;
		int selected = choice.getSelectionModel().getSelectedIndex();
		List<Group> groups = database.getGroups();
		choice.getItems().clear();
		cgroup.getItems().clear();
		citem.getItems().clear();
		choice.getItems().addAll(groups);
		cgroup.getItems().addAll(groups);
		int select = -1;
		if(choice.getItems().size() > 0) {
			if(selected == -1 || choice.getItems().size() <= selected) {
				select = -1;
			} else {
				select = selected;
			}
			cgroup.getSelectionModel().select(0);
			loadCGroup(cgroup.getValue());
		}
		choice.getSelectionModel().select(select);
	}

	private GridPane createVideosPane() {
		GridPane grid = new GridPane(); // Search options.
		grid.setPadding(new Insets(5,5,5,5));
		grid.setId("videoPane");
		grid.setAlignment(Pos.TOP_CENTER);
		grid.setHgap(5);
		grid.setVgap(10);

		GridPane grid2 = new GridPane(); // Results
		grid2.setHgap(5);
		grid2.setVgap(10);

		locField = new TextField();
		locField.setPromptText("40.7058253,-74.1180864");

		locDistance = new ComboBox<>();
		locDistance.getItems().addAll("1km", "2km", "5km", "10km", "15km", "20km", "25km", "30km", "50km", "75km", "100km", "200km", "500km", "1000km");
		locDistance.getSelectionModel().select(3);

		searchMethod = new ComboBox<>();
		searchMethod.getItems().addAll("Normal", "Location");
		searchMethod.setOnAction(e -> {
			String method = searchMethod.getSelectionModel().getSelectedItem();
			ObservableList<Node> list = grid.getChildren();
			if(method.equals("Normal")) {
				if(list.contains(locField)) {
					list.removeAll(locField, locDistance, search, searchOrder, searchType);
					grid.add(search, 2, 0);
					grid.add(searchOrder, 3, 0);
					grid.add(searchType, 4, 0);
					searchType.setDisable(false);
					GridPane.setColumnSpan(grid2, 5);
				}
			} else if(method.equals("Location")) {
				if(!list.contains(locField)) {
					list.removeAll(locField, locDistance, search, searchOrder, searchType);
					grid.add(locField, 2, 0);
					grid.add(locDistance, 3, 0);
					grid.add(search, 4, 0);
					grid.add(searchOrder, 5, 0);
					grid.add(searchType, 6, 0);
					searchType.getSelectionModel().select(1);
					searchType.setDisable(true);
					GridPane.setColumnSpan(grid2, 7);
				}
			}
		});
		searchMethod.getSelectionModel().select(0);
		grid.add(searchMethod, 0, 0);

		searchField = new TextField();
		searchField.setPromptText("Search");
		grid.add(searchField, 1, 0);
		GridPane.setHgrow(searchField, Priority.ALWAYS);

		search = new Button("Search");
		search.setOnAction(this);
		grid.add(search, 2, 0);
		searchField.setOnKeyPressed(ke -> {
			if(ke.getCode().equals(KeyCode.ENTER)) {
				search.fire();
			}
		});

		searchOrder = new ComboBox<>();
		searchOrder.getItems().addAll("Relevance", "Date", "Title", "Rating", "Views");
		searchOrder.getSelectionModel().select(0);
		grid.add(searchOrder, 3, 0);

		searchType = new ComboBox<>();
		searchType.getItems().addAll("All Types", "Video", "Channel", "Playlist");
		searchType.getSelectionModel().select(0);
		grid.add(searchType, 4, 0);

		grid.add(grid2, 0, 1, 5, 1);

		searchResults = new VBox();
		searchResults.setPadding(new Insets(10,10,10,10));
		searchResults.setAlignment(Pos.TOP_CENTER);

		scroll = new ScrollPane(searchResults);
		scroll.setFitToWidth(true);
		scroll.setFitToHeight(true);
		grid2.add(scroll, 0, 0);
		GridPane.setHgrow(scroll, Priority.ALWAYS);

		selectAll = new Button("Select All");
		selectAll.setTooltip(new Tooltip("Select or deselect all visible items."));
		selectAll.setDisable(true);
		selectAll.setOnAction(this);

		clearResults = new Button("Clear Results");
		clearResults.setTooltip(new Tooltip("Clear all visible items."));
		clearResults.setDisable(true);
		clearResults.setOnAction(this);

		addToGroup = new Button("Add To Group");
		addToGroup.setTooltip(new Tooltip("Add selected results to a group."));
		addToGroup.setId("completeForm");
		addToGroup.setDisable(true);
		addToGroup.setOnAction(this);

		nextPage = new Button("Next Page");
		nextPage.setTooltip(new Tooltip("Get the next page of results."));
		nextPage.setDisable(true);
		nextPage.setOnAction(this);

		resultStatus = new Label();

		HBox hbox = new HBox(5);
		hbox.setAlignment(Pos.CENTER);
		hbox.getChildren().addAll(selectAll, clearResults, addToGroup, nextPage, resultStatus);
		hbox.setPadding(new Insets(0,0,5,0));

		grid2.add(hbox, 0, 1);

		return grid;
	}

	private GridPane createMenuPane() {
		int height = 26;
		GridPane grid  = new GridPane();
		grid.setId("menuBar");
		grid.setMaxWidth(Double.MAX_VALUE);
		grid.setMaxHeight(height);

		StackPane img = new StackPane();
		header = new ImageView(new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuite/images/youtube.png")));
		header.setFitHeight(height);
		header.setFitWidth(height * header.getImage().getWidth() / header.getImage().getHeight());
		img.getChildren().add(header);
		img.setPadding(new Insets(0,15,0,15));
		grid.add(img, 0, 0);

		ToggleGroup toggle = new ToggleGroup();
		toggle.getToggles().addListener((ListChangeListener<Toggle>) c -> {
			while(c.next()) {
				for(final Toggle addedToggle : c.getAddedSubList()) {
					((ToggleButton) addedToggle).addEventFilter(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
						if (addedToggle.equals(toggle.getSelectedToggle()))
							mouseEvent.consume();
					});
				}
			}
		});

		videoToggle = new ToggleButton("Search Youtube");
		videoToggle.setMaxHeight(height);
		videoToggle.setToggleGroup(toggle);
		videoToggle.setId("menuButton");
		videoToggle.setOnAction(this);
		grid.add(videoToggle, 1, 0);

		groupToggle = new ToggleButton("Manage Groups");
		groupToggle.setMaxHeight(height);
		groupToggle.setToggleGroup(toggle);
		groupToggle.setId("menuButton");
		groupToggle.setOnAction(this);
		grid.add(groupToggle, 2, 0);

		commentToggle = new ToggleButton("Search Comments");
		commentToggle.setMaxHeight(height);
		commentToggle.setToggleGroup(toggle);
		commentToggle.setId("menuButton");
		commentToggle.setOnAction(this);
		grid.add(commentToggle, 3, 0);

		welcome = new Label();
		welcome.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 14));
		welcome.setPadding(new Insets(0,15,0,15));
		welcome.setMaxWidth(Double.MAX_VALUE);
		grid.add(welcome, 4, 0);

		gotoSetup = new Hyperlink("Setup");
		gotoSetup.setOnAction(e -> {
			if(!layout.getChildren().contains(setup)) {
				saveAndSetup.setDisable(false);
				layout.getChildren().add(setup);
			}
		});
		grid.add(gotoSetup, 5, 0);

		GridPane.setValignment(welcome, VPos.CENTER);
		GridPane.setHgrow(welcome, Priority.ALWAYS);

		return grid;
	}

	private StackPane createSetupPane() {
		Label title = new Label("Login to Youtube");
		title.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label desc = new Label("Sign in to leave comments and replies.");
		desc.setWrapText(true);

		signin = new Button("Add Account");
		signin.setOnAction(ae -> {
			Task<Void> task = new Task<Void>(){
				protected Void call() throws Exception {
					System.out.println("OAuth2");
					Platform.runLater(() -> {
						WebView web = new WebView();
						WebEngine engine = web.getEngine();
						try {
							engine.load(OA2Handler.getOAuth2Url());
							web.setPrefSize(400, 575);
							Dialog<WebView> dialog = new Dialog<>();
							dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
							dialog.getDialogPane().setContent(web);
							dialog.titleProperty().bind(engine.titleProperty());
							engine.titleProperty().addListener(e -> {
								System.out.println("CHANGE: "+engine.getTitle());
								if(engine.getTitle() != null && (engine.getTitle().contains("code=") || engine.getTitle().contains("error="))) {
									String response = engine.getTitle();
									String code = response.substring(13, response.length());
									web.setDisable(true);
									try {
										OA2Tokens tokens = OA2Handler.getAccessTokens(code);
										getConfig().submitTokens(tokens);
										accountList.getChildren().clear();
										accountList.getChildren().addAll(getConfig().accounts.stream().map(acc -> new AccountPane(acc)).collect(Collectors.toList()));
										saveAndSetup.requestFocus();
									} catch (IOException e1) {
										e1.printStackTrace();
									}
									dialog.close();
								} else {
									System.out.println("    NO RESPONSE");
								}
							});
							dialog.showAndWait();
							System.out.println("OAuth2 Done");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					});
					return null;
				}
			};
			Thread thread = new Thread(task);
			thread.setDaemon(true);
			thread.start();
		});

		/*if(signin.getText().equals("Sign in")) {

		} else if(signin.getText().equals("Sign out")) {
			Task<Void> task = new Task<Void>() {
				protected Void call() throws Exception {
					signOut();
					loadConfig();
					return null;
				}
			};
			Thread thread = new Thread(task);
			thread.setDaemon(true);
			thread.start();
		} else {
			System.out.println("Sign in/out: Something broke.");
		}*/

		HBox hBtn = new HBox(10);
		saveAndSetup = new Button("Save and Setup");
		saveAndSetup.setOnAction(this);
		saveAndSetup.setId("completeForm");
		exitSetup = new Button("Close");
		exitSetup.setOnAction(this);
		hBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hBtn.getChildren().addAll(exitSetup, saveAndSetup);

		VBox vbox = new VBox(10);
		vbox.setId("stackMenu");
		vbox.setAlignment(Pos.TOP_CENTER);
		vbox.setMaxWidth(300);
		vbox.setMaxHeight(0);
		vbox.setFillWidth(true);
		vbox.setPadding(new Insets(25,25,25,25));
		vbox.getChildren().addAll(title, desc, signin, accountList, hBtn);

		StackPane glass = new StackPane();
		glass.setStyle("-fx-background-color: rgba(127,127,127,0.5);");
		glass.setMaxHeight(Double.MAX_VALUE);
		glass.setMaxWidth(Double.MAX_VALUE);
		glass.setAlignment(Pos.CENTER);
		glass.getChildren().add(vbox);
		return glass;
	}

	public void refreshResultProfiles() {
		if(results != null) {
			for(CommentResult comment : results) {
				comment.refreshImage();
			}
		}
		if(tree != null) {
			for(CommentResult comment : tree) {
				comment.refreshImage();
			}
		}
	}

	public static void openInBrowser(String link) {
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
