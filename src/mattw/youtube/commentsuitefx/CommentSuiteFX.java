package mattw.youtube.commentsuitefx;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import mattw.youtube.datav3.YouTubeData3;

public class CommentSuiteFX extends Application {

	private final YCSConfig config = new YCSConfig();
	private YouTubeData3 data;
	private DatabaseManager database;
	private static CommentSuiteFX instance;

	public static final Image PLACEHOLDER = new Image(CommentResult.class.getResourceAsStream("/mattw/youtube/commentsuitefx/images/placeholder4.png"));
	public final ObservableList<Group> groupsList = FXCollections.observableArrayList();
	public final Image YT = new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuitefx/images/youtube.png"));
	public final Image ICON = new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuitefx/images/fxicon.png"));
	public final Image settings = new Image(getClass().getResourceAsStream("/mattw/youtube/commentsuitefx/images/settings.png"));

	private StackPane layout;
	private StackPane setup;
	private VBox main;
	private YoutubeSearchPane videos;
	private GroupManagePane groups;
	private CommentSearchPane comments;

	private ImageView header;
	private Pane lastSetPane;
	private ToggleButton startPage;
	private Label welcome;

	private Button saveSettings;
	private Button addAccount;
	private CheckBox downloadThumbs, showWelcome, prefixReplies;
	private final VBox accountList = new VBox();

	public static void main(String[] args) {
		launch(args);
	}

	public static CommentSuiteFX getApp() { return instance; }
	public static DatabaseManager getDatabase() { return instance.database; }
	public static YCSConfig getConfig() { return instance.config; }
	public static StackPane getMainPane() { return instance.layout; }
	public static YouTubeData3 getYoutube() { return instance.data; }

	public YoutubeSearchPane getYoutubeSearchPane() { return videos; }
	public GroupManagePane getGroupManagePane() { return groups; }
	public CommentSearchPane getCommentSearchPane() { return comments; }

	public static void addOverlay(StackPane stack) {
		if(!instance.layout.getChildren().contains(stack))
			instance.layout.getChildren().add(stack);
	}

	private void applyConfig() throws IOException {
		config.load();
		data = new YouTubeData3(config.getYoutubeKey());
		data.setRequestHeader("Referer", "https://github.com/mattwright324/youtube-data-list");
		Platform.runLater(() -> {
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
		public final Label name = new Label("...");
		public AccountPane(Account acc) {
			super(10);
			setId("account");
			setPadding(new Insets(5,35,5,35));
			setFillHeight(true);
			setAlignment(Pos.CENTER_LEFT);
			setMinWidth(50);
			setPrefWidth(300);
			acc.getUsername();
			name.textProperty().bind(acc.nameProperty);
			name.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 15));
			signOut.setStyle("-fx-base: firebrick");
			signOut.setOnAction(ae -> {
				acc.signOut();
				Platform.runLater(() -> accountList.getChildren().remove(this));
			});
			ImageView iv = new ImageView(new Image(acc.getProfile()));
			iv.setFitHeight(32);
			iv.setFitWidth(32);
			getChildren().addAll(signOut, iv, name);
		}
	}

	public static void setNodesDisabled(boolean disable, Node... nodes) {
		for(Node n : nodes) n.setDisable(disable);
	}

	public void start(Stage stage) throws Exception {
		instance = this;
		try {
			applyConfig();
		} catch (Exception e) { e.printStackTrace(); }

		database = new DatabaseManager();
		database.setup();

		layout = new StackPane();
		setup = createSettingsPane();
		videos = new YoutubeSearchPane();
		groups = new GroupManagePane();
		comments = new CommentSearchPane();
		GridPane menu = createMenuBar();

		main = new VBox();
		main.setAlignment(Pos.TOP_LEFT);
		main.setFillWidth(true);
		main.getChildren().add(menu);

		layout.getChildren().addAll(main);
		startPage.fire();
		reloadGroups();

		Scene scene = new Scene(layout, 900, 550);
		scene.getStylesheets().add(
				getClass().getResource("commentsuitefx.css").toExternalForm()
		);
		stage.setScene(scene);
		stage.setTitle("YouTube Comment Suite");
		stage.getIcons().add(ICON);
		stage.setOnCloseRequest(e -> {
			Platform.exit();
			System.exit(0);
		});
		stage.show();
	}

