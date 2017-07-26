package mattw.youtube.commensuitefx;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.SQLException;
import java.util.List;

public class GroupManagePane extends GridPane implements EventHandler<ActionEvent> {

    private GroupManager manager;
    private ChoiceBox<Group> choice;
    private Button createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup;
    private Button cleanDB, resetDB;

    public void handle(ActionEvent ae) {
        Object o = ae.getSource();
        if(o.equals(createGroup)) {
            List<Group> allGroups = choice.getItems();
            TextInputDialog input = new TextInputDialog("");
            input.setTitle("Create Group");
            input.setContentText("Pick a unique nameProperty: ");
            input.showAndWait().ifPresent(result -> {
                boolean unique = true;
                for(Group g : allGroups) {
                    if(g.group_name.equals(result))
                        unique = false;
                }
                if(unique) {
                    try {
                        CommentSuiteFX.getDatabase().insertGroup(result);
                        CommentSuiteFX.reloadGroups();
                    } catch (SQLException ignored) {}
                }
            });
        } else if(o.equals(deleteGroup)) {
            Group current = choice.getSelectionModel().getSelectedItem();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setContentText("Are you sure you want to delete '"+current.group_name+"' and all of its data?");
            dialog.showAndWait().ifPresent(result -> {
                if(result == ButtonType.OK) {
                    Task<Void> task = new Task<Void>(){
                        protected Void call() throws Exception {
                            CommentSuiteFX.setNodesDisabled(true, choice, createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB);
                            try {
                                CommentSuiteFX.getDatabase().removeGroupAndData(current);
                                Platform.runLater(() -> {
                                    try {
                                        CommentSuiteFX.reloadGroups();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                });
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            CommentSuiteFX.setNodesDisabled(false, choice, createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB);
                            return null;
                        }
                    };
                    Thread thread = new Thread(task);
                    thread.start();
                }
            });
        } else if(o.equals(renameGroup)) {
            Group current = choice.getSelectionModel().getSelectedItem();
            TextInputDialog input = new TextInputDialog(current.group_name);
            input.setTitle("Rename Group");
            input.setContentText("Pick a new nameProperty: ");
            input.showAndWait().ifPresent(result -> {
                if(!current.group_name.equals(result)) {
                    try {
                        CommentSuiteFX.getDatabase().updateGroupName(current.group_id, result);
                        CommentSuiteFX.reloadGroups();
                    } catch (SQLException ignored) {}
                }
            });
        } else if(o.equals(refreshGroup)) {
            Task<Void> task = new Task<Void>() {
                protected Void call() throws Exception {
                    if(manager != null) {
                        Platform.runLater(() -> {
                            manager.refresh();
                            CommentSuiteFX.setNodesDisabled(true, deleteGroup, renameGroup, refreshGroup, reloadGroup);
                        });
                    }
                    return null;
                }
            };
            new Thread(task).start();
        } else if(o.equals(reloadGroup)) {
            Platform.runLater(() -> {
                reloadGroup.setDisable(true);
                manager.reloadGroupData();
                reloadGroup.setDisable(false);
            });

        } else if(o.equals(cleanDB) || o.equals(resetDB)) {
            if(o.equals(cleanDB)) {
                CommentSuiteFX.getMainPane().getChildren().add(createCleanDbPane());
            } else if(o.equals(resetDB)) {
                CommentSuiteFX.getMainPane().getChildren().add(createResetDbPane());
            }
        }
    }

    public GroupManagePane() {
        setAlignment(Pos.TOP_CENTER);
        setHgap(5);
        setVgap(5);

        HBox menu = new HBox(5);
        menu.setAlignment(Pos.CENTER);
        add(menu, 0, 0);

        Label label = new Label("Select a group: ");

        choice = new ChoiceBox<>(CommentSuiteFX.getApp().groupsList);
        choice.setMaxWidth(250);
        choice.setPrefWidth(150);
        choice.setOnAction(e -> {
            deleteGroup.setDisable(true);
            renameGroup.setDisable(true);
            refreshGroup.setDisable(true);
            Group group = choice.getSelectionModel().getSelectedItem();
            if(group != null) {
                if(GroupManager.managers.containsKey(group.group_id)) {
                    manager = GroupManager.managers.get(group.group_id);
                } else {
                    manager = new GroupManager(group, CommentSuiteFX.getDatabase(), CommentSuiteFX.getYoutube());
                    GroupManager.managers.put(group.group_id, manager);
                }
                for(GroupManager gm : GroupManager.managers.values()) {
                    if(getChildren().contains(gm)) {
                        getChildren().remove(gm);
                    }
                }
                if(manager != null)
                    CommentSuiteFX.setNodesDisabled(manager.isRefreshing(), deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB);
                add(manager, 0, 1);
                GridPane.setHgrow(manager, Priority.ALWAYS);
                GridPane.setVgrow(manager, Priority.ALWAYS);
            }
        });

        createGroup = new Button("Create");
        createGroup.setTooltip(new Tooltip("Create a new, empty group."));
        createGroup.setOnAction(this);

        deleteGroup = new Button("Delete");
        deleteGroup.setTooltip(new Tooltip("Delete this group and all its data."));
        deleteGroup.setOnAction(this);
        deleteGroup.setDisable(true);
        deleteGroup.setStyle("-fx-base: mistyrose");

        renameGroup = new Button("Rename");
        renameGroup.setTooltip(new Tooltip("Rename this group."));
        renameGroup.setOnAction(this);
        renameGroup.setDisable(true);

        refreshGroup = new Button("Refresh");
        refreshGroup.setTooltip(new Tooltip("Check for new videos, comments, and replies."));
        refreshGroup.setOnAction(this);
        refreshGroup.setDisable(true);
        refreshGroup.setStyle("-fx-base: honeydew");

        reloadGroup = new Button("Reload");
        reloadGroup.setTooltip(new Tooltip("Reloads the data displayed."));
        reloadGroup.setOnAction(this);
        reloadGroup.setDisable(true);

        Separator sep = new Separator();
        sep.setOrientation(Orientation.VERTICAL);

        cleanDB = new Button("Clean DB");
        cleanDB.setTooltip(new Tooltip("Perform a VACUUM on the database."));
        cleanDB.setOnAction(this);

        resetDB = new Button("Reset DB");
        resetDB.setTooltip(new Tooltip("Delete everything and start from scratch. Does not affect sign-in or key."));
        resetDB.setOnAction(this);
        resetDB.setStyle("-fx-base: firebrick");

        menu.getChildren().addAll(label, choice, createGroup, deleteGroup, renameGroup, refreshGroup, reloadGroup, sep, cleanDB, resetDB);
    }

    private StackPane createCleanDbPane() {
        Label title = new Label("Clean Database");
        title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

        Label about = new Label("Repacks database into smallest size (VACUUM).");
        Label warn = new Label("Warning: This may take some time.");

        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxHeight(20);
        pi.setMaxWidth(20);
        pi.setVisible(false);

        Button cancel = new Button("Cancel");
        Button clean = new Button("Clean");
        clean.setStyle("-fx-base: derive(cornflowerblue, 80%);");

        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.getChildren().addAll(pi, cancel, clean);

        VBox vbox = new VBox(10);
        vbox.setId("stackMenu");
        vbox.setPadding(new Insets(25,25,25,25));
        vbox.setMaxWidth(350);
        vbox.setMaxHeight(0);
        vbox.getChildren().addAll(title, about, warn, hbox);

        StackPane glass = new StackPane();
        glass.setStyle("-fx-background-color: rgba(127,127,127,0.5);");
        glass.setMaxHeight(Double.MAX_VALUE);
        glass.setMaxWidth(Double.MAX_VALUE);
        glass.setAlignment(Pos.CENTER);
        glass.getChildren().add(vbox);
        cancel.setOnAction(ae -> CommentSuiteFX.getMainPane().getChildren().remove(glass));
        clean.setOnAction(ae -> {
            Task<Void> task = new Task<Void>() {
                protected Void call() throws Exception {
                    CommentSuiteFX.setNodesDisabled(true, cancel, clean);
                    pi.setVisible(true);
                    try {
                        CommentSuiteFX.getDatabase().clean();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        cancel.setText("Close");
                        clean.setVisible(false);
                        clean.setManaged(false);
                        pi.setVisible(false);
                    });
                    CommentSuiteFX.setNodesDisabled(false, cancel, clean);
                    if(manager != null)
                        CommentSuiteFX.setNodesDisabled(manager.isRefreshing(), deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB);
                    return null;
                }
            };
            Thread thread = new Thread(task);
            thread.start();
        });
        return glass;
    }

    private StackPane createResetDbPane() {
        Label title = new Label("Reset Database");
        title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

        Label warn = new Label("Warning: There is no going back!");
        warn.setStyle("-fx-text-fill: red;");

        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxHeight(20);
        pi.setMaxWidth(20);
        pi.setVisible(false);

        Button cancel = new Button("Cancel");
        Button reset = new Button("Yes, delete everything.");
        reset.setStyle("-fx-base: firebrick;");

        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.getChildren().addAll(pi, reset, cancel);

        VBox vbox = new VBox(10);
        vbox.setId("stackMenu");
        vbox.setPadding(new Insets(25,25,25,25));
        vbox.setMaxWidth(350);
        vbox.setMaxHeight(0);
        vbox.getChildren().addAll(title, new Label("Delete all data and thumbnails."), new Label("Does not remove YouTube sign-ins."), warn, hbox);

        StackPane glass = new StackPane();
        glass.setStyle("-fx-background-color: rgba(127,127,127,0.5);");
        glass.setMaxHeight(Double.MAX_VALUE);
        glass.setMaxWidth(Double.MAX_VALUE);
        glass.setAlignment(Pos.CENTER);
        glass.getChildren().add(vbox);
        cancel.setOnAction(ae -> CommentSuiteFX.getMainPane().getChildren().remove(glass));
        reset.setOnAction(ae -> {
            Task<Void> task = new Task<Void>(){
                protected Void call() throws Exception {
                    CommentSuiteFX.setNodesDisabled(true, reset, cancel);
                    pi.setVisible(true);
                    try {
                        CommentSuiteFX.getDatabase().dropTables();
                        CommentSuiteFX.getDatabase().setup();
                        CommentSuiteFX.getDatabase().clean();
                        Platform.runLater(() -> {
                            try {
                                CommentSuiteFX.reloadGroups();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        cancel.setText("Close");
                        reset.setVisible(false);
                        reset.setManaged(false);
                        pi.setVisible(false);
                    });
                    CommentSuiteFX.setNodesDisabled(false, reset, cancel);
                    if(manager != null)
                        CommentSuiteFX.setNodesDisabled(manager.isRefreshing(), deleteGroup, renameGroup, refreshGroup, reloadGroup, cleanDB, resetDB);
                    return null;
                }
            };
            Thread thread = new Thread(task);
            thread.start();
        });
        return glass;
    }
}
