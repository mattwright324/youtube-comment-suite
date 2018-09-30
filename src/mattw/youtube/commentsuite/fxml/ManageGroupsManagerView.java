package mattw.youtube.commentsuite.fxml;

import static javafx.application.Platform.runLater;
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
import mattw.youtube.commentsuite.ImageLoader;
import mattw.youtube.commentsuite.db.CommentDatabase;
import mattw.youtube.commentsuite.db.Group;
import mattw.youtube.commentsuite.db.GroupItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
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
    private Image edit = ImageLoader.PENCIL.getImage();
    private Image close = ImageLoader.CLOSE.getImage();

    private ChangeListener<Font> fontListener;
    private Group group;

    private CommentDatabase database;

    private @FXML OverlayModal<MGMVRefreshModal> refreshModal;
    private @FXML OverlayModal deleteModal;
    private @FXML OverlayModal addItemModal;
    private @FXML OverlayModal removeItemModal;
    private @FXML OverlayModal removeAllModal;
    private @FXML Button btnAddItem;
    private @FXML Button btnRemoveItems;
    private @FXML Button btnRemoveAll;
    private @FXML ListView<MGMVGroupItemView> groupItemList;

    private @FXML TextField groupTitle;
    private @FXML ImageView editIcon;
    private @FXML Hyperlink rename;
    private @FXML Button btnRefresh;
    private @FXML Button btnReload;
    private @FXML Button btnDelete;
    private @FXML Label refreshStatus;

    private @FXML Accordion accordion;
    private @FXML TitledPane generalPane;

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

        accordion.setExpandedPane(generalPane);

        editIcon.setImage(edit);

        groupTitle.setMinWidth(Region.USE_PREF_SIZE);
        groupTitle.setMaxWidth(Region.USE_PREF_SIZE);
        groupTitle.textProperty().addListener((ov, prevText, currText) -> resizeTextField(groupTitle));
        groupTitle.fontProperty().addListener(fontListener = (o, ov, nv) -> {
            resizeTextField(groupTitle);
            // One-time font listener resize.
            // Will match content after font set on label from styleClass.
            // If not removed, when clicking the 'Rename' button, the label will
            // flicker once between Font size 15 (default) and back to the styleClass font size
            // every time the edit button is clicked.
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
            runLater(() -> {
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
            runLater(() -> {
                int items = lcl.getList().size();
                btnRemoveItems.setText(String.format("Remove (%s)", items));
                btnRemoveItems.setDisable(items <= 0);
            });
        });
        groupItemList.getItems().addListener((ListChangeListener<MGMVGroupItemView>)(lcl) -> {
           runLater(() -> {
               int items = lcl.getList().size();
               btnRemoveAll.setText(String.format("Remove All (%s)", items));
               btnRemoveAll.setDisable(items <= 0);
           });
        });

        reloadGroupItems();
        reload();

        /**
         * Refresh Modal
         */
        MGMVRefreshModal mgmvRefresh = new MGMVRefreshModal(group);
        refreshModal.setContent(mgmvRefresh);
        btnRefresh.setOnAction(ae -> runLater(() -> {
            mgmvRefresh.reset();
            refreshModal.setVisible(true);
        }));
        mgmvRefresh.getBtnClose().setOnAction(ae -> refreshModal.setVisible(false));
        mgmvRefresh.getErrorList().managedProperty().addListener((o, ov, nv) -> {
            if(nv) {
                runLater(() -> refreshModal.getModalContainer().setMaxWidth(420+250));
            } else {
                runLater(() -> refreshModal.getModalContainer().setMaxWidth(420));
            }
        });

        /**
         * Delete Group Modal
         */
        MGMVDeleteGroupModal mgmvDelete = new MGMVDeleteGroupModal(group);
        deleteModal.setContent(mgmvDelete);
        deleteModal.setDividerClass("dividerDanger");
        btnDelete.setOnAction(ae -> runLater(() -> deleteModal.setVisible(true)));
        mgmvDelete.getBtnClose().setOnAction(ae -> deleteModal.setVisible(false));

        /**
         * Add Item Modal
         */
        MGMVAddItemModal mgmvAddItem = new MGMVAddItemModal(group);
        addItemModal.setContent(mgmvAddItem);
        btnAddItem.setOnAction(ae -> runLater(() -> {
            mgmvAddItem.reset();
            addItemModal.setVisible(true);
        }));
        mgmvAddItem.getBtnClose().setOnAction(ae -> addItemModal.setVisible(false));
        mgmvAddItem.itemAddedProperty().addListener((o, ov, nv) -> reloadGroupItems());

        /**
         * Remove Selected GroupItems Modal
         */
        MGMVRemoveSelectedModal mgmvRemoveSelected = new MGMVRemoveSelectedModal(group, groupItemList.getSelectionModel());
        removeItemModal.setContent(mgmvRemoveSelected);
        removeItemModal.setDividerClass("dividerWarning");
        btnRemoveItems.setOnAction(ae -> runLater(() -> {
            mgmvRemoveSelected.reset();
            removeItemModal.setVisible(true);
        }));
        mgmvRemoveSelected.getBtnClose().setOnAction(ae -> removeItemModal.setVisible(false));

        /**
         * Remove All GroupItems Modal
         */
        MGMVRemoveAllModal mgmvRemoveAll = new MGMVRemoveAllModal(group);
        removeAllModal.setContent(mgmvRemoveAll);
        removeAllModal.setDividerClass("dividerDanger");
        btnRemoveAll.setOnAction(ae -> runLater(() -> {
            mgmvRemoveAll.reset();
            removeAllModal.setVisible(true);
        }));
        mgmvRemoveAll.getBtnClose().setOnAction(ae -> removeAllModal.setVisible(false));
    }

    private void reload() {
        long timestamp = database.getLastChecked(this.group);
        runLater(() ->  refreshStatus.setText(timestamp == Long.MAX_VALUE ? "Never refreshed." : String.valueOf(timestamp)));

        /*try {
            // TODO: Reload status content: stats, videos, viewers, etc.
        } catch (SQLException e) {
            logger.error("Error on data reload.");
        }*/
    }

    /**
     * Starts a thread to reload the GroupItems in the ListView.
     */
    private void reloadGroupItems() {
        new Thread(() -> {
            logger.debug("[Load] Grabbing GroupItems");
            List<GroupItem> groupItems = database.getGroupItems(this.group);
            List<MGMVGroupItemView> groupItemViews = groupItems.stream()
                    .map(MGMVGroupItemView::new).collect(Collectors.toList());
            logger.debug("[Load] Found "+groupItems.size()+" GroupItem(s)");
            runLater(() -> {
                groupItemList.getItems().clear();
                groupItemList.getItems().addAll(groupItemViews);
            });
        }).start();
    }

    /**
     * Source: https://stackoverflow.com/a/25643696/2650847
     * Modifies a TextField's preferred width based on it's text content.
     * @param field
     */
    private void resizeTextField(TextField field) {
        runLater(() -> {
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
