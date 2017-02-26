package mattw.youtube.commensuitefx;

import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.datav3.list.SearchList;

public class SearchResult extends HBox {
	
	final Label author, title, description, published;
	final private CheckBox select;
	
	final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
	final Date publishedAt;
	
	final GitemType gitem;
	
	public void setSelected(boolean s) {
		select.setSelected(s);
		if(s) {
			setId("itemSelected");
		} else {
			setId("");
		}
	}
	
	public boolean isSelected() {
		return select.isSelected();
	}
	
	public SearchResult(SearchList.Item item) {
		super(5);
		setAlignment(Pos.CENTER_LEFT);
		setFillHeight(true);
		gitem = new GitemType(item, false);
		int width = 850;
		int height = 120;
		setMaxWidth(width);
		setPrefWidth(width);
		setMinWidth(250);
		setMaxHeight(height);
		setPrefHeight(height);
		setMinHeight(height);
		setPadding(new Insets(0,10,0,10));
		
		author = new Label(gitem.getChannelTitle());
		publishedAt = new Date(gitem.getPublished());
		description = new Label("Published on "+sdf.format(publishedAt)+"  "+item.snippet.description);
		published = new Label();
		title = new Label(gitem.getTitle());
		
		select = new CheckBox();
		select.setOnAction(e -> {
			setSelected(select.isSelected());
		});
		
		Label type = new Label(gitem.getTypeText());
		type.setAlignment(Pos.CENTER);
		type.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 14));
		
		ImageView img = new ImageView(gitem.fetchThumb());
		img.setFitHeight(80);
		img.setFitWidth(80 * img.getImage().getWidth() / img.getImage().getHeight());
		// img.setCursor(Cursor.HAND);
		
		VBox thumb = new VBox(5);
		thumb.setAlignment(Pos.CENTER);
		thumb.setMaxWidth(160);
		thumb.setPrefWidth(160);
		thumb.getChildren().addAll(img, type);
		
		title.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
		title.setMaxWidth(550);
		
		author.setMaxWidth(550);
		
		description.setWrapText(true);
		description.setMaxWidth(550);
		description.setMaxHeight(50);
		
		VBox info = new VBox(5);
		info.setAlignment(Pos.CENTER_LEFT);
		info.getChildren().addAll(title, author, description);
		
		getChildren().addAll(select, thumb, info);
		
		ContextMenu context = new ContextMenu();
		MenuItem open = new MenuItem("Open in Browser");
		open.setOnAction(ae -> {
			CommentSuiteFX.openInBrowser(gitem.getYoutubeLink());
		});
		context.getItems().addAll(open);
		
		setOnMouseClicked(me -> {
			if(me.isPopupTrigger()) {
				context.show(this, me.getScreenX(), me.getScreenY());
			} else {
				if(me.getSource().equals(img)) {
					CommentSuiteFX.openInBrowser(gitem.getYoutubeLink());
				} else {
					setSelected(!select.isSelected());
				}
			}
		});
	}
}
