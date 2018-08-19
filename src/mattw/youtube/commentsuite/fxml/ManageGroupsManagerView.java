package mattw.youtube.commentsuite.fxml;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.commentsuite.ImageCache;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.commentsuite.db.GroupItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Manages a specific group; refreshing, stats, renaming, deletion, adding group items, etc.
 *
 * Loads template FXML and displays info from database.
 *
 * @author mattwright324
 */
public class ManageGroupsManagerView extends StackPane implements ImageCache {

    private Logger logger = LogManager.getLogger(this.toString());
    private Image loading = new Image("/mattw/youtube/commentsuite/img/loading.png");
    private Image edit = new Image("/mattw/youtube/commentsuite/img/pencil.png");
    private Image close = new Image("/mattw/youtube/commentsuite/img/close.png");

    private ChangeListener<Font> fontListener;
    private Group group;

    private CommentDatabase database;

    @FXML OverlayModal<MGMVRefreshModal> refreshModal;
    @FXML OverlayModal deleteModal;
    @FXML OverlayModal addItemModal;
    @FXML OverlayModal removeItemModal;
    @FXML OverlayModal removeAllModal;
    @FXML Button btnAddItem;
    @FXML Button btnRemoveItems;
    @FXML Button btnRemoveAll;
    @FXML ListView<MGMVGroupItemView> groupItemList;

    @FXML TextField groupTitle;
    @FXML ImageView editIcon;
    @FXML Hyperlink rename;
    @FXML Button btnRefresh;
    @FXML Button btnReload;
    @FXML Button btnDelete;

    public ManageGroupsManagerView(Group group) throws IOException {
        logger.debug(String.format("Initialize for Group [id=%s,name=%s]", group.getId(), group.getName()));

        database = FXMLSuite.getDatabase();

        this.group = group;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("ManageGroupsManager.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        Random random = new Random();
        this.setStyle(String.format("-fx-background-color: linear-gradient(to top, rgba(%s,%s,%s,%s), transparent);",
                220-random.nextInt(60), 220-random.nextInt(60), 220-random.nextInt(60), 0.4));

        editIcon.setImage(edit);

        groupTitle.setMinWidth(Region.USE_PREF_SIZE);
        groupTitle.setMaxWidth(Region.USE_PREF_SIZE);
        groupTitle.textProperty().addListener((ov, prevText, currText) -> resizeTextField(groupTitle));
        groupTitle.fontProperty().addListener(fontListener = (o, ov, nv) -> {
            resizeTextField(groupTitle);
            // One-time font listener resize.
            // Will match content after font set on label from styleClass.
            // If not removed, when clicking the 'Rename' button, the label will
            // flicker once between Font size 15 (default) and back to the styleClass font size.
            groupTitle.fontProperty().removeListener(fontListener);
        });
        groupTitle.setText(group.getName());
        groupTitle.setOnKeyPressed(ke -> {
            if(ke.getCode() == KeyCode.ENTER) {
                rename.fire();
            }
        });

        rename.setOnAction(ae -> new Thread(() -> {
            if(editIcon.getImage().equals(close)) {
                try {
                    database.renameGroup(group, groupTitle.getText());
                } catch (SQLException e){
                    logger.error(e);
                }
            }
            Platform.runLater(() -> {
                if(editIcon.getImage().equals(edit)) {
                    editIcon.setImage(close);
                    groupTitle.getStyleClass().remove("clearTextField");
                    groupTitle.setEditable(true);
                    rename.setTooltip(new Tooltip("Save Changes"));
                } else {
                    editIcon.setImage(edit);
                    groupTitle.getStyleClass().add("clearTextField");
                    groupTitle.setEditable(false);
                    rename.setTooltip(new Tooltip("Rename"));
                }
            });
        }).start());

        SelectionModel selectionModel = groupItemList.getSelectionModel();
        ((MultipleSelectionModel) selectionModel).setSelectionMode(SelectionMode.MULTIPLE);
        ((MultipleSelectionModel) selectionModel).getSelectedItems().addListener((ListChangeListener)(lcl) -> {
            Platform.runLater(() -> {
                int items = lcl.getList().size();
                btnRemoveItems.setText(String.format("Remove (%s)", items));
                btnRemoveItems.setDisable(items <= 0);
            });
        });
        groupItemList.getItems().addListener((ListChangeListener)(lcl) -> {
           Platform.runLater(() -> {
               int items = lcl.getList().size();
               btnRemoveAll.setText(String.format("Remove All (%s)", items));
               btnRemoveAll.setDisable(items <= 0);
           });
        });

        new Thread(() -> {
            logger.debug("[Load] Loading details...");
            logger.debug("[Load] Grabbing GroupItems");
            List<GroupItem> groupItems = database.getGroupItems(this.group);
            List<MGMVGroupItemView> groupItemViews = groupItems.stream()
                    .map(MGMVGroupItemView::new).collect(Collectors.toList());
            logger.debug("[Load] Found "+groupItems.size()+" GroupItem(s)");
            Platform.runLater(() -> {
               groupItemList.getItems().addAll(groupItemViews);
            });
        }).start();

        /**
         * Refresh Modal
         */
        MGMVRefreshModal mgmvRefresh = new MGMVRefreshModal(group);
        refreshModal.setContent(mgmvRefresh);
        btnRefresh.setOnAction(ae -> Platform.runLater(() -> {
            mgmvRefresh.reset();
            refreshModal.setVisible(true);
        }));
        mgmvRefresh.getBtnClose().setOnAction(ae -> refreshModal.setVisible(false));
        mgmvRefresh.getErrorList().managedProperty().addListener((o, ov, nv) -> {
            if(nv) {
                Platform.runLater(() -> refreshModal.getModalContainer().setMaxWidth(420+250));
            } else {
                Platform.runLater(() -> refreshModal.getModalContainer().setMaxWidth(420));
            }
        });

        /**
         * Delete Modal
         */
        MGMVDeleteGroupModal mgmvDelete = new MGMVDeleteGroupModal(group);
        deleteModal.setContent(mgmvDelete);
        deleteModal.setDividerClass("horizontalDividerRed");
        btnDelete.setOnAction(ae -> Platform.runLater(() -> deleteModal.setVisible(true)));
        mgmvDelete.getBtnClose().setOnAction(ae -> deleteModal.setVisible(false));
    }

    /**
     * Source: https://stackoverflow.com/a/25643696/2650847
     * Modifies a TextField's preferred width based on it's text content.
     * @param field
     */
    private void resizeTextField(TextField field) {
        Platform.runLater(() -> {
            Text text = new Text(field.getText());
            text.setFont(field.getFont());
            double width = text.getLayoutBounds().getWidth()
                    + field.getPadding().getLeft() + field.getPadding().getRight()
                    + 3d;
            field.setPrefWidth(width);
            field.positionCaret(field.getCaretPosition());
        });
    }

}
