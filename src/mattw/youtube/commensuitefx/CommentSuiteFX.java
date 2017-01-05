package mattw.youtube.commensuitefx;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
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
import mattw.youtube.datav3.YoutubeData;
import mattw.youtube.datav3.list.ChannelsList;
import mattw.youtube.datav3.list.CommentThreadsList;
import mattw.youtube.datav3.list.CommentsList;
import mattw.youtube.datav3.list.SearchList;

public class CommentSuiteFX extends Application implements EventHandler<ActionEvent> {
	
	public YoutubeData data;
	
	public YCSConfig config = new YCSConfig();
	public Stage stage;
	
	public StackPane layout, setup;
	public GridPane main, menu, videos, groups, comments;
	
	public ToggleButton videoToggle, groupToggle, commentToggle;
	public Label welcome;
	public Hyperlink gotoSetup;
	
	public Button saveAndSetup, exitSetup;
	public Hyperlink googleGuide;
	public Button signin;
	public TextField status;
	public PasswordField keyField;
	
	public String pageToken = "";
	public Button search, selectAll, clearResults, addToGroup, nextPage;
	public TextField searchField, locField;
	public ComboBox<String> searchMethod, searchOrder, searchType, locDistance;
	public ScrollPane scroll;
	public VBox searchResults;
	public Label resultStatus;
	
