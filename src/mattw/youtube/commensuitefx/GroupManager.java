package mattw.youtube.commensuitefx;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.commentsuite.*;

public class GroupManager extends StackPane {
	
	public static Map<Integer, GroupManager> managers = new HashMap<Integer, GroupManager>();
	public GroupManager manager;
	
	public ObservableList<GroupItem> gi_list = FXCollections.observableArrayList();
	public ObservableList<Video> v_list = FXCollections.observableArrayList();
	
	public TabPane tabs = new TabPane();
	public Tab items, videos, analytics;
	
	public ExecutorService es;
	public boolean refreshing = false;
	public Button close;
	
	public Group group;
	
	public GroupManager(Group g) {
		super();
		manager = this;
		
		setMaxHeight(Double.MAX_VALUE);
		setMaxWidth(Double.MAX_VALUE);
		group = g;
		
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
		
		Label gi_label = new Label("0 items");
		gi_label.setMaxWidth(Double.MAX_VALUE);
		gi_label.setAlignment(Pos.CENTER);
		grid.add(gi_label, 0, 0);
		GridPane.setHgrow(gi_label, Priority.ALWAYS);
		
		TableView<GroupItem> gi_table = new TableView<>();
		gi_table.setMaxHeight(Double.MAX_VALUE);
		gi_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		gi_table.setItems(gi_list);
		TableColumn<GroupItem, String> typeCol = new TableColumn<>("Type");
		typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
		TableColumn<GroupItem, String> gi_titleCol = new TableColumn<>("Title");
		gi_titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		TableColumn<GroupItem, Long> gi_checked = new TableColumn<>("Last Checked");
		gi_checked.setCellValueFactory(new PropertyValueFactory<>("last_checked"));
		gi_table.getColumns().add(typeCol);
		gi_table.getColumns().add(gi_titleCol);
		gi_table.getColumns().add(gi_checked);
		grid.add(gi_table, 0, 1);
		GridPane.setVgrow(gi_table, Priority.ALWAYS);
		
		Label v_label = new Label("0 videos");
		v_label.setMaxWidth(Double.MAX_VALUE);
		v_label.setAlignment(Pos.CENTER);
		grid.add(v_label, 1, 0);
		GridPane.setHgrow(v_label, Priority.ALWAYS);
		
		TableView<Video> v_table = new TableView<>();
		v_table.setMaxHeight(Double.MAX_VALUE);
		v_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		v_table.setItems(v_list);
		TableColumn<Video,String> v_titleCol = new TableColumn<>("Title");
		v_titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
		v_table.getColumns().add(v_titleCol);
		grid.add(v_table, 1, 1);
		GridPane.setConstraints(v_table, 1, 1, 1, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
		
		
		analytics = new Tab("Analytics");
		analytics.setClosable(false);
		
		VBox vbox2 = new VBox();
		analytics.setContent(vbox2);
		
		tabs.getTabs().addAll(items, analytics);
	}
	
	public void reloadTables() {
		gi_list.clear();
		v_list.clear();
	}
	
	public boolean isRefreshing() {
		return refreshing;
	}
	
	// Overlay stackpane like the setup overlay but doesn't affect whole application.
	// Make use of JavaFX graphs.
	public void refresh() throws InterruptedException {
		refreshing = true;
		
		StackPane stack = new StackPane();
		stack.setStyle("-fx-background-color: linear-gradient(rgba(200,200,200,0.2), rgba(220,220,200,0.5), rgba(220,220,200,0.85), rgba(220,220,220,1))");
		stack.setMaxHeight(Double.MAX_VALUE);
		stack.setMaxWidth(Double.MAX_VALUE);
		getChildren().add(stack);
		
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25,25,25,25));
		stack.getChildren().add(grid);
		
		Label label = new Label("Progress");
		label.setAlignment(Pos.CENTER);
		label.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));
		grid.add(label, 0, 0);
		
		Label status = new Label("Part 1 of 2. Checking for new videos.");
		status.setAlignment(Pos.CENTER);
		status.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
		grid.add(status, 1, 0);
		
		NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel("Elapsed Time (s)");
		NumberAxis yAxis = new NumberAxis();
		LineChart<Number,Number> line = new LineChart<>(xAxis, yAxis);
		XYChart.Series<Number,Number> vseries = new XYChart.Series<>();
		vseries.setName("New Videos");
		XYChart.Series<Number,Number> cseries = new XYChart.Series<>();
		cseries.setName("New Comments");
		line.getData().add(vseries);
		line.getData().add(cseries);
		line.setPrefWidth(600);
		line.setPrefHeight(300);
		grid.add(line, 0, 1, 2, 1);
		
		HBox hbox = new HBox();
		hbox.setAlignment(Pos.CENTER_RIGHT);
		close = new Button("Close");
		close.setDisable(true);
		close.setOnAction(e -> {
			getChildren().remove(stack);
		});
		hbox.getChildren().add(close);
		grid.add(hbox, 0, 2, 2, 1);

		es = Executors.newCachedThreadPool();
		Task<Void> task = new Task<Void>() {
			protected Void call() throws Exception {
				Random rand = new Random();
				long start = System.currentTimeMillis();
				long last = 0;
				long vcount = 0, ccount = 0;
				long seconds = 0;
				boolean videos_done = false;
				do {
					if(System.currentTimeMillis() - last >= 5000) {
						final long t = seconds, v = vcount, c = ccount;
						final boolean vd = videos_done;
						System.out.println(t+", "+v+", "+c);
						Platform.runLater(() -> {
							if(vd == false)
								vseries.getData().add(new XYChart.Data<>(t, v));
							cseries.getData().add(new XYChart.Data<>(t, c));
						});
						last = System.currentTimeMillis();
						seconds += 5;
					}
					if(System.currentTimeMillis() - start >= 15000) {
						Platform.runLater(() -> {
							status.setText("Part 2 of 2\tChecking for new commentThreads.");
						});
						ccount += rand.nextInt(50)+50;
						videos_done = true;
					} else if(System.currentTimeMillis() - start >= 0) {
						vcount += rand.nextInt(4);
					}
					Thread.sleep(200);
				} while(System.currentTimeMillis() - start < 60 * 1000);
				Platform.runLater(() -> {
					status.setText("Complete.");
				});
				refreshing = false;
				close.setDisable(false);
				CommentSuiteFX.app.setupWithManager(manager);
				return null;
			}
		};
		es.execute(task);
		es.shutdown();
	}
}
