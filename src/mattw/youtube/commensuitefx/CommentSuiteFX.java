package mattw.youtube.commensuitefx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonSyntaxException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
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
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import mattw.youtube.datav3.YoutubeData;
import mattw.youtube.datav3.list.ChannelsList;
import mattw.youtube.datav3.list.SearchList;

public class CommentSuiteFX extends Application implements EventHandler<ActionEvent> {
	
	public static CommentSuiteFX app;
	public YoutubeData data;
	public YCSConfig config = new YCSConfig();
	public SuiteDatabase db = new SuiteDatabase("commentsuite.db");
	public Stage stage;
	final Image placeholder = new Image(CommentResult.class.getResourceAsStream("/mattw/youtube/commentsuite/images/placeholder4.png"));
	
	public StackPane layout, setup;
	public GridPane main, menu, videos, groups;
	public HBox comments;
	
	public ImageView header;
	public ToggleButton videoToggle, groupToggle, commentToggle;
	public Label welcome;
	public Hyperlink gotoSetup;
	
	public Button saveAndSetup, exitSetup;
	public Button signin;
	public Label status;
	public PasswordField keyField;
	
	public String pageToken = "";
	public Button search, selectAll, clearResults, addToGroup, nextPage;
	public TextField searchField, locField;
	public ComboBox<String> searchMethod, searchOrder, searchType, locDistance;
	public ScrollPane scroll;
	public VBox searchResults;
	public Label resultStatus;
	
	public GroupManager manager;
	public Button createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup, resetDB, cleanDB;
	public ChoiceBox<Group> choice;
	
	public VBox commentResults, searchBox;
	public ScrollPane cscroll;
	public double vValue = 0.0;
	public ImageView thumbnail, authorThumb;
	public TextField title, author;
	public Label views, likes, dislikes, resultCount;
	public TextArea description;
	public ChoiceBox<Group> cgroup;
	public ChoiceBox<GroupItem> citem;
	public ToggleButton videoContext;
	public TextField userLike, textLike;
	public Button find, backToResults, clearComments;
	public ComboBox<String> type, orderby;
	public List<CommentResult> results;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public void saveConfig() {
		Platform.runLater(() -> {
			if(layout.getChildren().contains(setup)) {
				config.setYoutubeKey(keyField.getText());
				try {
					config.save();
					loadConfig();
				} catch (IOException e) {}
				layout.getChildren().remove(setup);
			}
		});

	}
	
	public void loadConfig() throws IOException {
		config.load();
		Platform.runLater(() -> {
			keyField.setText(config.getYoutubeKey());
			welcome.setText("Welcome, "+config.getUsername());
			author.setText(config.getUsername());
			if(config.getAccessTokens() != null) {
				signin.setText("Sign out");
				signin.setStyle("-fx-base: firebrick");
				data.setAccessToken(config.getAccessTokens().access_token);
			} else {
				signin.setText("Sign in");
				signin.setStyle("");
			}
		});
		data = new YoutubeData(config.getYoutubeKey());
		if(!config.isSetup()) {
			Platform.runLater(() -> {
				layout.getChildren().add(setup);
			});
		}
	}
	
