package mattw.youtube.commensuitefx;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import mattw.youtube.datav3.YoutubeData;

public class CommentSuiteFX extends Application implements EventHandler<ActionEvent> {

	private final YCSConfig config = new YCSConfig();
	private YoutubeData data;
	private DatabaseManager database;
	private static CommentSuiteFX instance;

	public static final Image PLACEHOLDER = new Image(CommentResult.class.getResourceAsStream("./images/placeholder4.png"));
	public final ObservableList<Group> groupsList = FXCollections.observableArrayList();

	private StackPane layout;
	private StackPane setup;
	private GridPane main;
	private YoutubeSearchPane videos;
	private GroupManagePane groups;
	private CommentSearchPane comments;

	private ImageView header;
	private ToggleButton videoToggle;
	private ToggleButton groupToggle;
	private ToggleButton commentToggle;
	private Label welcome;

	private Button saveAndSetup;
	private Button exitSetup;
	private Button signin;
	private final VBox accountList = new VBox();

	public static void main(String[] args) {
		launch(args);
	}

	public static CommentSuiteFX getApp() { return instance; }
	public static DatabaseManager getDatabase() { return instance.database; }
	public static YCSConfig getConfig() { return instance.config; }
	public static StackPane getMainPane() { return instance.layout; }
	public static YoutubeData getYoutube() { return instance.data; }

	public YoutubeSearchPane getYoutubeSearchPane() { return videos; }
	public GroupManagePane getGroupManagePane() { return groups; }
	public CommentSearchPane getCommentSearchPane() { return comments; }

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
			System.out.println("Loaded Accounts: "+getConfig().accounts.size());
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
				header.setImage(new Image(getClass().getResourceAsStream("./images/youtube.png")));
			} else if(groupToggle.isSelected()) {
				if(main.getChildren().contains(videos)) main.getChildren().remove(videos);
				if(main.getChildren().contains(comments)) main.getChildren().remove(comments);
				if(!main.getChildren().contains(groups)) {
					main.add(groups, 0, 1);
					GridPane.setVgrow(groups, Priority.ALWAYS);
				}
				header.setImage(new Image(getClass().getResourceAsStream("./images/manage.png")));
			} else if(commentToggle.isSelected()) {
				if(main.getChildren().contains(videos)) main.getChildren().remove(videos);
				if(main.getChildren().contains(groups)) main.getChildren().remove(groups);
				if(!main.getChildren().contains(comments)) {
					main.add(comments, 0, 1);
					GridPane.setVgrow(comments, Priority.ALWAYS);
				}
				header.setImage(new Image(getClass().getResourceAsStream("./images/search.png")));
			} else {
				System.out.println("Menu Toggle: Something broke.");
			}
		}
	}



	public void start(Stage stage) throws Exception {
		instance = this;
		try {
			loadConfig();
		} catch (Exception e) { e.printStackTrace(); }

		database = new DatabaseManager();
		database.setup();

		layout = new StackPane();
		setup = createSettingsPane();
		GridPane menu = createMenuBar();
		videos = new YoutubeSearchPane();
		groups = new GroupManagePane();
		comments = new CommentSearchPane();

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
		stage.setTitle("YouTube Comment Suite");
		stage.getIcons().add(new Image(getClass().getResourceAsStream("./images/fxicon.png")));
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

		StackPane img = new StackPane();
		header = new ImageView(new Image(getClass().getResourceAsStream("./images/youtube.png")));
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

		videoToggle = new ToggleButton("Search YouTube");
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

		Image settings = new Image(getClass().getResourceAsStream("./images/settings.png"));
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
					saveAndSetup.setDisable(false);
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

		CheckBox downloadThumbs = new CheckBox("Save Thumbnails Locally");

		Label titleA = new Label("Login to YouTube");
		titleA.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));

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
										saveAndSetup.requestFocus();
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
		vbox.setAlignment(Pos.TOP_LEFT);
		vbox.setFillWidth(true);
		vbox.setPadding(new Insets(25,25,25,25));
		vbox.getChildren().addAll(titleB, downloadThumbs, titleA, desc, signin, accountList, hBtn);

		Tab login = new Tab("YouTube");
		login.setContent(vbox);

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
				"Copyright (c) 2016 Matthew Wright\n\n" +
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
		tabs.getTabs().addAll(login, about);

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