	public static void reloadGroups() throws SQLException {
		List<Group> groups = instance.database.getGroups();
		instance.groupsList.clear();
		instance.groupsList.addAll(groups);
	}

	private GridPane createMenuBar() {
		int height = 26;
		GridPane grid  = new GridPane();
		grid.setId("menuBar");
		grid.setMaxWidth(Double.MAX_VALUE);
		grid.setMaxHeight(height);
		grid.setPadding(new Insets(0,0,10,0));

		StackPane img = new StackPane();
		header = new ImageView(YT);
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

		Map<ToggleButton,Pane> pages = new HashMap<>();
		EventHandler<ActionEvent> changePage = new EventHandler<ActionEvent>(){
			public void handle(ActionEvent ae) {
				if(lastSetPane != null && main.getChildren().contains(lastSetPane)) main.getChildren().remove(lastSetPane);
				lastSetPane = pages.get(ae.getSource());
				if(lastSetPane != null && !main.getChildren().contains(lastSetPane)) {
					main.getChildren().add(lastSetPane);
					VBox.setVgrow(lastSetPane, Priority.ALWAYS);
				}
			}
		};

		ToggleButton videoToggle = new ToggleButton("Search YouTube");
		videoToggle.setMaxHeight(height);
		videoToggle.setToggleGroup(toggle);
		videoToggle.setId("menuButton");
		videoToggle.setOnAction(changePage);
		pages.put(videoToggle, videos);
		grid.add(videoToggle, 1, 0);

		ToggleButton groupToggle = new ToggleButton("Manage Groups");
		groupToggle.setMaxHeight(height);
		groupToggle.setToggleGroup(toggle);
		groupToggle.setId("menuButton");
		groupToggle.setOnAction(changePage);
		pages.put(groupToggle, groups);
		grid.add(groupToggle, 2, 0);

		ToggleButton commentToggle = new ToggleButton("Search Comments");
		commentToggle.setMaxHeight(height);
		commentToggle.setToggleGroup(toggle);
		commentToggle.setId("menuButton");
		commentToggle.setOnAction(changePage);
		pages.put(commentToggle, comments);
		grid.add(commentToggle, 3, 0);

		startPage = videoToggle;

		welcome = new Label();
		welcome.setFont(Font.font("Tahoma", FontWeight.MEDIUM, 14));
		welcome.setPadding(new Insets(0,15,0,15));
		welcome.setMaxWidth(Double.MAX_VALUE);
		grid.add(welcome, 4, 0);


		ImageView view = new ImageView(settings), clip = new ImageView(settings);
		StackPane set = new StackPane(view);
		view.setFitWidth(24);
		view.setFitHeight(24);
		clip.setFitWidth(24);
		clip.setFitHeight(24);
		view.setClip(clip);

		ColorAdjust monochrome = new ColorAdjust();
		monochrome.setBrightness(1.0);
		Blend blush = new Blend(BlendMode.MULTIPLY, monochrome, new ColorInput(0, 0, 24, 24, Color.CORNFLOWERBLUE));
		view.effectProperty().bind(Bindings.when(set.hoverProperty()).then((Effect) blush).otherwise((Effect) null));
		view.setCache(true);
		view.setCacheHint(CacheHint.SPEED);

		set.setPadding(new Insets(0, 10, 0, 0));
		set.setOnMouseClicked(me -> {
			if(me.getClickCount() == 1) {
				if(!layout.getChildren().contains(setup)) {
					saveSettings.setDisable(false);
					layout.getChildren().add(setup);
				}
			}
		});
		set.setCursor(Cursor.HAND);
		grid.add(set, 5, 0);

		GridPane.setValignment(welcome, VPos.CENTER);
		GridPane.setHgrow(welcome, Priority.ALWAYS);

		return grid;
	}