	public void refreshTokens() {
		if(config.getAccessTokens() != null) {
			OA2Tokens old_tokens = config.getAccessTokens();
			OA2Tokens new_tokens;
			try {
				new_tokens = OA2Handler.refreshAccessTokens(old_tokens);
				new_tokens.setRefreshToken(old_tokens.refresh_token);
				config.setAccessTokens(new_tokens);
				data.setAccessToken(new_tokens.access_token);
				config.save();
				checkSignin(new_tokens);
				System.out.println("Sign in success?");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void checkSignin(OA2Tokens tokens) throws JsonSyntaxException, IOException {
		ChannelsList cl = data.getChannelsByMine(ChannelsList.PART_SNIPPET);
		String title = cl.items[0].snippet.title;
		config.setUsername(title);
		config.setChannelId(cl.items[0].id);
		config.setAccessTokens(tokens);
		config.save();
		loadConfig();
		System.out.println("Signed in and loaded.");
	}
	
	public void signOut() throws IOException {
		config.setUsername("Guest");
		data.setAccessToken("");
		config.setAccessTokens(null);
		config.save();
	}
	
	public void setNodesDisabled(boolean disable, Node... nodes) {
		for(Node n : nodes) n.setDisable(disable);
	}
	
	public void handle(ActionEvent arg0) {
		Object o = arg0.getSource();
		if(o.equals(saveAndSetup) || o.equals(exitSetup) || o.equals(signin)) {
			if(o.equals(saveAndSetup)) {
				Task<Void> task = new Task<Void>(){
					protected Void call() throws Exception {
						setNodesDisabled(true, saveAndSetup, keyField);
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
				if(signin.getText().equals("Sign in")) {
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
												data.setAccessToken(tokens.access_token);
												checkSignin(tokens);
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
				}
			}
		} else if(o.equals(videoToggle) || o.equals(groupToggle) || o.equals(commentToggle)) {
			if(videoToggle.isSelected()) {
				if(main.getChildren().contains(groups)) main.getChildren().remove(groups);
				if(main.getChildren().contains(comments)) main.getChildren().remove(comments);
				if(!main.getChildren().contains(videos)) {
					main.add(videos, 0, 1);
					GridPane.setVgrow(videos, Priority.ALWAYS);
				}
				header.setImage(new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuite/images/icon.png")));
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
				protected Void call() throws Exception {
					setNodesDisabled(true, search, nextPage, searchField, searchMethod, locField, locDistance, searchOrder, searchType);
					try {
						String escaped_search = URLEncoder.encode(searchField.getText(), "UTF-8");
						System.out.println("q="+escaped_search);
						String order = searchOrder.getSelectionModel().getSelectedItem().toLowerCase();
						String type = "";
						if(searchType.getSelectionModel().getSelectedIndex() != 0) type = searchType.getSelectionModel().getSelectedItem().toLowerCase();
						int method = searchMethod.getSelectionModel().getSelectedIndex();
						String location = locField.getText();
						String distance = locDistance.getSelectionModel().getSelectedItem();
						String token = "";
						if(o.equals(search)) {
							Platform.runLater(() -> {
								searchResults.getChildren().clear();
							});
						} else {
							token = pageToken;
						}
						SearchList sl;
						if(method == 0) {
							sl = data.getSearch(escaped_search, 25, token, order, type);
						} else {
							sl = data.getSearchVideosAtLocation(escaped_search, 25, token, order, location, distance);
						}
						SearchResult last = null;
						for(SearchList.Item item : sl.items) {
							final SearchResult result = new SearchResult(last != null ? last : searchResults, item);
							Platform.runLater(() -> {
								searchResults.getChildren().add(result);
								setNodesDisabled(false, selectAll, clearResults, addToGroup);
								resultStatus.setText("Showing "+searchResults.getChildren().size()+" out of "+sl.pageInfo.totalResults+" results.");
							});
							last = result;
						}
						if(sl.nextPageToken != null) {
							setNodesDisabled(false, nextPage);
							pageToken = sl.nextPageToken;
						}
					} catch (JsonSyntaxException | IOException e) {
						Platform.runLater(() -> {
							resultStatus.setText(e.getMessage());
						});
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
			Dialog<ButtonType> dialog = new Dialog<>();
			DialogPane pane = new DialogPane();
			dialog.setDialogPane(pane);
			pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
			
			GridPane grid = new GridPane();
			grid.setPadding(new Insets(10,10,10,10));
			grid.setAlignment(Pos.CENTER);
			
			ToggleGroup toggle = new ToggleGroup();
			RadioButton existing = new RadioButton("Existing group.");
			existing.setToggleGroup(toggle);
			RadioButton newGroup = new RadioButton("Make new group.");
			newGroup.setToggleGroup(toggle);
			
			grid.add(existing, 0, 0);
			
			ChoiceBox<Group> groupList = new ChoiceBox<>();
			groupList.getItems().addAll(choice.getItems());
			grid.add(groupList, 0, 1);
			grid.add(newGroup, 0, 2);
			
			TextField field = new TextField();
			field.setPromptText("Choose a unique name.");
			grid.add(field, 0, 3);
			
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
			
			List<GroupItem> items = new ArrayList<GroupItem>();
			searchResults.getChildren().stream().filter(object -> object instanceof SearchResult && ((SearchResult) object).isSelected()).forEach(result -> {
				SearchResult sr = (SearchResult) result;
				items.add(new GroupItem(sr.type_id, sr.type, sr.youtubeId, sr.title.getText(), sr.author.getText(), sr.publishedAt, new Date(0), sr.thumbUrl.toString(), true));
			});
			
			pane.setContent(grid);
			dialog.setTitle("Add Items to Group ("+items.size()+")");
			dialog.showAndWait().ifPresent(choice -> {
				if(choice == ButtonType.OK) {
					String group_name;
					try {
						if(newGroup.isSelected()) {
							boolean unique = true;
							group_name = field.getText();
							for(Group g : groupList.getItems()) if(g.group_name.equals(group_name)) {
								unique = false;
								break;
							}
							if(unique) {
								db.createGroup(group_name);
								reloadGroups();
							}
						} else {
							group_name = groupList.getSelectionModel().getSelectedItem().group_name;
						}
						db.insertGroupItems(group_name, items);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
			
		} else if(o.equals(createGroup) || o.equals(deleteGroup) || o.equals(renameGroup) || o.equals(refreshGroup) || o.equals(reloadGroup)) {
			if(o.equals(createGroup)) {
				List<Group> allGroups = choice.getItems();
				TextInputDialog input = new TextInputDialog("");
				input.setTitle("Create Group");
				input.setContentText("Pick a unique name: ");
				input.showAndWait().ifPresent(result -> {
					boolean unique = true;
					if(result != null) {
						for(Group g : allGroups) {
							if(g.group_name.equals(result))
								unique = false;
						}
						if(unique) {
							try {
								db.createGroup(result);
								reloadGroups();
							} catch (SQLException e) {}
						}
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
									db.deleteGroup(current.group_name);
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
				input.setContentText("Pick a new name: ");
				input.showAndWait().ifPresent(result -> {
					if(result != null && !current.group_name.equals(result)) {
						try {
							db.editGroupName(current.group_name, result);
							reloadGroups();
						} catch (SQLException e) {}
					}
				});
			} else if(o.equals(refreshGroup)) {
				Task<Void> task = new Task<Void>() {
					protected Void call() throws Exception {
						if(manager != null) {
							Platform.runLater(() -> {
								try {
									manager.refresh();
									setNodesDisabled(true, deleteGroup, renameGroup, refreshGroup, reloadGroup);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							});
						}
						return null;
					}
				};
				new Thread(task).start();
			} else if(o.equals(reloadGroup)) {
				Platform.runLater(() -> {
					reloadGroup.setDisable(true);
					manager.reloadTables();
					reloadGroup.setDisable(false);
				});
				
			}
		} else if(o.equals(cleanDB) || o.equals(resetDB)) {
			if(o.equals(cleanDB)) {
				Task<Void> task = new Task<Void>() {
					protected Void call() throws Exception {
						setNodesDisabled(true, choice, createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB, videoToggle, commentToggle);
						try {
							db.clean();
						} catch (SQLException e) {
							e.printStackTrace();
						}
						setNodesDisabled(false, choice, createGroup, cleanDB, resetDB, videoToggle, commentToggle);
						if(manager != null) {
							setupWithManager(manager);
						}
						return null;
					}
				};
				Thread thread = new Thread(task);
				thread.setDaemon(true);
				thread.start();
			} else if(o.equals(resetDB)) {
				Dialog<ButtonType> dialog = new Dialog<>();
				dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
				dialog.setContentText("Reseting the database will delete everything. Are you sure?");
				dialog.showAndWait().ifPresent(result -> {
					if(result == ButtonType.OK) {
						Task<Void> task = new Task<Void>(){
							protected Void call() throws Exception {
								setNodesDisabled(true, choice, createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB, videoToggle, commentToggle);
								try {
									db.dropAllTables();
									try {
										db.create();
									} catch (ClassNotFoundException e) {}
									db.clean();
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
								setNodesDisabled(false, choice, createGroup, cleanDB, resetDB, videoToggle, commentToggle);
								if(manager != null) {
									setupWithManager(manager);
								}
								return null;
							}
						};
						Thread thread = new Thread(task);
						thread.setDaemon(true);
					}
				});
			}
		}
	}
	
	public void start(Stage arg0) throws Exception {
		app = this;
		stage = arg0;
		
		db.create();
		
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
		Task<Void> task = new Task<Void>(){
			protected Void call() throws IOException {
				loadConfig();
				try {
					reloadGroups();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
		
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
	
	// TODO 
	public HBox createCommentsPane() {
		HBox grid = new HBox();
		grid.setAlignment(Pos.TOP_LEFT);
		
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
		
		description = new TextArea("Published Nov 18, 1918  This is an example description. You may select this text, the title, and author's name. Right click to copy or select all."
				+ "\n\nThe thumbnail and author's picture are clickable to open either the video or channel in your browser."
				+ "\n\nComments may be replied to if you are signed in. Commentor names may be clicked to open their channel in browser.");
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
		
		resultCount = new Label("Showing 0 out of 0 results.");
		
		backToResults = new Button("Return to Results");
		backToResults.setStyle("-fx-base: seagreen");
		backToResults.setDisable(true);
		backToResults.setOnAction(e -> {
			returnToResults();
		});
		
		resultControls.getChildren().addAll(clearComments, backToResults, resultCount);
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
		
		cgroup = new ChoiceBox<Group>();
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
		citem = new ChoiceBox<GroupItem>();
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
		
		type = new ComboBox<String>();
		type.setMaxWidth(Double.MAX_VALUE);
		type.getItems().addAll("Comments and Replies", "Comments Only", "Replies Only");
		type.getSelectionModel().select(0);
		
		Label label3 = new Label("Sort by ");
		label3.setAlignment(Pos.CENTER_RIGHT);
		
		orderby = new ComboBox<String>();
		orderby.setMaxWidth(Double.MAX_VALUE);
		orderby.getItems().addAll("Most Recent", "Least Recent", "Most Likes", "Most Replies", "Longest Comment", "Names (A to Z)", "Comments (A to Z)");
		orderby.getSelectionModel().select(0);
		
		HBox orderbox = new HBox(5);
		orderbox.setAlignment(Pos.CENTER_LEFT);
		orderbox.getChildren().addAll(label3, orderby);
		
		userLike = new TextField();
		userLike.setMaxWidth(Double.MAX_VALUE);
		userLike.setPromptText("Username contains...");
		
		textLike = new TextField();
		textLike.setMaxWidth(Double.MAX_VALUE);
		textLike.setPromptText("Comment contains...");
		
		find = new Button("Find Comments");
		find.setMaxWidth(Double.MAX_VALUE);
		find.setOnAction(e -> {
			Task<Void> task = new Task<Void>() {
				protected Void call() throws Exception {
					try {
						find.setDisable(true);
						String group_name = cgroup.getValue().group_name;
						int order = orderby.getSelectionModel().getSelectedIndex();
						String user = userLike.getText();
						String text = textLike.getText();
						int limit = 1000;
						GroupItem gitem = citem.getValue().gitem_id != -1 ? citem.getValue() : null;
						int comment_type =  type.getSelectionModel().getSelectedIndex();
						CommentSearch searchResult = db.getComments(group_name, order, user, text, limit, gitem, comment_type);
						final List<CommentResult> list = searchResult.results.stream()
								.map(c -> new CommentResult(c, true))
								.collect(Collectors.toList());
						results = list;
						Platform.runLater(() -> {
							resultCount.setText("Showing "+list.size()+" out of "+searchResult.total_results+" results.");
							commentResults.getChildren().clear();
							commentResults.getChildren().addAll(list);
							find.setDisable(false);
							backToResults.setDisable(true);
							vValue = 0;
							scroll.layout();
							scroll.setVvalue(0.0);
						});
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return null;
				}
			};
			new Thread(task).start();
		});
		
		searchBox.getChildren().addAll(label1, cgroup, citem, videoContext, label2, type, orderbox, userLike, textLike, find);
		grid.getChildren().addAll(context, resultBox, searchBox);
		HBox.setHgrow(resultBox, Priority.ALWAYS);
		
		return grid;
	}
	
	public void returnToResults() {
			backToResults.setDisable(true);
			commentResults.getChildren().clear();
			commentResults.getChildren().addAll(results);
			cscroll.layout();
			cscroll.setVvalue(vValue);
	}
	
	public void viewTree(Comment comment) throws SQLException {
		vValue = cscroll.getVvalue();
		final List<CommentResult> list = db.getCommentTree(comment.is_reply ? comment.parent_id : comment.comment_id).stream()
				.map(c -> new CommentResult(c, false))
				.collect(Collectors.toList());
			commentResults.getChildren().clear();
			commentResults.getChildren().addAll(list);
			find.setDisable(false);
			backToResults.setDisable(false);
	}
	
	public void loadContext(String videoId) throws SQLException, ParseException {
		Video video = db.getVideo(videoId, true);
		thumbnail.setImage(SwingFXUtils.toFXImage(video.buffered_thumb, null));
		thumbnail.setCursor(Cursor.HAND);
		thumbnail.setOnMouseClicked(e -> {
			if(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 1) {
				openInBrowser(video.getYoutubeLink());
			}
		});
		authorThumb.setImage(video.channel.buffered_profile != null ? SwingFXUtils.toFXImage(video.channel.buffered_profile,null) : CommentResult.BLANK_PROFILE);
		authorThumb.setCursor(Cursor.HAND);
		authorThumb.setOnMouseClicked(e -> {
			if(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 1) {
				openInBrowser(video.channel.getYoutubeLink());
			}
		});
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
		description.setText("Published on "+sdf.format(video.publish_date)+"  "+video.video_desc);
		title.setText(video.video_title);
		author.setText(video.channel.channel_name);
		likes.setText("+"+video.total_likes);
		dislikes.setText("-"+video.total_dislikes);
		views.setText(video.total_views+" views");
	}
	
	public void loadCGroup(Group g) throws SQLException {
		final List<GroupItem> items = db.getGroupItems(g.group_name, false);
		Platform.runLater(() -> {
			citem.getItems().clear();
			citem.getItems().add(new GroupItem(-1, "All Items ("+items.size()+")"));
			citem.getItems().addAll(items);
			citem.getSelectionModel().select(0);
			// Not smart to load all relevant videos into another ChoiceBox, too slow and list has potential to be gigantic. 
		});
	}
	
	public GridPane createGroupsPane() {
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
					manager = new GroupManager(group, db, data);
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
	
	public void reloadGroups() throws SQLException {
		manager = null;
		int selected = choice.getSelectionModel().getSelectedIndex();
		List<Group> groups = db.getGroups();
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
	
	public GridPane createVideosPane() {
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
		
		searchOrder = new ComboBox<>();
		searchOrder.getItems().addAll("Relevance", "Date", "Title", "Rating", "Views");
		searchOrder.getSelectionModel().select(0);
		grid.add(searchOrder, 3, 0);
		
		searchType = new ComboBox<>();
		searchType.getItems().addAll("All Types", "Video", "Channel", "Playlist", "Movie", "Show");
		searchType.getSelectionModel().select(0);
		grid.add(searchType, 4, 0);
		
		grid.add(grid2, 0, 1, 5, 1);
		
		searchResults = new VBox();
		searchResults.setAlignment(Pos.TOP_CENTER);
		
		scroll = new ScrollPane(searchResults);
		scroll.setFitToWidth(true);
		scroll.setFitToHeight(true);
		grid2.add(scroll, 0, 0);
		GridPane.setHgrow(scroll, Priority.ALWAYS);
		
		HBox hbox = new HBox(5);
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
		addToGroup.setDisable(true);
		addToGroup.setOnAction(this);
		
		nextPage = new Button("Next Page");
		nextPage.setTooltip(new Tooltip("Get the next page of results."));
		nextPage.setDisable(true);
		nextPage.setOnAction(this);
		
		resultStatus = new Label();
		hbox.setAlignment(Pos.CENTER);
		hbox.getChildren().addAll(selectAll, clearResults, addToGroup, nextPage, resultStatus);
		grid2.add(hbox, 0, 1);
		
		return grid;
	}
	
	public GridPane createMenuPane() {
		int height = 26;
		GridPane grid  = new GridPane();
		grid.setId("menuBar");
		grid.setMaxWidth(Double.MAX_VALUE);
		grid.setMaxHeight(height);
		
		StackPane img = new StackPane();
		header = new ImageView(new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuite/images/icon.png")));
		header.setFitHeight(height);
		header.setFitWidth(height * header.getImage().getWidth() / header.getImage().getHeight());
		img.getChildren().add(header);
		img.setPadding(new Insets(0,15,0,15));
		grid.add(img, 0, 0);
		
		ToggleGroup toggle = new ToggleGroup();
		toggle.getToggles().addListener(new ListChangeListener<Toggle>() {
			public void onChanged(Change<? extends Toggle> c) {
				while(c.next()) {
					for(final Toggle addedToggle : c.getAddedSubList()) {
						((ToggleButton) addedToggle).addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
							public void handle(MouseEvent mouseEvent) {
								if (addedToggle.equals(toggle.getSelectedToggle()))
									mouseEvent.consume();
							}
						});
					}
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
	
	public StackPane createSetupPane() {
		String gradient = "-fx-background-color: linear-gradient(rgba(200,200,200,0.7), rgba(220,220,220,0.95), rgba(200,200,200,0.99), rgba(220,220,220,1)) ";
		StackPane glass = new StackPane();
		glass.setStyle(gradient); 
		glass.setMaxHeight(Double.MAX_VALUE);
		glass.setMaxWidth(Double.MAX_VALUE);
		glass.setAlignment(Pos.CENTER);
		
		GridPane form = new GridPane();
		form.setAlignment(Pos.CENTER);
		form.setHgap(10);
		form.setVgap(10);
		form.setPadding(new Insets(15,15,15,15));
		
		Text text = new Text("Youtube Setup");
		text.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
		form.add(text, 0, 0, 2, 1);
		
		Label label1 = new Label("Data API Key");
		form.add(label1, 0, 1);
		
		keyField = new PasswordField();
		keyField.setPromptText("Paste your Youtube Data API key here.");
		keyField.setTooltip(new Tooltip("This key is required for everything to function. Do not clear this field."));
		keyField.setId("form-plain");
		keyField.setDisable(true);
		form.add(keyField, 1, 1, 3, 1);
		
		Text text2 = new Text("Youtube Login (OAuth2)");
		text2.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
		form.add(text2, 0, 3, 2, 1);
		
		signin = new Button("Sign in");
		signin.setOnAction(this);
		form.add(signin, 0, 4);
		
		status = new Label("Sign in to leave comments and replies.");
		form.add(status, 1, 4, 3, 1);
		
		HBox hBtn = new HBox(10);
		saveAndSetup = new Button("Save and Setup");
		saveAndSetup.setOnAction(this);
		exitSetup = new Button("Close");
		exitSetup.setOnAction(this);
		hBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hBtn.getChildren().addAll(saveAndSetup, exitSetup);
		
		form.add(hBtn, 3, 6);
		
		glass.getChildren().add(form);
		return glass;
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
