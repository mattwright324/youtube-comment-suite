package mattw.youtube.commensuitefx;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CommentResult extends HBox {
	
	final static Map<String,Image> profileMap = new HashMap<String,Image>();
	final static Image BLANK_PROFILE = new Image(CommentResult.class.getResourceAsStream("/mattw/youtube/commentsuite/images/blank_profile.jpg"));
	
	public static CommentResult lastSelected = null;
	
	final Comment comment;
	final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
	final Hyperlink author;
	final Label date, textShort, likes;
	final Hyperlink reply, viewtree, viewfulltext;
	
	final String parsedText;
	
	private boolean selected;
	
	public void setSelected(boolean select) {
		selected = select;
		if(selected) {
			if(lastSelected != null) lastSelected.setSelected(false);
			setId("commentSelected");
			lastSelected = this;
			Platform.runLater(() -> {
				try {
					CommentSuiteFX.app.loadContext(comment.video_id);
				} catch (SQLException | ParseException e) {
					e.printStackTrace();
				}
			});
		} else {
			setId(comment.is_reply ? "commentReply" : "");
		}
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public CommentResult(Comment c, boolean showTreeLink) {
		super();
		comment = c;
		setMinHeight(90);
		setPrefHeight(90);
		setMaxHeight(90);
		setMinWidth(200);
		setPrefWidth(750);
		setMaxWidth(950);
		setSelected(false);
		setAlignment(Pos.CENTER_LEFT);
		
		VBox box = new VBox();
		box.setMaxWidth(75);
		box.setPrefWidth(75);
		box.setMinWidth(75);
		box.setAlignment(Pos.CENTER);
		ImageView img = new ImageView(BLANK_PROFILE);
		img.setFitHeight(32);
		img.setFitWidth(32);
		if(c.channel.buffered_profile != null) {
			if(profileMap.containsKey(c.channel.channel_id)) {
				img.setImage(profileMap.get(c.channel.channel_id));
			} else {
				Image converted = SwingFXUtils.toFXImage(c.channel.buffered_profile, null);
				profileMap.put(c.channel.channel_id, converted);
				img.setImage(converted);
			}
			
		}
		box.getChildren().addAll(img, c.is_reply ? new Label("Reply") : new Label("Comment"));
		
		author = new Hyperlink(c.channel.channel_name);
		author.setOnAction(e -> {
			CommentSuiteFX.openInBrowser("https://www.youtube.com/channel/"+c.channel.channel_id);
		});
		if(c.channel.channel_id.equals(CommentSuiteFX.app.config.getChannelId())) {
			author.setId("commentMine");
		}
		date = new Label(sdf.format(c.comment_date));
		date.setId("commentDate");
		
		likes = new Label(c.comment_likes > 0 ? "+"+c.comment_likes : "");
		likes.setId("commentLikes");
		
		int length = 400;
		parsedText = Jsoup.parse(comment.comment_text.replace("<br />", "\r\n")).text();
		textShort = new Label(parsedText.length() > length ? parsedText.substring(0, length-3)+"..." : parsedText);
		textShort.setWrapText(true);
		
		reply = new Hyperlink("Reply");
		reply.setOnAction(e -> {
			replyToComment();
		});
		
		viewtree = new Hyperlink("View Tree"+(c.reply_count > 0 ? " ("+c.reply_count+" replies)" : ""));
		viewtree.setDisable(!showTreeLink);
		viewtree.setOnAction(e -> {
			try {
				CommentSuiteFX.app.viewTree(this.comment);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		});
		
		viewfulltext = new Hyperlink("Show Full Comment");
		viewfulltext.setOnAction(e -> {
			viewFullComment();
		});
		
		HBox hbox = new HBox(10);
		hbox.setAlignment(Pos.CENTER_LEFT);
		hbox.getChildren().add(date); // save can_reply
		if(c.comment_likes > 0) hbox.getChildren().add(likes);
		if(CommentSuiteFX.app.config.getAccessTokens() != null) hbox.getChildren().add(reply);
		if(c.reply_count > 0 || c.is_reply) hbox.getChildren().add(viewtree);
		hbox.getChildren().add(viewfulltext);
		
		VBox text = new VBox();
		text.setAlignment(Pos.CENTER_LEFT);
		text.getChildren().addAll(author, textShort, hbox);
		
		getChildren().addAll(box, text);
		
		setOnMouseClicked(e -> {
			setSelected(!isSelected());
		});
	}
	
	public void replyToComment() {
		Dialog<ButtonType> dialog = new Dialog<ButtonType>();
		dialog.setTitle("Reply");
		DialogPane pane = new DialogPane();
		pane.getButtonTypes().addAll(ButtonType.FINISH, ButtonType.CANCEL);
		dialog.setDialogPane(pane);
		
		VBox vbox = new VBox();
		vbox.setPadding(new Insets(10,10,10,10));
		vbox.setFillWidth(true);
		pane.setContent(vbox);
		
		TextArea text = new TextArea();
		text.setPromptText("Write your response here.");
		int width = 450;
		int height = 500;
		text.setMaxWidth(width);
		text.setPrefWidth(width);
		text.setMinWidth(width);
		text.setMaxHeight(height);
		text.setPrefHeight(height);
		text.setMinHeight(height);
		text.setWrapText(true);
		
		if(comment.is_reply) text.setText("+"+comment.channel.channel_name+" ");
		
		vbox.getChildren().addAll(text);
		VBox.setVgrow(text, Priority.ALWAYS);
		
		dialog.showAndWait().ifPresent(result -> {
			if(result == ButtonType.FINISH) {
				if(!text.getText().equals("")) {
					System.out.println("Commenting");
					OA2Handler.postNewReply(comment.is_reply ? comment.parent_id : comment.comment_id, text.getText());
				}
			}
		});
	}
	
	public void viewFullComment() {
		Dialog<Void> dialog = new Dialog<Void>();
		dialog.setTitle("Viewing Full Comment");
		DialogPane pane = new DialogPane();
		pane.getButtonTypes().add(ButtonType.CLOSE);
		dialog.setDialogPane(pane);
		
		VBox vbox = new VBox(10);
		vbox.setPadding(new Insets(10,10,10,10));
		vbox.setFillWidth(true);
		pane.setContent(vbox);
		
		ImageView img = new ImageView(BLANK_PROFILE);
		img.setFitHeight(32);
		img.setFitWidth(32);
		if(comment.channel.buffered_profile != null) {
			img.setImage(SwingFXUtils.toFXImage(comment.channel.buffered_profile, null));
		}
		HBox hbox = new HBox(5);
		Hyperlink label = new Hyperlink(author.getText());
		label.setOnAction(author.getOnAction());
		hbox.getChildren().addAll(img, label);
		
		int width = 450;
		int height = 350;
		TextArea text = new TextArea(parsedText);
		text.setMaxWidth(width);
		text.setPrefWidth(width);
		text.setMinWidth(width);
		text.setMaxHeight(height);
		text.setPrefHeight(height);
		text.setMinHeight(height);
		text.setWrapText(true);
		text.setEditable(false);
		vbox.getChildren().addAll(hbox, text);
		VBox.setVgrow(text, Priority.ALWAYS);
		dialog.showAndWait();
	}
	
}
