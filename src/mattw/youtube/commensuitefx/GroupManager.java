package mattw.youtube.commensuitefx;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.JsonSyntaxException;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.commensuitefx.DatabaseManager.Comment;
import mattw.youtube.commensuitefx.DatabaseManager.Viewer;
import mattw.youtube.datav3.YoutubeData;
import mattw.youtube.datav3.list.ChannelsList;
import mattw.youtube.datav3.list.CommentThreadsList;
import mattw.youtube.datav3.list.CommentsList;
import mattw.youtube.datav3.list.PlaylistItemsList;
import mattw.youtube.datav3.list.VideosList;

class GroupManager extends StackPane {

	private static int VIDEO_THREAD_COUNT = 10;
	private static int COMMMENT_THREAD_COUNT = 20;
	public static final Map<Integer, GroupManager> managers = new HashMap<>();
	private final GroupManager manager;
	
	private final ObservableList<GitemType> gi_list = FXCollections.observableArrayList();
	private final ObservableList<VideoType> v_list = FXCollections.observableArrayList();
	private final ObservableList<GitemType> choiceList = FXCollections.observableArrayList();
	
	private final TabPane tabs = new TabPane();
	private final Tab items;
    private final Tab analytics;
	private final Label gi_label;
    private final Label v_label;
	private final VBox abox;
	private final Button loadAnalytics;
	private ChoiceBox<GitemType> choice;
	private ChoiceBox<String> type;
	
	private ExecutorService es;
	private boolean refreshing = false;
	private Button close;
	
	private Group group;
	private final int group_id;
	private static DatabaseManager database;
	private static YoutubeData data;
	