	private StackPane createSettingsPane() {
		Label titleB = new Label("General");
		titleB.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

		downloadThumbs = new CheckBox("Save thumbnails locally (thumbs/)");
		downloadThumbs.setDisable(true);

		prefixReplies = new CheckBox("Prefix username when replying to a comment");
		prefixReplies.setSelected(true);

		showWelcome = new CheckBox("Show welcome message");
		showWelcome.setSelected(true);

		Label titleA = new Label("Login to YouTube");
		titleA.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

		Label desc = new Label("Sign in to leave comments and replies.");
		desc.setWrapText(true);

		addAccount = new Button("Add Account");
		addAccount.setOnAction(ae -> {
			Task<Void> task = new Task<Void>(){
				protected Void call() throws Exception {
					System.out.println("OAuth2");
					Platform.runLater(() -> {
						WebView web = new WebView();
						WebEngine engine = web.getEngine();
						engine.setJavaScriptEnabled(true);
						try {
							engine.load(OA2Handler.getOAuth2Url());
							web.setPrefSize(400, 575);
							Dialog<WebView> dialog = new Dialog<>();
							dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
							dialog.getDialogPane().setContent(web);
							dialog.titleProperty().bind(engine.titleProperty());
							engine.titleProperty().addListener(e -> { // Check for correct token to appear in title.
								System.out.println("Title Change: "+engine.getTitle());
								if(engine.getTitle() != null && (engine.getTitle().contains("code=") || engine.getTitle().contains("error="))) {
									String response = engine.getTitle();
									String code = response.substring(13, response.length());
									web.setDisable(true);
									try {
										OA2Tokens tokens = OA2Handler.getAccessTokens(code);
										getConfig().submitTokens(tokens);
										accountList.getChildren().clear();
										accountList.getChildren().addAll(getConfig().accounts.stream().map(AccountPane::new).collect(Collectors.toList()));
										saveSettings.requestFocus();
									} catch (IOException e1) {
										e1.printStackTrace();
									}
									dialog.close();
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
			thread.start();
		});

		Button closeSettings = new Button("Close");
		closeSettings.setOnAction(ae -> layout.getChildren().remove(setup));

		saveSettings = new Button("Save");
		saveSettings.setOnAction(ae -> {
			try {
				config.setShowWelcome(showWelcome.isSelected());
				config.setDownloadThumbs(downloadThumbs.isSelected());
				config.setPrefixReplies(prefixReplies.isSelected());
				config.save();
				applyConfig();
				closeSettings.fire();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		saveSettings.setId("completeForm");

		HBox hBtn = new HBox(10);
		hBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hBtn.getChildren().addAll(closeSettings, saveSettings);

		VBox vbox = new VBox(10);
		vbox.setId("stackMenu");
		vbox.setAlignment(Pos.TOP_LEFT);
		vbox.setFillWidth(true);
		vbox.setPadding(new Insets(25,25,25,25));
		vbox.getChildren().addAll(titleB, showWelcome, downloadThumbs, prefixReplies, titleA, desc, addAccount, accountList, hBtn);

		ScrollPane scroll = new ScrollPane(vbox);
		scroll.setFitToWidth(true);
		scroll.setFitToHeight(true);
		scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

		Tab settings = new Tab("YouTube");
		settings.setContent(scroll);

		Label title1 = new Label("About");
		title1.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

		Label author = new Label("Author: mattwright324");

		Hyperlink link = new Hyperlink("GitHub/youtube-comment-suite");
		link.setOnMouseClicked(me -> {
			openInBrowser("https://github.com/mattwright324/youtube-comment-suite");
		});

		Label title2 = new Label("MIT License");
		title2.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

		TextArea license = new TextArea("MIT License\n\n" +
				"Copyright (c) 2017 Matthew Wright\n\n" +
				"Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\n" +
				"The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\n" +
				"THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.");
		license.setWrapText(true);
		license.setEditable(false);
		VBox.setVgrow(license, Priority.ALWAYS);

		VBox vbox1 = new VBox(10);
		vbox1.setId("stackMenu");
		vbox1.setAlignment(Pos.TOP_LEFT);
		vbox1.setFillWidth(true);
		vbox1.setPadding(new Insets(25,25,25,25));
		vbox1.getChildren().addAll(title1, author, link, title2, license);

		Tab about = new Tab("About");
		about.setContent(vbox1);

		TabPane tabs = new TabPane();
		tabs.setMaxWidth(600);
		tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		tabs.getTabs().addAll(settings, about);

		StackPane glass = new StackPane();
		glass.setStyle("-fx-background-color: rgba(127,127,127,0.5);");
		glass.setMaxHeight(Double.MAX_VALUE);
		glass.setMaxWidth(Double.MAX_VALUE);
		glass.setAlignment(Pos.CENTER);
		glass.getChildren().add(tabs);
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
