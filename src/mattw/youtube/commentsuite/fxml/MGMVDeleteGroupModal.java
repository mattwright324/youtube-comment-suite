package mattw.youtube.commentsuite.fxml;

import javafx.scene.layout.VBox;
import mattw.youtube.commentsuite.db.Group;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MGMVDeleteGroupModal extends VBox {

    private static Logger logger = LogManager.getLogger(MGMVDeleteGroupModal.class.getName());

    private Group group;

    public MGMVDeleteGroupModal(Group group) {
        this.group = group;
    }

}
