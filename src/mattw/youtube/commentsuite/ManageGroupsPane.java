package mattw.youtube.commentsuite;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import mattw.youtube.commentsuite.db.Group;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ManageGroupsPane extends StackPane {

    private SimpleStringProperty managerGroupId = new SimpleStringProperty(Group.NO_GROUP);
    private Map<String,GroupManageView> managerMap = new HashMap<>();
    private StackPane managerDisplay = new StackPane();

    public ManageGroupsPane() {
        Label message = new Label("Create groups of YouTube videos, playlists, and channels.");
        message.setTextFill(Color.LIGHTGRAY);

        Label label = new Label("Select a group: ");

        ComboBox<Group> groupList = new ComboBox<>();
        groupList.setId("control");
        groupList.setPrefWidth(200);
        groupList.setMaxWidth(200);
        groupList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if(ov != null) {
                ov.nameProperty().unbind();
            }
            if(nv != null) {
                managerGroupId.setValue(nv.getId());
                nv.nameProperty().addListener((o1, ov1, nv1) -> {
                    groupList.setItems(CommentSuite.db().globalGroupList);
                    groupList.setValue(nv);
                });
            }
            new Thread(() -> {
                Platform.runLater(() -> {
                    managerDisplay.getChildren().clear();
                    if(!groupList.isDisabled() && nv != null) {
                        if(!managerMap.containsKey(nv.getId()) || managerMap.get(nv.getId()).deletedProperty().getValue()) {
                            managerMap.put(nv.getId(), new GroupManageView(nv));
                        }
                        managerDisplay.getChildren().add(managerMap.get(nv.getId()));
                    } else {
                        managerDisplay.getChildren().add(message);
                    }
                });
            }).start();
        });
        groupList.setItems(CommentSuite.db().globalGroupList);
        groupList.getItems().addListener((ListChangeListener<Group>) c -> {
            if(groupList.getSelectionModel().getSelectedIndex() == -1 && groupList.getItems() != null && groupList.getItems().size() > 0) {
                groupList.getSelectionModel().select(0);
            }
        });
        if(groupList.getSelectionModel().getSelectedIndex() == -1 && groupList.getItems() != null && groupList.getItems().size() > 0) {
            groupList.getSelectionModel().select(0);
        }

        Button create = new Button("Create New Group");
        create.setId("control");

        HBox control = new HBox(10);
        control.setPadding(new Insets(0, 0, 10, 0));
        control.setAlignment(Pos.CENTER);
        control.getChildren().addAll(label, groupList, create);

        Label divider = new Label();
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setMinHeight(4);
        divider.setMaxHeight(4);
        divider.setStyle("-fx-background-color: derive(firebrick, 95%);");

        managerDisplay.setAlignment(Pos.CENTER);
        managerDisplay.getChildren().add(message);
        VBox.setVgrow(managerDisplay, Priority.ALWAYS);

        VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().addAll(control, divider, managerDisplay);

        getChildren().add(vbox);
        setPadding(new Insets(10,0,0,0));

        create.setOnAction(ae -> {
            Label title = new Label("Create Group");
            title.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 18));

            TextField nameField = new TextField();
            nameField.setMinWidth(250);
            nameField.setPromptText("Group name...");

            Label error = new Label("");
            error.setManaged(false);

            Button doCreate = new Button("Create");
            doCreate.setStyle("-fx-base: derive(cornflowerblue, 70%)");

            Button cancel = new Button("Cancel");

            HBox hbox0 = new HBox(10);
            hbox0.setAlignment(Pos.CENTER_RIGHT);
            hbox0.getChildren().addAll(cancel, doCreate);

            VBox vbox0 = new VBox(10);
            vbox0.setAlignment(Pos.CENTER);
            vbox0.setMaxWidth(0);
            vbox0.setMaxHeight(0);
            vbox0.setId("overlayMenu");
            vbox0.setPadding(new Insets(25));
            vbox0.getChildren().addAll(title, nameField, error, hbox0);

            StackPane overlay = new StackPane(vbox0);
            overlay.setStyle("-fx-background-color: rgba(127,127,127,0.4);");
            getChildren().add(overlay);

            cancel.setOnAction(ae0 -> getChildren().remove(overlay));

            doCreate.setOnAction(ae0 -> {
                try {
                    Group group = CommentSuite.db().createGroup(nameField.getText());
                    groupList.setValue(group);
                    cancel.fire();
                } catch (SQLException e) {
                    error.setText(e.getMessage());
                    error.setManaged(true);
                }
            });
        });
    }
}
