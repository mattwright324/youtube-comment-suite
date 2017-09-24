package mattw.youtube.commentsuite;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GroupManageView extends StackPane {

    private Group group;
    private GroupRefresh refreshThread;

    public GroupManageView(Group group) {
        setPadding(new Insets(10));
        setStyle("-fx-border-color: red");
        this.group = group;

        Label title = new Label();
        title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 16));
        title.textProperty().bind(group.nameProperty());

        Button refresh = new Button("Refresh");
        refresh.setOnAction(ae -> beginGroupRefresh());

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().addAll(title, refresh);

        getChildren().addAll(vbox);
    }

    private void beginGroupRefresh() {
        this.refreshThread = new GroupRefresh(group, CommentSuite.db(), CommentSuite.youtube());

        ProgressIndicator activity = new ProgressIndicator();
        activity.setMaxWidth(25);
        activity.setMaxHeight(25);
        activity.visibleProperty().bind(refreshThread.refreshingProperty());

        Label title = new Label();
        title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 16));
        title.textProperty().bind(refreshThread.refreshStatusProperty());

        Label subtitle = new Label();
        subtitle.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 12));
        subtitle.textProperty().bind(refreshThread.elapsedTimeValueProperty());

        ProgressBar progress = new ProgressBar();
        progress.maxWidth(Double.MAX_VALUE);
        progress.prefWidth(Double.MAX_VALUE);
        progress.progressProperty().bind(refreshThread.progressProperty());

        HBox header = new HBox(10);
        header.getChildren().addAll(activity, title);

        Button finish = new Button("Finish");
        finish.disableProperty().bind(refreshThread.completedProperty().not());

        HBox hbox1 = new HBox(10);
        hbox1.setAlignment(Pos.CENTER_RIGHT);
        hbox1.getChildren().add(finish);

        VBox vbox0 = new VBox(10);
        vbox0.setPadding(new Insets(0, 0, 0, 35));
        vbox0.setAlignment(Pos.TOP_LEFT);
        vbox0.setFillWidth(true);
        vbox0.getChildren().addAll(subtitle, progress, hbox1);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(25));
        vbox.setMaxWidth(300);
        vbox.setMaxHeight(0);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: #eee; -fx-opacity: 1;");
        vbox.getChildren().addAll(header, vbox0);

        StackPane overlay = new StackPane(vbox);
        overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4); -fx-border-color: green");
        getChildren().addAll(overlay);

        finish.setOnAction(ae -> {
            finish.disableProperty().unbind();
            title.textProperty().unbind();
            progress.progressProperty().unbind();
            subtitle.textProperty().unbind();
            activity.visibleProperty().unbind();
            Platform.runLater(() -> getChildren().remove(overlay));
        });

        this.refreshThread.start();
    }
}