	public static void main(String[] args) {
		launch(args);
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
	
	public CommentThreadsList.Item postComment(String channelId, String videoId, String textOriginal) throws IOException {
		System.out.println("Commenting on ["+videoId+", "+channelId+"]\n    "+textOriginal);
		String payload = new Gson().toJson(new MakeComment(channelId, videoId, textOriginal), MakeComment.class);
		HttpURLConnection url = (HttpURLConnection) new URL("https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&access_token="+data.access_token+"&key="+data.data_api_key).openConnection();
		url.setDoOutput(true);
		url.setDoInput(true);
		url.setRequestProperty("Content-Type", "application/json");
		OutputStream os = url.getOutputStream();
		os.write(payload.getBytes("UTF-8"));
		String response = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(url.getInputStream()));
			String line;
			while((line = br.readLine()) != null) {response+=line;}
		} catch (IOException e) {
			BufferedReader br = new BufferedReader(new InputStreamReader(url.getErrorStream()));
			String line;
			while((line = br.readLine()) != null) {response+=line;}
		}
		System.out.println("["+response+"]");
		return data.gson.fromJson(response, CommentThreadsList.Item.class);
	}
	
	public CommentsList.Item postReply(String parentId, String textOriginal) throws IOException {
		System.out.println("Replying to ["+parentId+"]\n    "+textOriginal);
		String payload = new Gson().toJson(new MakeReply(parentId, textOriginal), MakeReply.class);
		HttpURLConnection url = (HttpURLConnection) new URL("https://www.googleapis.com/youtube/v3/comments?part=snippet&access_token="+data.access_token+"&key="+data.data_api_key).openConnection();
		url.setDoOutput(true);
		url.setDoInput(true);
		url.setRequestProperty("Content-Type", "application/json");
		OutputStream os = url.getOutputStream();
		os.write(payload.getBytes("UTF-8"));
		String response = null;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(url.getInputStream()));
			String line;
			while((line = br.readLine()) != null) {response+=line;}
		} catch (IOException e) {
			BufferedReader br = new BufferedReader(new InputStreamReader(url.getErrorStream()));
			String line;
			while((line = br.readLine()) != null) {response+=line;}
		}
		System.out.println("["+response+"]");
		return data.gson.fromJson(response, CommentsList.Item.class);
	}
	
	public class MakeReply {
		public MakeReply(String parentId, String textOriginal) {
			snippet = new Snippet();
			snippet.parentId = parentId;
			snippet.textOriginal = textOriginal;
		}
		public Snippet snippet;
		public class Snippet {
			public String parentId;
			public String textOriginal;
		}
	}
	
	public class MakeComment {
		public MakeComment(String channel_id, String textOriginal) {
			snippet.channelId = channel_id;
			snippet.topLevelComment.snippet.textOriginal = textOriginal;
		}
		public MakeComment(String channel_id, String videoId, String textOriginal) {
			this(channel_id, textOriginal);
			snippet.videoId = videoId;
		}
		public Snippet snippet = new Snippet();
		public class Snippet {
			public String channelId;
			public String videoId;
			public TopLevelComment topLevelComment = new TopLevelComment();
			public class TopLevelComment {
				public TLCSnippet snippet = new TLCSnippet();
				public class TLCSnippet {
					public String textOriginal;
				}
			}
		}
		
	}
	
	public void handle(ActionEvent arg0) {
		Object o = arg0.getSource();
		if(o.equals(saveAndSetup) || o.equals(exitSetup) || o.equals(signin)) {
			if(o.equals(saveAndSetup)) {
				Task<Void> task = new Task<Void>(){
					protected Void call() throws Exception {
						saveAndSetup.setDisable(true);
						keyField.setDisable(true);
						Platform.runLater(() -> {
							if(layout.getChildren().contains(setup)) {
								config.setYoutubeKey(keyField.getText());
								try {
									config.save();
									data = new YoutubeData(config.getYoutubeKey());
									welcome.setText("Welcome, "+config.getUsername());
								} catch (IOException e) {
									e.printStackTrace();
								}
								layout.getChildren().remove(setup);
							}
						});
							
						return null;
					}
				};
				Thread thread = new Thread(task);
				thread.setDaemon(true);
				thread.start();
			} else if(o.equals(exitSetup)) {
				layout.getChildren().remove(setup);
			} else if(o.equals(signin)) {
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
										} catch (IOException e1) {
											e1.printStackTrace();
										}
										
										dialog.close();
									} else {
										System.out.println("    NO RESPONSE");
									}
								});
								dialog.showAndWait();
								System.out.println("OAuth2 Done?");
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
			}
		} else if(o.equals(videoToggle) || o.equals(groupToggle) || o.equals(commentToggle)) {
			if(videoToggle.isSelected()) {
				if(main.getChildren().contains(groups)) main.getChildren().remove(groups);
				if(main.getChildren().contains(comments)) main.getChildren().remove(comments);
				if(!main.getChildren().contains(videos)) {
					main.add(videos, 0, 1);
				}
			} else if(groupToggle.isSelected()) {
				if(main.getChildren().contains(videos)) main.getChildren().remove(videos);
				if(main.getChildren().contains(comments)) main.getChildren().remove(comments);
				if(!main.getChildren().contains(groups)) {
					main.add(groups, 0, 1);
				}
			} else if(commentToggle.isSelected()) {
				if(main.getChildren().contains(videos)) main.getChildren().remove(videos);
				if(main.getChildren().contains(groups)) main.getChildren().remove(groups);
				if(!main.getChildren().contains(comments)) {
					main.add(comments, 0, 1);
				}
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
						for(SearchList.Item item : sl.items) { // TODO
							final SearchResult result = new SearchResult(last != null ? last : searchResults, item);
							Platform.runLater(() -> {
								searchResults.getChildren().add(result);
								selectAll.setDisable(false);
								clearResults.setDisable(false);
								addToGroup.setDisable(false);
								resultStatus.setText("Showing "+(searchResults.getChildren().size()+1)+" out of "+sl.pageInfo.totalResults+" results.");
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
		} else if(o.equals(addToGroup)) {
			
		}
	}
	
	public void start(Stage arg0) throws Exception {
		stage = arg0;
		// TODO 
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
				config.load();
				Platform.runLater(() -> {
					keyField.setText(config.getYoutubeKey());
					welcome.setText("Welcome, "+config.getUsername());
					if(config.getAccessTokens() != null) {
						data.setAccessToken(config.getAccessTokens().access_token);
					}
				});
				data = new YoutubeData(config.getYoutubeKey());
				if(!config.isSetup()) {
					Platform.runLater(() -> {
						layout.getChildren().add(setup);
					});
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
		grid.setGridLinesVisible(true);
		
		return grid;
	}
	
	public GridPane createGroupsPane() {
		GridPane grid = new GridPane();
		grid.setGridLinesVisible(true);
		
		
		
		return grid;
	}
	
	public GridPane createVideosPane() {
		GridPane grid = new GridPane(); // Search options.
		grid.setPadding(new Insets(5,5,5,5));
		grid.setId("videoPane");
		grid.setAlignment(Pos.CENTER);
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
		ImageView header = new ImageView(new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuite/images/icon.png")));
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
				keyField.setDisable(false);
				saveAndSetup.setDisable(false);
				layout.getChildren().add(setup);
				try {
					postComment("UCUcyEsEjhPEDf69RRVhRh4A", "04r5XVEgn0Q", "Test post! http://www.google.com");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		grid.add(gotoSetup, 5, 0);
		
		GridPane.setValignment(welcome, VPos.CENTER);
		GridPane.setHgrow(welcome, Priority.ALWAYS);
		
		return grid;
	}
	
	public StackPane createSetupPane() {
		String gradient = "-fx-background-color: linear-gradient(rgba(160,160,160,0.7), rgba(160,160,160,0.95), rgba(160,160,160,0.99), rgba(220,220,220,1)) ";
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
		
		googleGuide = new Hyperlink();
		googleGuide.setText("How do I get my own key?");
		googleGuide.setOnAction(e -> {
			openInBrowser("https://developers.google.com/youtube/v3/getting-started");
		});
		form.add(googleGuide, 0, 2, 4, 1);
		GridPane.setHalignment(googleGuide, HPos.CENTER);
		
		Text text2 = new Text("Youtube Login (OAuth2)");
		text2.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
		form.add(text2, 0, 3, 2, 1);
		
		signin = new Button("Sign in");
		signin.setOnAction(this);
		form.add(signin, 0, 4);
		
		status = new TextField("Sign in to leave comments and replies.");
		status.setEditable(false);
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