	public GroupManager(Group g, DatabaseManager database, YoutubeData data) {
		super();
		manager = this;
		GroupManager.database = database;
		GroupManager.data = data;
		
		setMaxHeight(Double.MAX_VALUE);
		setMaxWidth(Double.MAX_VALUE);
		group = g;
		group_id = g.group_id;
		
		VBox vbox = new VBox();
		
		getChildren().add(vbox);
		tabs.setMaxHeight(Double.MAX_VALUE);
		tabs.setMaxWidth(Double.MAX_VALUE);
		vbox.getChildren().add(tabs);
		VBox.setVgrow(tabs, Priority.ALWAYS);
		
		items = new Tab("Items and Videos");
		items.setClosable(false);
		
		GridPane grid = new GridPane();
		grid.setMaxHeight(Double.MAX_VALUE);
		grid.setAlignment(Pos.CENTER);
		grid.setVgap(5);
		grid.setHgap(5);
		grid.setPadding(new Insets(10,10,10,10));
		ColumnConstraints col1 = new ColumnConstraints();
		col1.setPercentWidth(40);
		ColumnConstraints col2 = new ColumnConstraints();
		col2.setPercentWidth(60);
		grid.getColumnConstraints().addAll(col1, col2);
		items.setContent(grid);
		
		gi_label = new Label("Add some items to this group.");
		gi_label.setMaxWidth(Double.MAX_VALUE);
		gi_label.setAlignment(Pos.CENTER);
		grid.add(gi_label, 0, 0);
		GridPane.setHgrow(gi_label, Priority.ALWAYS);
		
		TableView<GitemType> gi_table = new TableView<>();
		gi_table.setMaxHeight(Double.MAX_VALUE);
		gi_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		gi_table.setItems(gi_list);
		TableColumn<GitemType, String> typeCol = new TableColumn<>("Type");
		typeCol.setCellValueFactory(new PropertyValueFactory<>("typeText"));
		typeCol.setCellFactory(col -> new TableCell<GitemType, String>(){
			public void updateItem(String item, boolean empty) {
				if(empty || item == null) {
					setText(null);
				} else {
					setText(item);
				}
				setAlignment(Pos.CENTER);
			}
		});
		TableColumn<GitemType, String> gi_titleCol = new TableColumn<>("Title");
		gi_titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		TableColumn<GitemType, Long> gi_checkedCol = new TableColumn<>("Last checked");
		gi_checkedCol.setCellValueFactory(new PropertyValueFactory<>("lastChecked"));
		gi_checkedCol.setCellFactory(col -> new TableCell<GitemType, Long>(){
			private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
			public void updateItem(Long item, boolean empty) {
				if(empty || item == null) {
					setText(null);
				} else {
					setText(item > 0 ? sdf.format(new Date(item)) : "Never");
				}
				setAlignment(Pos.CENTER);
			}
		});
		gi_table.getColumns().add(typeCol);
		gi_table.getColumns().add(gi_titleCol);
		gi_table.getColumns().add(gi_checkedCol);
		grid.add(gi_table, 0, 1);
		GridPane.setVgrow(gi_table, Priority.ALWAYS);
		
		ContextMenu gi_menu = new ContextMenu();
		MenuItem gi_open = new MenuItem("Open in Browser");
		gi_open.setOnAction(e -> {
			GitemType gi = gi_table.getSelectionModel().getSelectedItem();
			CommentSuiteFX.openInBrowser(gi.getYoutubeLink());
		});
		gi_menu.getItems().add(gi_open);
		gi_table.setOnMouseClicked(e -> {
			if(e.isPopupTrigger()) {
				gi_menu.show(this, e.getScreenX(), e.getScreenY());
			}
		});
		
		
		v_label = new Label("Refresh this group to get video data.");
		v_label.setMaxWidth(Double.MAX_VALUE);
		v_label.setAlignment(Pos.CENTER);
		grid.add(v_label, 1, 0);
		GridPane.setHgrow(v_label, Priority.ALWAYS);
		
		TableView<VideoType> v_table = new TableView<>();
		v_table.setMaxHeight(Double.MAX_VALUE);
		v_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		v_table.setItems(v_list);
		TableColumn<VideoType,String> v_titleCol = new TableColumn<>("Title");
		v_titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		v_titleCol.setCellFactory(col -> new TableCell<VideoType, String>(){
			public void updateItem(String item, boolean empty) {
				if(empty || item == null) {
					setText(null);
				} else {
					setText(item);
				}
				setAlignment(Pos.CENTER_LEFT);
			}
		});
		TableColumn<VideoType,VideoType> v_aboutCol = new TableColumn<>("About");
		v_aboutCol.setCellValueFactory(celldata -> new ReadOnlyObjectWrapper<>(celldata.getValue()));
		v_aboutCol.setCellFactory(col -> new TableCell<VideoType,VideoType>(){
			private VBox vbox;
			private Label author;
			private Label published;
			private Label comments;
			private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
			{
				vbox = new VBox();
				author = createLabel();
				published = createLabel();
				published.setStyle("-fx-color: lightgray");
				published.setTextFill(Color.GOLDENROD);
				comments = createLabel();
				vbox.getChildren().addAll(author,published,comments);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			}
			private Label createLabel() {
				Label label = new Label();
				label.setMaxWidth(Double.MAX_VALUE);
				label.setAlignment(Pos.CENTER);
				VBox.setVgrow(label, Priority.ALWAYS);
				return label;
			}
			public void updateItem(VideoType video, boolean empty) {
				if(empty) {
					setGraphic(null);
				} else {
					author.setText(DatabaseManager.isChannelLoaded(video.getChannelId()) ? DatabaseManager.getChannel(video.getChannelId()).getTitle() : "Not found.");
					published.setText(sdf.format(video.getPublishDate()));
					comments.setText(video.getHttpCode() == 200 ? video.getComments()+" comments" : video.getHttpCode() == 403 ? "Comments Disabled" : "HTTP "+video.getHttpCode());
					if(video.getHttpCode() != 200) {
						comments.setStyle("-fx-color: red");
						vbox.setStyle("-fx-background-color: mistyrose");
					} else {
						comments.setStyle("");
						vbox.setStyle("");
					}
					setGraphic(vbox);
				}
			}
		});
		TableColumn<VideoType,Long> v_checkedCol = new TableColumn<>("Last checked");
		v_checkedCol.setCellValueFactory(new PropertyValueFactory<>("grabDate"));
		v_checkedCol.setCellFactory(col -> new TableCell<VideoType, Long>(){
			private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
			public void updateItem(Long item, boolean empty) {
				if(empty || item == null) {
					setText(null);
				} else {
					setText(item > 0 ? sdf.format(new Date(item)) : "Never");
				}
				setAlignment(Pos.CENTER);
			}
		});
		v_table.getColumns().add(v_titleCol);
		v_table.getColumns().add(v_aboutCol);
		v_table.getColumns().add(v_checkedCol);
		grid.add(v_table, 1, 1);
		GridPane.setConstraints(v_table, 1, 1, 1, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
		
		ContextMenu v_menu = new ContextMenu();
		MenuItem v_open = new MenuItem("Open in Browser");
		v_open.setOnAction(e -> {
			VideoType v = v_table.getSelectionModel().getSelectedItem();
			CommentSuiteFX.openInBrowser(v.getYoutubeLink());
		});
		v_menu.getItems().add(v_open);
		v_table.setOnMouseClicked(e -> {
			if(e.isPopupTrigger()) {
				v_menu.show(this, e.getScreenX(), e.getScreenY());
			}
		});
		
		analytics = new Tab("Overview");
		analytics.setClosable(false);
		
		VBox vbox2 = new VBox(5);
		vbox2.setPadding(new Insets(5,5,5,5));
		vbox2.setAlignment(Pos.TOP_CENTER);
		analytics.setContent(vbox2);
		
		HBox menu = new HBox(5);
		menu.setAlignment(Pos.CENTER);
		
		loadAnalytics = new Button("Load");
		loadAnalytics.setOnAction(e -> {
			Task<Void> task = new Task<Void>() {
				protected Void call() throws Exception {
					loadAnalytics.setDisable(true);
					choice.setDisable(true);
					type.setDisable(true);
					loadAnalytics(choice.getValue(), type.getSelectionModel().getSelectedIndex());
					loadAnalytics.setDisable(false);
					choice.setDisable(false);
					type.setDisable(false);
					return null;
				}
			};
			Thread thread = new Thread(task);
			thread.start();
		});
		
		choice = new ChoiceBox<>();
		choice.setMaxWidth(200);
		choice.setItems(choiceList);
		
		type = new ChoiceBox<>();
		type.getItems().addAll("Comments", "Videos");
		type.getSelectionModel().select(0);
		
		menu.getChildren().addAll(loadAnalytics, choice, type);
		
		abox = new VBox(5);
		abox.setAlignment(Pos.TOP_CENTER);
		
		vbox2.getChildren().addAll(menu, abox);
		
		reloadGroupData();
		tabs.getTabs().addAll(analytics, items);
		
		loadAnalytics.fire();
	}
	
	class ViewerEntry extends HBox {

		private ImageView thumb;
		public final Label num;
		public final TextField author;
        public final TextField about;
		public final VBox vbox;

		public Viewer viewer;
		public Comment comment;
		public VideoType video;

		public final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
		
		private ViewerEntry(int pos) {
			super(10);
			setAlignment(Pos.CENTER_LEFT);
			
			if(pos % 2 == 0) {
				setId("odd");
			}
			
			author = new TextField("...");
			author.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 15));
			author.setEditable(false);
			author.setId("context");
			
			about = new TextField("...");
			about.setEditable(false);
			about.setId("context");
			
			num = new Label(pos+".");
			num.setPrefWidth(35);
			num.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 16));
			num.setAlignment(Pos.CENTER);
			HBox.setHgrow(num, Priority.ALWAYS);

			vbox = new VBox();
			vbox.setFillWidth(true);
		}
		
		public ViewerEntry(int pos, Viewer viewer) {
			this(pos);
			this.viewer = viewer;
			
			author.setText(DatabaseManager.getChannel(viewer.channelId).getTitle());
			
			thumb = new ImageView(CommentResult.BLANK_PROFILE);
			thumb.setFitHeight(26);
			thumb.setFitWidth(26);
			thumb.setCursor(Cursor.HAND);
			thumb.setOnMouseClicked(e -> CommentSuiteFX.openInBrowser(DatabaseManager.getChannel(viewer.channelId).getYoutubeLink()));
			if(DatabaseManager.getChannel(viewer.channelId).hasThumb()) {
				thumb.setImage(DatabaseManager.getChannel(viewer.channelId).fetchThumb());
			}
			
			setText();
			vbox.getChildren().addAll(author, about);
			getChildren().addAll(num, thumb, vbox);
		}
		
		public ViewerEntry(int pos, Comment comment) {
			this(pos);
			setMaxWidth(500);
			setPrefWidth(500);
			this.comment = comment;
			
			HBox.setHgrow(vbox, Priority.ALWAYS);
			
			author.setText(comment.commentText);
			
			setText();
			vbox.getChildren().addAll(author, about);
			getChildren().addAll(num, vbox);
		}
		
		public ViewerEntry(int pos, VideoType video) {
			this(pos);
			this.video = video;
			
			thumb = new ImageView(video.fetchThumb());
			thumb.setFitHeight(32);
			thumb.setFitWidth(57);
			thumb.setCursor(Cursor.HAND);
			thumb.setOnMouseClicked(e -> CommentSuiteFX.openInBrowser(video.getYoutubeLink()));
			
			setText();
			vbox.getChildren().addAll(author, about);
			getChildren().addAll(num, thumb, vbox);
		}
		
		public void setText() {}
	}
	
	private void loadAnalytics(GitemType gitem, int type) {
		Platform.runLater(() -> abox.getChildren().clear());
		
		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel("Weeks");
		NumberAxis yAxis = new NumberAxis();
		LineChart<String,Number> chart = new LineChart<>(xAxis, yAxis);
		chart.setMinHeight(225);
		chart.setPrefWidth(250);
		chart.setMaxHeight(300);
		chart.setLegendVisible(false);
		XYChart.Series<String,Number> series = new XYChart.Series<>();
		chart.getData().add(series);
		Platform.runLater(() -> abox.getChildren().addAll(chart));
		
		FlowPane flow = new FlowPane();
		flow.setPadding(new Insets(10,10,10,10));
		flow.setAlignment(Pos.TOP_CENTER);
		flow.setHgap(5);
		
		if(type == 0) {
			chart.setTitle("Comment Counts by Week");
			yAxis.setLabel("Comments");
			
			VBox active = new VBox(5);
			active.setFillWidth(true);
			active.setAlignment(Pos.TOP_CENTER);
			Label lbl1 = new Label("Most Active Viewers");
			lbl1.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));
			active.getChildren().add(lbl1);
			
			VBox popular = new VBox(5);
			popular.setFillWidth(true);
			popular.setAlignment(Pos.TOP_CENTER);
			Label lbl2 = new Label("Most Popular Viewers");
			lbl2.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));
			popular.getChildren().add(lbl2);
			
			VBox comments = new VBox(5);
			comments.setFillWidth(true);
			comments.setAlignment(Pos.TOP_CENTER);
			Label lbl3 = new Label("Most Common Comments");
			lbl3.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));
			comments.getChildren().add(lbl3);
			
			flow.getChildren().addAll(active, popular, comments);
			
			ScrollPane scroll = new ScrollPane(flow);
			scroll.setFitToWidth(true);
			
			ProgressIndicator prog = new ProgressIndicator();
			prog.setMaxWidth(50);
			prog.setMinWidth(50);
			prog.setMaxHeight(50);
			prog.setMinHeight(50);
			
			Platform.runLater(() -> abox.getChildren().addAll(scroll, prog));
			
			try {
				Map<Long,Long> histogram = database.getWeekByWeekCommentHistogram(group_id, gitem.getGitemId());
				Platform.runLater(() -> xAxis.setLabel("Weeks ("+histogram.size()+" total)"));
				SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yy");
				for(long time : histogram.keySet()) {
					String date = sdf.format(new Date(time));
					Platform.runLater(() -> {
						XYChart.Data<String, Number> point = new XYChart.Data<>(date, histogram.get(time));
						series.getData().add(point);
						Tooltip.install(point.getNode(), new Tooltip("Week of "+date+" - "+sdf.format(new Date(time+604800000-60*60*24*1000))+"\nNew Comments: "+histogram.get(time)+""));
						point.getNode().setStyle("-fx-background-color: rgba(255,255,255,0.0)");
						point.getNode().setOnMouseEntered(e -> point.getNode().setStyle("-fx-background-color: orange"));
						point.getNode().setOnMouseExited(e -> point.getNode().setStyle("-fx-background-color: rgba(255,255,255,0.0)"));
					});
				}
			} catch (SQLException ignored) {}
			
			
			try {
				List<Viewer> mostActive = database.getMostActiveViewers(group_id, gitem.getGitemId(), 25);
				int pos = 1;
				for(Viewer viewer : mostActive) {
					final int num = pos;
					Platform.runLater(() -> active.getChildren().add(new ViewerEntry(num, viewer) {
                        public void setText() {
                            about.setText(viewer.totalComments+" total comments");
                        }
                    }));
					pos++;
				}
			} catch (SQLException ignored) {}
			
			try {
				List<Viewer> mostPopular = database.getMostPopularViewers(group_id, gitem.getGitemId(), 25);
				int pos = 1;
				for(Viewer viewer : mostPopular) {
					final int num = pos;
					Platform.runLater(() -> popular.getChildren().add(new ViewerEntry(num, viewer) {
                        public void setText() {
                            about.setText(viewer.totalLikes+" total likes");
                        }
                    }));
					pos++;
				}
			} catch (SQLException ignored) {}
			
			try {
				List<Comment> list = database.getMostCommonComments(group_id, gitem.getGitemId(), 25);
				int pos = 1;
				for(Comment c : list) {
					final int num = pos;
					Platform.runLater(() -> comments.getChildren().add(new ViewerEntry(num, c){
                        public void setText() {
                            about.setText(c.occurances+" times Last on "+sdf.format(new Date(c.lastCommentOn)));
                        }
                    }));
					pos++;
				}
				
			} catch (SQLException ignored) {}
			
			Platform.runLater(() -> abox.getChildren().remove(prog));
		} else if(type == 1) {
			chart.setTitle("Video Counts by Week");
			yAxis.setLabel("Videos");
			
			VBox popular = new VBox(5);
			popular.setFillWidth(true);
			popular.setAlignment(Pos.TOP_CENTER);
			Label lbl1 = new Label("Most Popular");
			lbl1.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));
			popular.getChildren().add(lbl1);
			
			VBox disliked = new VBox(5);
			disliked.setFillWidth(true);
			disliked.setAlignment(Pos.TOP_CENTER);
			Label lbl2 = new Label("Most Disliked");
			lbl2.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));
			disliked.getChildren().add(lbl2);
			
			VBox comments = new VBox(5);
			comments.setFillWidth(true);
			comments.setAlignment(Pos.TOP_CENTER);
			Label lbl3 = new Label("Most Commented");
			lbl3.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));
			comments.getChildren().add(lbl3);
			
			VBox disabled = new VBox(5);
			disabled.setFillWidth(true);
			disabled.setAlignment(Pos.TOP_CENTER);
			Label lbl4 = new Label("Disabled");
			lbl4.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));
			disabled.getChildren().add(lbl4);
			
			flow.getChildren().addAll(popular, disliked, comments, disabled);
			
			ScrollPane scroll = new ScrollPane(flow);
			scroll.setFitToWidth(true);
			
			ProgressIndicator prog = new ProgressIndicator();
			prog.setMaxWidth(50);
			prog.setMaxHeight(50);
			
			Platform.runLater(() -> abox.getChildren().addAll(scroll, prog));
			
			try {
				Map<Long,Long> histogram = database.getWeekByWeekVideoHistogram(group_id, gitem.getGitemId());
				Platform.runLater(() -> xAxis.setLabel("Weeks ("+histogram.size()+" total)"));
				SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yy");
				for(long time : histogram.keySet()) {
					String date = sdf.format(new Date(time));
					Platform.runLater(() -> {
						XYChart.Data<String, Number> point = new XYChart.Data<>(date, histogram.get(time));
						series.getData().add(point);
						Tooltip.install(point.getNode(), new Tooltip("Week of "+date+" - "+sdf.format(new Date(time+604800000-60*60*24*1000))+"\nNew Videos: "+histogram.get(time)+""));
						point.getNode().setStyle("-fx-background-color: rgba(255,255,255,0.0)");
						point.getNode().setOnMouseEntered(e -> point.getNode().setStyle("-fx-background-color: red"));
						point.getNode().setOnMouseExited(e -> point.getNode().setStyle("-fx-background-color: rgba(255,255,255,0.0)"));
					});
				}
			} catch (SQLException ignored) {}
			
			try {
				List<VideoType> videos = database.getMostPopularVideos(group_id, gitem.getGitemId(), 10);
				int pos = 1;
				for(VideoType v : videos) {
					final int num = pos;
					Platform.runLater(() -> popular.getChildren().add(new ViewerEntry(num, v){
                        public void setText() {
                            author.setText(video.getTitle());
                            about.setText(video.getViews()+" views");
                        }
                    }));
					pos++;
				}
			} catch (SQLException ignored) {}
			
			try {
				List<VideoType> videos = database.getMostDislikedVideos(group_id, gitem.getGitemId(), 10);
				int pos = 1;
				for(VideoType v : videos) {
					final int num = pos;
					Platform.runLater(() -> disliked.getChildren().add(new ViewerEntry(num, v){
                        public void setText() {
                            author.setText(video.getTitle());
                            about.setText(video.getDislikes()+" dislikes");
                        }
                    }));
					pos++;
				}
			} catch (SQLException ignored) {}
			
			try {
				List<VideoType> videos = database.getMostCommentedVideos(group_id, gitem.getGitemId(), 10);
				int pos = 1;
				for(VideoType v : videos) {
					final int num = pos;
					Platform.runLater(() -> comments.getChildren().add(new ViewerEntry(num, v){
                        public void setText() {
                            author.setText(video.getTitle());
                            about.setText(video.getComments()+" comments");
                        }
                    }));
					pos++;
				}
			} catch (SQLException ignored) {}
			
			try {
				List<VideoType> videos = database.getDisabledVideos(group_id, gitem.getGitemId());
				int pos = 1;
				if(videos.isEmpty()) {
					Label none = new Label("No comments disabled.");
					Platform.runLater(() -> disabled.getChildren().add(none));
				}
				for(VideoType v : videos) {
					final int num = pos;
					Platform.runLater(() -> disabled.getChildren().add(new ViewerEntry(num, v){
                        public void setText() {
                            author.setText(video.getTitle());
                            about.setText(v.getHttpCode() == 403 ? "Comments Disabled" : "HTTP "+v.getHttpCode());
                            about.setStyle("-fx-text-fill: firebrick");
                        }
                    }));
					pos++;
				}
			} catch (SQLException ignored) {}
			
			Platform.runLater(() -> abox.getChildren().remove(prog));
		}
	}
	
	public void reloadGroupData() {
		gi_list.clear();
		v_list.clear();
		choiceList.clear();
		try {
			group = database.getGroup(group_id);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			List<GitemType> list = database.getGitems(group_id, false);
			gi_list.addAll(list);
			choiceList.add(new GitemType(-1, "All Items ("+list.size()+")"));
			choiceList.addAll(list);
			choice.getSelectionModel().select(0);
			if(!gi_list.isEmpty())
				gi_label.setText(gi_list.size()+" items");
			else
				gi_label.setText("Add some items to this group.");
		} catch (SQLException e) {
			gi_label.setText(e.getClass().getName()+": "+e.getMessage());
			e.printStackTrace();
		}
		try {
			v_list.addAll(database.getVideos(group_id, false));
			if(!v_list.isEmpty())
				v_label.setText(v_list.size()+" videos");
			else
				v_label.setText("Refresh this group to get video data.");
		} catch (SQLException e) {
			v_label.setText(e.getClass().getName()+": "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public boolean isRefreshing() {
		return refreshing;
	}
	
	public void refresh() {
		refreshing = true;
		ProgressIndicator progress = new ProgressIndicator();
		progress.setMaxHeight(36);
		progress.setMinHeight(36);
		progress.setMaxWidth(36);
		progress.setMinWidth(36);

		Label title = new Label("Refreshing Group Data");
		title.setAlignment(Pos.CENTER);
		title.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label lbltc = new Label("Total Comments");
		Label lblvp = new Label("Video Progress");
		Label lblct = new Label("Comment Thread Progress");
		Label lblnc = new Label("New Comments");

		Label elapsedTime = new Label("");
		elapsedTime.setStyle("-fx-text-fill: cornflowerblue;");
		Label totalVideos = new Label("0");
		Label newVideos = new Label("0");
		newVideos.setStyle("-fx-text-fill: orange;");
		Label videoProgress = new Label("0");
		videoProgress.setStyle("-fx-text-fill: orange;");
		Label threadProgress = new Label("0");
		threadProgress.setStyle("-fx-text-fill: orange;");
		Label totalComments = new Label("0");
		Label newComments = new Label("0");
		newComments.setStyle("-fx-text-fill: orange;");

		GridPane grid = new GridPane();
		grid.setMinWidth(350);
		grid.setVgap(10);
		grid.setHgap(10);
		grid.addRow(0, new Label("Elapsed Time"), elapsedTime);
		grid.addRow(1, new Label("Total Videos"), totalVideos);
		grid.addRow(2, lbltc, totalComments);
		grid.addRow(4, new Label("New Videos"), newVideos);
		grid.addRow(5, lblvp, videoProgress);
		grid.addRow(6, lblct, threadProgress);
		grid.addRow(7, lblnc, newComments);
		setNodesDisabled(true, lbltc, lblvp, lblct, lblnc, totalComments, videoProgress, threadProgress, newComments);

		VBox vbox = new VBox(10);
		vbox.setFillWidth(true);
		vbox.getChildren().addAll(title, grid);

		HBox hbox = new HBox(10);
		hbox.setAlignment(Pos.TOP_LEFT);
		hbox.setFillHeight(true);
		hbox.setPadding(new Insets(25,35,25,35));
		hbox.getChildren().addAll(progress, vbox);

		VBox box = new VBox();
		box.setId("stackMenu");
		box.setMaxHeight(0);
		box.setMaxWidth(0);
		box.setAlignment(Pos.CENTER);
		box.getChildren().addAll(hbox);

		StackPane stack = new StackPane();
		stack.setStyle("-fx-background-color: rgba(127,127,127,0.5)");
		stack.setMaxHeight(Double.MAX_VALUE);
		stack.setMaxWidth(Double.MAX_VALUE);
		getChildren().add(stack);
		stack.getChildren().add(box);

		close = new Button("Finish");
		close.setDisable(true);
		close.setOnAction(e -> getChildren().remove(stack) );
		vbox.getChildren().add(close);
		
		es = Executors.newCachedThreadPool();
		Task<Void> task = new Task<Void>() {
			private final int COMMENT_INSERT_SIZE = 500;
			private final ElapsedTime timer = new ElapsedTime();
			private final AtomicLong new_comments = new AtomicLong(0);
			private final AtomicLong thread_progress = new AtomicLong(0);
			private final AtomicLong video_progress = new AtomicLong(0);
			private final AtomicLong total_comments = new AtomicLong(0);
			private final AtomicLong total_videos = new AtomicLong(0);
			
			private final List<String> existingVideoIds = new ArrayList<>();
			private final Set<String> existingCommentIds = new HashSet<>();
			private final Set<String> existingChannelIds = new HashSet<>();
			private final List<VideoGroup> existingVideoGroups = new ArrayList<>();
			private final List<GitemType> existingGroupItems = new ArrayList<>();
			
			private final Map<String, String> commentThreadIds = new HashMap<>(); // <commentThreadId, videoId>
			private Map<String, Integer> commentThreadReplies;
			private final Queue<String> videosQueue = new ConcurrentLinkedQueue<>();
			private final Queue<String> threadsQueue = new ConcurrentLinkedQueue<>(); // commentThreadIds.keySet()
			
			private final List<VideoType> insertVideos = new ArrayList<>();
			private final List<VideoType> updateVideos = new ArrayList<>();
			private final List<ChannelType> insertChannels = new ArrayList<>();
			private final List<ChannelType> updateChannels = new ArrayList<>();
			private final List<VideoGroup> insertVideoGroups = new ArrayList<>();

			private final List<String> gitemVideos = new ArrayList<>();
			private final List<GitemType> gitemChannels = new ArrayList<>();
			private final List<GitemType> gitemPlaylists = new ArrayList<>();

			private boolean canDie1 = false, canDie2 = false;
			private boolean stayAlive1 = true, stayAlive2 = true;

			protected Void call() {
				try {
					ExecutorService es1 = Executors.newFixedThreadPool(1);
					es1.execute(() -> {
						while(refreshing) {
							updateLabel(elapsedTime, timer.getTimeString());
							try { Thread.sleep(100); } catch (Exception ignored) {}
						}
					});
					es1.shutdown();

					timer.set();
					existingVideoIds.addAll(database.getVideoIds());
					existingCommentIds.addAll(database.getCommentIds(group_id));
					existingChannelIds.addAll(database.getChannelIds());
					existingVideoGroups.addAll(database.getVideoGroups());
					existingGroupItems.addAll(database.getGitems(group_id, false));
					commentThreadReplies = database.getCommentThreadReplyCounts(group_id);
					
					for(GitemType gi : existingGroupItems) {
						if(gi.typeId == 0) {
							VideoGroup vg = new VideoGroup(gi.getGitemId(), gi.getId());
							if(!existingVideoGroups.contains(vg)) {
								insertVideoGroups.add(vg);
							}
							gitemVideos.add(gi.getId());
						} else if(gi.typeId == 1) {
							gitemChannels.add(gi);
						} else if(gi.typeId == 2) {
							gitemPlaylists.add(gi);
						}
					}
					database.updateGitems(existingGroupItems);
					try {
						database.setAutoCommit(false);
						ExecutorService ves = Executors.newCachedThreadPool();
						for(int i=0; i < VIDEO_THREAD_COUNT; i++) { // Video Queue Threads
							final int tid = i;
							ves.execute(() -> {
								while(!videosQueue.isEmpty() || stayAlive1) {
									if(canDie1 && stayAlive1) stayAlive1 = false;
									try {
										String videoId;
										if((videoId = videosQueue.poll()) != null) {
											getComments(videoId);
											updateLabel(videoProgress, String.valueOf(video_progress.incrementAndGet()));
										}
									} catch (JsonSyntaxException | SQLException e) { e.printStackTrace(); }
									try { Thread.sleep(100); } catch (Exception ignored) {}
								}
								System.out.println("VQT"+tid+" has ended.");
							});
						}
						ExecutorService ces = Executors.newCachedThreadPool();
						for(int i=0; i < COMMMENT_THREAD_COUNT; i++) { // CommentThread Queue Threads
							final int tid = i;
							ces.execute(() -> {
								while(!threadsQueue.isEmpty() || stayAlive2) {
									if(canDie2 && stayAlive2) stayAlive2 = false;
									final String threadId = threadsQueue.poll();
									try {
										if(threadId != null) {
											getReplies(threadId, commentThreadIds.get(threadId));
											updateLabel(threadProgress, String.valueOf(thread_progress.incrementAndGet())+" / "+commentThreadIds.size());
										}
									} catch (JsonSyntaxException e) {
										e.printStackTrace();
									} catch (Throwable e) {
										System.err.println("Something broke for video: "+threadId);
										e.printStackTrace();
									}
									try { Thread.sleep(100); } catch (Exception ignored) {}
								}
								System.out.println("CTQT"+tid+" has ended.");
							});
						}
						try {
							parseVideoItems(gitemVideos, -1);
							parseChannelItems(gitemChannels);
							parsePlaylistItems(gitemPlaylists);
						} catch (JsonSyntaxException | IOException e) {
							e.printStackTrace();
						}
						database.insertVideos(insertVideos);
						database.updateVideos(updateVideos);
						database.insertVideoGroups(insertVideoGroups);
						clearAll(existingVideoIds, existingVideoGroups);
						clearAll(insertVideos, updateVideos, insertVideoGroups);
						clearAll(gitemVideos, gitemChannels, gitemPlaylists);

						videosQueue.addAll(database.getVideoIds(group_id));
						setNodesDisabled(false, lbltc, lblvp, lblct, lblnc, totalComments, videoProgress, threadProgress, newComments);

						canDie1 = true;
						ves.shutdown();
						ves.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
						canDie2 = true;
						ces.shutdown();
						ces.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

						database.insertChannels(insertChannels);
						database.updateChannels(updateChannels);
						database.commit();
					} catch (SQLException e) {
						e.printStackTrace();
					}
					clearAll(insertChannels, updateChannels, existingCommentIds, existingChannelIds);

					Platform.runLater(() -> {
						reloadGroupData();
						progress.setVisible(false);
					});
					database.setAutoCommit(true);
					close.setDisable(false);
					refreshing = false;
					// CommentSuiteFX.getApp().setupWithManager(manager);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			private void updateLabel(Label label, String text) {
				Platform.runLater(() -> label.setText(text));
			}

			private boolean videoListContainsId(List<VideoType> list, String id) {
				return list.stream().anyMatch(vt -> vt.getId().equals(id));
			}

			private void clearAll(Collection<?>... lists) {
				for(Collection<?> list : lists) {
					list.clear();
				}
			}
			
			private void parseChannelItems(List<GitemType> channels) throws JsonSyntaxException, IOException {
				for(GitemType gi : channels) {
					ChannelsList cl = data.getChannelsByChannelId(ChannelsList.PART_CONTENT_DETAILS, gi.getId(), ChannelsList.MAX_RESULTS, "");
					String uploadPlaylistId = cl.items[0].contentDetails.relatedPlaylists.uploads;
					handlePlaylist(uploadPlaylistId, gi.getGitemId());
				}
			}
			
			private void parsePlaylistItems(List<GitemType> playlists) throws JsonSyntaxException, IOException {
				for(GitemType gi : playlists) {
					handlePlaylist(gi.getId(), gi.getGitemId());
				}
			}
			
			private void handlePlaylist(final String playlistId, int gitem_id) throws JsonSyntaxException, IOException {
				PlaylistItemsList pil;
				String pageToken = "";
				List<String> videos = new ArrayList<>();
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
			
			private void parseVideoItems(List<String> videos, int gitem_id) throws JsonSyntaxException, IOException {
				updateLabel(totalVideos, String.valueOf(total_videos.addAndGet(videos.size())));
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
					String ids = sublist.stream().filter(id -> !videoListContainsId(insertVideos, id)).collect(Collectors.joining(","));
					handleVideos(ids);
				}
			}
			
			private void handleVideos(final String ids) throws JsonSyntaxException, IOException {
				System.out.println("Videos: "+ids);
				VideosList snip = data.getVideosById(VideosList.PART_SNIPPET, ids, VideosList.MAX_RESULTS, "");
				VideosList stats = data.getVideosById(VideosList.PART_STATISTICS, ids, VideosList.MAX_RESULTS, "");
				for(int i = 0; i<snip.items.length; i++) {
					VideosList.Item itemSnip = snip.items[i];
					VideosList.Item itemStat = stats.items[i];
					checkChannel(null, itemSnip, false);
					String videoId = itemSnip.id;
					String channelId = itemSnip.snippet.channelId;
					String title = itemSnip.snippet.title;
					String thumbUrl = itemSnip.snippet.thumbnails.medium.url.toString();
					String description = itemSnip.snippet.description;
					long views = itemStat.statistics.viewCount;
					long likes = itemStat.statistics.likeCount;
					long dislikes = itemStat.statistics.dislikeCount;
					long comments = itemStat.statistics.commentCount;
					VideoType video = new VideoType(videoId, channelId, title, thumbUrl, false, description, comments, likes, dislikes, views, itemSnip.snippet.publishedAt, new Date(), 200);
					System.out.println(videoId+": "+title);
					if(!(existingVideoIds.contains(itemSnip.id) || videoListContainsId(insertVideos, itemSnip.id) || videoListContainsId(updateVideos, itemSnip.id))) {
						insertVideos.add(video);
					} else {
						if(videoListContainsId(updateVideos, itemSnip.id))
							updateVideos.add(video);
					}
				}
				updateLabel(newVideos, String.valueOf(insertVideos.size()));
			}
			
			private void getComments(final String videoId) throws JsonSyntaxException, SQLException {
				List<CommentType> comments = new ArrayList<>();
				CommentThreadsList snippet = null;
				String snipToken = "";
				int fails = 0;
				do {
					if(comments.size() >= COMMENT_INSERT_SIZE) {
						submitComments(comments);
						comments.clear();
					}
					try {
						snippet = data.getCommentThreadsByVideoId(CommentThreadsList.PART_SNIPPET, videoId, CommentThreadsList.MAX_RESULTS, snipToken);
						snipToken = snippet.nextPageToken;
						total_comments.addAndGet(snippet.items.length);
						for(CommentThreadsList.Item item : snippet.items) {
							if(item.hasSnippet()) {
								String commentId = item.snippet.topLevelComment.id;
								boolean contains = commentThreadReplies.containsKey(commentId);
								if((!contains && item.snippet.totalReplyCount > 0) || (contains && item.snippet.totalReplyCount != commentThreadReplies.get(item.snippet.topLevelComment.id))) {
									commentThreadIds.put(commentId, videoId);
									threadsQueue.offer(commentId);
								} else {
									updateLabel(totalComments, String.valueOf(total_comments.addAndGet(item.snippet.totalReplyCount)));
								}
								if(!existingCommentIds.contains(commentId)) {
									checkChannel(item.snippet.topLevelComment, null, false);
									CommentType comment = new CommentType(item);
									if(!comment.getChannelId().equals("")) {
										comments.add(comment);
									} else {
										System.out.println("Google Plus comment? "+item.snippet.topLevelComment.snippet.authorChannelUrl);
									}
								}
							}
						}
					} catch (IOException e) {
						fails++;
						if(e.getMessage().contains("HTTP response code")) {
							Pattern p = Pattern.compile("([0-9]{3}) for URL");
							Matcher m = p.matcher(e.getMessage());
							if(m.find()) {
								try {
									int code = Integer.parseInt(m.group(1));
									if(code == 400) { // Retry / Bad request.
										System.err.println("Bad Request (400): Retry #"+fails+"  http://youtu.be/"+videoId);
									} else if(code == 403) { // Comments Disabled or Forbidden
										System.err.println("Comments Disabled (403): http://youtu.be/"+videoId);
										database.updateVideoHttpCode(videoId, code);
										break;
									} else if(code == 404) { // Not found.
										System.err.println("Not found (404): http://youtu.be/"+videoId);
										database.updateVideoHttpCode(videoId, code);
										break;
									} else { // Unknown error.
										System.err.println("Unknown Error ("+code+"): http://youtu.be/"+videoId);
										database.updateVideoHttpCode(videoId, code);
										break;
									}
								} catch (SQLException e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				} while ((snippet == null || snippet.nextPageToken != null) && fails < 5);
				if(comments.size() > 0) {
					submitComments(comments);
					comments.clear();
				}
			}
			
			private void getReplies(final String threadId, final String videoId) throws JsonSyntaxException, SQLException {
                List<CommentType> replies = new ArrayList<>();
                CommentsList cl = null;
                String pageToken = "";
                int fails = 0;
                do {
                    if (replies.size() >= COMMENT_INSERT_SIZE) {
                        submitComments(replies);
                        replies.clear();
                    }
                    try {
                        cl = data.getCommentsByParentId(threadId, CommentsList.MAX_RESULTS, pageToken);
                        total_comments.addAndGet(cl.items.length);
                        pageToken = cl.nextPageToken;
                        for (CommentsList.Item reply : cl.items) {
                            if (!existingCommentIds.contains(reply.id)) {
                                checkChannel(reply, null, false);
                                CommentType comment = new CommentType(reply, videoId);
                                if (!comment.getChannelId().equals("")) {
                                    replies.add(comment);
                                } else {
                                    System.out.println("Google Plus comment? " + reply.snippet.authorChannelUrl);
                                }
                            }
                        }
	                    updateLabel(totalComments, String.valueOf(total_comments.get()));
                    } catch (IOException e) {
                        fails++;
                    }
                } while (cl != null && cl.nextPageToken != null && fails < 5);
                if (replies.size() > 0) {
                    submitComments(replies);
                    replies.clear();
                }
            }

            /**
             * Insert new comments into the database.
             * @param comments list of comments
             * @throws SQLException insert failed
             */
			private void submitComments(List<CommentType> comments) throws SQLException {
				if(comments.size() > 0) {
					updateLabel(newComments, String.valueOf(new_comments.addAndGet(comments.size())));
					database.insertComments(comments.stream()
							.filter(ct -> !existingCommentIds.contains(ct.getId()))
							.collect(Collectors.toList()));
				}
			}

            /**
             * Checks the channel associated with a comment or video to see if it is unique.
             * @param comment null or a comment
             * @param video null or a video
             * @param fetchThumb fetch thumbnail of video or channel
             */
			private void checkChannel(CommentsList.Item comment, VideosList.Item video, boolean fetchThumb) {
				String channelId = null;
				if(comment != null && comment.snippet != null && comment.snippet.authorChannelId != null && comment.snippet.authorChannelId.value != null) {
					channelId = comment.snippet.authorChannelId.value;
				}
				if(video != null) {
					channelId = video.snippet.channelId;
				}
				ChannelType channel = null;
				if(channelId != null) {
					if(!existingChannelIds.contains(channelId)) {
						if(comment != null) {
							channel = new ChannelType(comment, fetchThumb);
						} else { // if(video != null)
							try {
								ChannelsList cl = data.getChannelsByChannelId(ChannelsList.PART_SNIPPET, channelId, 1, "");
								ChannelsList.Item item = cl.items[0];
								channel = new ChannelType(item, true);
							} catch (JsonSyntaxException | IOException e) {
								e.printStackTrace();
							}
						}
					}
					if(channel != null) {
						if(!existingChannelIds.contains(channelId)) {
							if(!insertChannels.contains(channel)) {
								insertChannels.add(channel);
							}
						} else {
							if(!updateChannels.contains(channel)) {
								updateChannels.add(channel);
							}
						}
					}
				}
			}
		};
		es.execute(task);
		es.shutdown();
	}

	private void setNodesDisabled(boolean disable, Node... nodes) {
		for(Node n : nodes) { n.setDisable(disable); }
	}
}
