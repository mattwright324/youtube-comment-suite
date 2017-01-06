package mattw.youtube.commensuitefx;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.JsonSyntaxException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

import mattw.youtube.commentsuite.*;
import mattw.youtube.datav3.YoutubeData;
import mattw.youtube.datav3.list.ChannelsList;
import mattw.youtube.datav3.list.SearchList;

public class CommentSuiteFX extends Application implements EventHandler<ActionEvent> {
	
	public static CommentSuiteFX app;
	
	public YoutubeData data;
	
	public YCSConfig config = new YCSConfig();
	public SuiteDatabase db = new SuiteDatabase("commentsuite.db");
	public Stage stage;
	
	public StackPane layout, setup;
	public GridPane main, menu, videos, groups, comments;
	
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
				config.setAccessTokens(new_tokens);
				data.setAccessToken(new_tokens.access_token);
				config.save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void signOut() throws IOException {
		config.setUsername("Guest");
		data.setAccessToken("");
		config.setAccessTokens(null);
		config.save();
	}
	
	public void handle(ActionEvent arg0) {
		Object o = arg0.getSource();
		if(o.equals(saveAndSetup) || o.equals(exitSetup) || o.equals(signin)) {
			if(o.equals(saveAndSetup)) {
				Task<Void> task = new Task<Void>(){
					protected Void call() throws Exception {
						saveAndSetup.setDisable(true);
						keyField.setDisable(true);
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
									dialog.getDialogPane().setContent(web);
									dialog.titleProperty().bind(engine.titleProperty());
									engine.titleProperty().addListener(e -> {
										System.out.println("CHANGE: "+engine.getTitle());
										if(engine.getTitle() != null && (engine.getTitle().contains("code=") || engine.getTitle().contains("error="))) {
											String response = engine.getTitle();
											String code = response.substring(13, response.length());
											dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
											web.setDisable(true);
											try {
												OA2Tokens tokens = OA2Handler.getAccessTokens(code);
												data.setAccessToken(tokens.access_token);
												ChannelsList cl = data.getChannelsByMine(ChannelsList.PART_SNIPPET);
												String title = cl.items[0].snippet.title;
												config.setUsername(title);
												config.setAccessTokens(tokens);
												config.save();
												loadConfig();
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
					search.setDisable(true);
					nextPage.setDisable(true);
					searchField.setDisable(true);
					searchMethod.setDisable(true);
					locField.setDisable(true);
					locDistance.setDisable(true);
					searchOrder.setDisable(true);
					searchType.setDisable(true);
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
								selectAll.setDisable(false);
								clearResults.setDisable(false);
								addToGroup.setDisable(false);
								resultStatus.setText("Showing "+searchResults.getChildren().size()+" out of "+sl.pageInfo.totalResults+" results.");
							});
							last = result;
						}
						if(sl.nextPageToken != null) {
							nextPage.setDisable(false);
							pageToken = sl.nextPageToken;
						}
					} catch (JsonSyntaxException | IOException e) {
						Platform.runLater(() -> {
							resultStatus.setText(e.getMessage());
						});
						e.printStackTrace();
					}
					search.setDisable(false);
					searchField.setDisable(false);
					searchMethod.setDisable(false);
					locField.setDisable(false);
					locDistance.setDisable(false);
					searchOrder.setDisable(false);
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
			selectAll.setDisable(true);
			clearResults.setDisable(true);
			addToGroup.setDisable(true);
			nextPage.setDisable(true);
			resultStatus.setText("");
			searchResults.getChildren().clear();
		} else if(o.equals(addToGroup)) { // TODO
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
			
		} else if(o.equals(createGroup) || o.equals(deleteGroup) || o.equals(renameGroup) || o.equals(refreshGroup) || o.equals(reloadGroup)) { // TODO
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
								choice.setDisable(true);
								createGroup.setDisable(true);
								deleteGroup.setDisable(true);
								renameGroup.setDisable(true);
								refreshGroup.setDisable(true);
								reloadGroup.setDisable(true);
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
								createGroup.setDisable(false);
								deleteGroup.setDisable(false);
								renameGroup.setDisable(false);
								refreshGroup.setDisable(false);
								reloadGroup.setDisable(false);
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
									deleteGroup.setDisable(true);
									renameGroup.setDisable(true);
									refreshGroup.setDisable(true);
									reloadGroup.setDisable(true);
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
		}
	}
	
	public void start(Stage arg0) throws Exception {
		app = this;
		stage = arg0;
		
		db.create();
		
		/*Dialog<Void> dialog = new Dialog<>();
		DialogPane pane = dialog.getDialogPane();
		pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		GridPane grid = new GridPane();
		pane.getChildren().add(grid);
		grid.setAlignment(Pos.CENTER);
		
		dialog.showAndWait();*/
		
		layout = new StackPane();
		setup = createSetupPane();
		menu = createMenuPane();
		videos = createVideosPane();
		groups = createGroupsPane();
		comments = createCommentsPane();
		
		main = new GridPane();
		main.setMaxHeight(Double.MAX_VALUE);
		main.setMaxWidth(Double.MAX_VALUE);
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
	
	public GridPane createCommentsPane() {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_CENTER);
		grid.setHgap(5);
		grid.setVgap(5);
		
		
		
		return grid;
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
		
		menu.getChildren().addAll(label, choice, createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup);
		
		return grid;
	}
	
	public void setupWithManager(GroupManager gm) {
		boolean disable = manager.isRefreshing();
		deleteGroup.setDisable(disable);
		renameGroup.setDisable(disable);
		refreshGroup.setDisable(disable);
		reloadGroup.setDisable(disable);
	}
	
	public void reloadGroups() throws SQLException {
		manager = null;
		int selected = choice.getSelectionModel().getSelectedIndex();
		choice.getItems().clear();
		choice.getItems().addAll(db.getGroups());
		choice.getSelectionModel().select(selected);
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
