package mattw.youtube.commensuitefx;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.datav3.list.SearchList;

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
			setStyle("-fx-background-color: radial-gradient(focus-distance 0%, center 50% 50%, radius 55%, rgba(220,80,80,0.5), rgba(220,220,220,0))");
		} else {
			setStyle("");
		}
	}
	
	public boolean isSelected() {
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
			CommentSuiteFX.openInBrowser(url);
		});
		context.getItems().addAll(open);
		setOnMouseClicked(e -> {
			if(e.isPopupTrigger()) {
				context.show(this, e.getScreenX(), e.getScreenY());
			} else {
				setSelected(!select.isSelected());
			}
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
		BufferedImage bi = null;
		try {
			bi = ImageIO.read(thumbUrl);
			img.setImage(SwingFXUtils.toFXImage(bi, null));
			img.setFitHeight(80);
			img.setFitWidth(80 * img.getImage().getWidth() / img.getImage().getHeight());
		} catch (IOException e) {} finally {
			if(bi != null) {
				bi.flush();
			}
		}
		System.gc();
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
