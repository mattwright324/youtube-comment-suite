package mattw.youtube.commensuitefx;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
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
	
	final CommentType ct;
	
	final ImageView img;
	final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
	final Hyperlink author;
	final Label date, textShort, likes;
	final Hyperlink reply, viewtree, viewfulltext;
	final MenuItem loadProfile, openInBrowser, copyName, copyText, copyChannelLink, copyVideoLink, copyCommentLink;
	
	final String parsedText;
	
	
	private boolean selected;
	
	public void setSelected(boolean select) {
		selected = select;
		if(selected) {
			if(lastSelected != null) lastSelected.setSelected(false);
			setId("itemSelected");
			lastSelected = this;
			Platform.runLater(() -> {
				try {
					CommentSuiteFX.app.loadContext(ct.getVideoId());
				} catch (SQLException | ParseException e) {
					e.printStackTrace();
				}
			});
		} else {
			setId(ct.isReply() ? "commentReply" : "");
		}
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public CommentResult(CommentType c, boolean showTreeLink) {
		super();
		ct = c;
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
		img = new ImageView(BLANK_PROFILE);
		img.setFitHeight(32);
		img.setFitWidth(32);
		Image thumb = DatabaseManager.getChannel(c.getChannelId()).fetchThumb();
		if(thumb != null)
			img.setImage(thumb);
		box.getChildren().addAll(img, c.isReply() ? new Label("Reply") : new Label("Comment"));
		
		author = new Hyperlink(DatabaseManager.getChannel(c.getChannelId()).getTitle());
		author.setOnAction(e -> {
			CommentSuiteFX.openInBrowser("https://www.youtube.com/channel/"+c.getChannelId());
		});
		if(c.getChannelId().equals(CommentSuiteFX.app.config.getChannelId())) {
			author.setId("commentMine");
		}
		date = new Label(sdf.format(c.getDate()));
		date.setId("commentDate");
		
		likes = new Label(c.getLikes() > 0 ? "+"+c.getLikes() : "");
		likes.setId("commentLikes");
		
		int length = 400;
		parsedText = Jsoup.parse(c.getText().replace("<br />", "\r\n")).text();
		textShort = new Label(parsedText.length() > length ? parsedText.substring(0, length-3)+"..." : parsedText);
		textShort.setWrapText(true);
		
		reply = new Hyperlink("Reply");
		reply.setOnAction(e -> {
			replyToComment();
		});
		
		viewtree = new Hyperlink("View Tree"+(c.getReplies() > 0 ? " ("+c.getReplies()+" replies)" : ""));
		viewtree.setDisable(!showTreeLink);
		viewtree.setOnAction(e -> {
			try {
				CommentSuiteFX.app.viewTree(this.ct);
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
		if(c.getLikes() > 0) hbox.getChildren().add(likes);
		if(CommentSuiteFX.app.config.getAccessTokens() != null) hbox.getChildren().add(reply);
		if(c.getReplies() > 0 || c.isReply()) hbox.getChildren().add(viewtree);
		hbox.getChildren().add(viewfulltext);
		
		VBox text = new VBox();
		text.setAlignment(Pos.CENTER_LEFT);
		text.getChildren().addAll(author, textShort, hbox);
		
		getChildren().addAll(box, text);
		
		ContextMenu context = new ContextMenu();
		
		openInBrowser = new MenuItem("Open in Browser");
		openInBrowser.setOnAction(e -> {
			CommentSuiteFX.openInBrowser(ct.getYoutubeLink());
		});
		
		loadProfile = new MenuItem("Load Profile Image");
		loadProfile.setOnAction(e -> {
			try {
				loadProfileImage();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		});
		
		copyName = new MenuItem("Copy Username");
		copyName.setOnAction(e -> {
			Clipboards.setClipboard(DatabaseManager.getChannel(ct.getChannelId()).getTitle());
		});
		
		copyText = new MenuItem("Copy Comment");
		copyText.setOnAction(e -> {
			Clipboards.setClipboard(ct.getText());
		});
		
		copyChannelLink = new MenuItem("Copy Channel Link");
		copyChannelLink.setOnAction(e -> {
			Clipboards.setClipboard(DatabaseManager.getChannel(ct.getChannelId()).getYoutubeLink());
		});
		
		copyVideoLink = new MenuItem("Copy Video Link");
		copyVideoLink.setOnAction(e -> {
			Clipboards.setClipboard("https://youtu.be/"+ct.getVideoId());
		});
		
		copyCommentLink = new MenuItem("Copy Comment Link");
		copyCommentLink.setOnAction(e -> {
			Clipboards.setClipboard(ct.getYoutubeLink());
		});
		
		context.getItems().addAll(openInBrowser, loadProfile, new SeparatorMenuItem(), copyName, copyText, copyChannelLink, copyVideoLink, copyCommentLink);
		
		setOnMouseClicked(e -> {
			if(e.isPopupTrigger()) {
				if(!context.isShowing()) {
					context.show(this, e.getScreenX(), e.getScreenY());
				}
			} else {
				if(context.isShowing()) {
					context.hide();
				}
				setSelected(!isSelected());
			}
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
		
		if(ct.isReply()) text.setText("+"+DatabaseManager.getChannel(ct.getChannelId()).getTitle()+" ");
		
		vbox.getChildren().addAll(text);
		VBox.setVgrow(text, Priority.ALWAYS);
		
		dialog.showAndWait().ifPresent(result -> {
			if(result == ButtonType.FINISH) {
				if(!text.getText().equals("")) {
					System.out.println("Commenting");
					OA2Handler.postNewReply(ct.isReply() ? ct.getParentId() : ct.getId(), text.getText());
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
		Image thumb = DatabaseManager.getChannel(ct.getChannelId()).fetchThumb();
		if(thumb != null) {
			img.setImage(thumb);
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
	
	
	public void refreshImage() {
		ChannelType channel = DatabaseManager.getChannel(ct.getChannelId());
		if(channel.hasThumb())
			img.setImage(channel.fetchThumb());
	}
	
	public void loadProfileImage() throws SQLException {
		ChannelType channel = DatabaseManager.getChannel(ct.getChannelId());
		if(!channel.hasThumb()) {
			CommentSuiteFX.app.database.updateChannelFetchThumb(channel.getId(), true);
			channel = CommentSuiteFX.app.database.getChannelById(channel.getId());
			DatabaseManager.channelCache.put(channel.getId(), channel);
			CommentSuiteFX.app.refreshResultProfiles();
		}
	}
}

