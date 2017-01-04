package mattw.youtube.commensuitefx;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;

import javax.imageio.ImageIO;

import com.google.gson.JsonSyntaxException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import mattw.youtube.datav3.YoutubeData;
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
	public TextField userField;
	public PasswordField keyField, passField;
	
	public String pageToken = "";
	public Button search, selectAll, clearResults, addToGroup, nextPage;
	public TextField searchField, locField;
	public ComboBox<String> searchMethod, searchOrder, searchType, locDistance;
	public ScrollPane scroll;
	public VBox searchResults;
	public Label resultStatus;
	
	
	public class SearchResult extends AnchorPane {
		
		final Label author, title, description, published;
		private CheckBox select;
		
		final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
		final URL thumbUrl;
		final String youtubeId;
		final int type_id;
		final String type;
		
		
		public void setSelected(boolean s) {
			select.setSelected(s);
			if(s) {
				setStyle("-fx-background-color: radial-gradient(radius 85%, rgba(220,220,220,0), rgba(200,80,80,0.5), rgba(220,220,220,0.1))");
			} else {
				setStyle("");
			}
		}
		
		public boolean getSelected() {
			return select.isSelected();
		}
		
		public SearchResult(Node topAnchor, SearchList.Item item) {
			super();
			int width = 650;
			int height = 120;
			setMaxWidth(width);
			setPrefWidth(width);
			setMinWidth(width);
			setMaxHeight(height);
			setPrefHeight(height);
			setMinHeight(height);
			setTopAnchor(topAnchor, 10d);
			
			thumbUrl = item.snippet.thumbnails.medium.url;
			author = new Label(item.snippet.channelTitle);
			description = new Label("Published on "+sdf.format(item.snippet.publishedAt)+"  "+item.snippet.description);
			published = new Label();
			title = new Label(item.snippet.title);
			if(item.id.videoId != null) {
				type_id = 0;
				type = "Video";
				youtubeId = item.id.videoId;
			} else if(item.id.channelId != null) {
				type_id = 1;
				type = "Channel";
				youtubeId = item.id.channelId;
			} else if(item.id.playlistId != null) {
				type_id = 2;
				type = "Playlist";
				youtubeId = item.id.playlistId;
			} else {
				type = "Error";
				type_id = -1;
				youtubeId = "";
			}
			
			ContextMenu context = new ContextMenu();
			MenuItem open = new MenuItem("Open in Browser");
			open.setOnAction(e -> {
				String url = "";
				if(type_id == 0) url = "http://youtu.be/"+youtubeId;
				if(type_id == 1) url = "http://www.youtube.com/channel/"+youtubeId;
				if(type_id == 2) url = "http://www.youtube.com/playlist?list="+youtubeId;
				openInBrowser(url);
			});
			context.getItems().addAll(open);
			setOnMouseClicked(e -> {
				if(e.isPopupTrigger()) {
					context.show(stage, e.getScreenX(), e.getScreenY());
				}
				setSelected(!select.isSelected());
			});
			
			GridPane grid = new GridPane();
			getChildren().add(grid);
			grid.setAlignment(Pos.CENTER_RIGHT);
			grid.setVgap(5);
			grid.setHgap(5);
			
			GridPane side = new GridPane();
			side.setAlignment(Pos.CENTER);
			side.setMaxWidth(160);
			side.setMinWidth(160);
			side.setHgap(5);
			side.setVgap(5);
			grid.add(side, 0, 0);
			
			select = new CheckBox();
			select.setOnAction(e -> {
				setSelected(select.isSelected());
			});
			select.setMaxHeight(Double.MAX_VALUE);
			side.add(select, 0, 0);
			GridPane.setHgrow(select, Priority.ALWAYS);
			
			Label label = new Label(type);
			label.setMaxWidth(Double.MAX_VALUE);
			label.setAlignment(Pos.CENTER);
			side.add(label, 0, 1, 2, 1);
			label.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 14));
			GridPane.setHgrow(label, Priority.ALWAYS);
			
			StackPane stack = new StackPane();
			ImageView img = new ImageView();
			try {
				BufferedImage bi = ImageIO.read(thumbUrl);
				img.setImage(SwingFXUtils.toFXImage(bi, null));
				img.setFitHeight(80);
				img.setFitWidth(80 * img.getImage().getWidth() / img.getImage().getHeight());
			} catch (IOException e) {}
			stack.getChildren().add(img);
			side.add(stack, 1, 0);
			GridPane.setVgrow(stack, Priority.ALWAYS);
			GridPane.setHgrow(stack, Priority.ALWAYS);
			
			GridPane center = new GridPane();
			center.setHgap(5);
			center.setVgap(5);
			grid.add(center, 1, 0);
			center.add(title, 1, 0);
			title.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
			title.setMaxWidth(550);
			center.add(author, 1, 1);
			author.setMaxWidth(550);
			center.add(description, 1, 2);
			description.setWrapText(true);
			description.setMaxWidth(550);
			description.setMaxHeight(50);
			GridPane.setFillWidth(description, true);
			GridPane.setFillHeight(description, true);
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public void handle(ActionEvent arg0) {
		Object o = arg0.getSource();
		if(o.equals(saveAndSetup) || o.equals(exitSetup)) {
			if(o.equals(saveAndSetup)) {
				Task<Void> task = new Task<Void>(){
					protected Void call() throws Exception {
						saveAndSetup.setDisable(true);
						keyField.setDisable(true);
						userField.setDisable(true);
						passField.setDisable(true);
						Platform.runLater(() -> {
							if(layout.getChildren().contains(setup)) {
								config.setYoutubeKey(keyField.getText());
								config.setUsername(userField.getText());
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
					if(!r.select.isSelected()) {
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
					userField.setText(config.getUsername());
					welcome.setText("Welcome, "+config.getUsername());
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
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuite/images/icon.png")));
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
			}
		});
		grid.add(gotoSetup, 5, 0);
		
		GridPane.setValignment(welcome, VPos.CENTER);
		GridPane.setHgrow(welcome, Priority.ALWAYS);
		
		return grid;
	}
	
	public StackPane createSetupPane() {
		String gradient = "-fx-background-color: linear-gradient(rgba(160,160,160,0.7), rgba(80,80,80,0.95), rgba(80,80,80,0.95), rgba(220,220,220,0.7)) ";
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
		
		Label label2 = new Label("Username");
		form.add(label2, 0, 4);
		
		userField = new TextField();
		userField.setDisable(true);
		userField.setPromptText("Your username here.");
		userField.setId("form-plain");
		form.add(userField, 1, 4, 3, 1);
		
		Label label3 = new Label("Password");
		form.add(label3, 0, 5);
		
		passField = new PasswordField();
		passField.setDisable(true);
		passField.setPromptText("Your password here.");
		passField.setId("form-plain");
		form.add(passField, 1, 5, 3, 1);
		
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

}
