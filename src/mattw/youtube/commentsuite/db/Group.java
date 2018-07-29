package mattw.youtube.commentsuite.db;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class Group {
    public static String NO_GROUP = "G000";

    private String groupId;
    private SimpleStringProperty name = new SimpleStringProperty();
    private SimpleIntegerProperty itemsUpdated = new SimpleIntegerProperty(0); // Listen for change to regrab items.

    private List<GroupItem> groupItems = new ArrayList<>();

    /**
     * Used when creating a new group.
     */
    public Group(String name) {
        this.name.setValue(name);
        this.groupId = generateId();
    }

    /**
     * Use for database init.
     */
    public Group(String groupId, String name) {
        this.groupId = groupId;
        this.name.setValue(name);
    }

    private String generateId() {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(StandardCharsets.UTF_8.encode(String.valueOf(System.nanoTime())+this.name));
            return String.format("%032x", new BigInteger(1, md5.digest()));
        } catch (Exception e) {
            e.printStackTrace();
            return String.valueOf(System.nanoTime());
        }
    }

    public String getId() { return groupId; }

    public String getName() { return name.getValue(); }
    protected void setName(String name) { this.name.setValue(name); }
    public SimpleStringProperty nameProperty() { return name; }
    public SimpleIntegerProperty itemsUpdatedProperty() { return itemsUpdated; }
    public void incrementItemsUpdated() { itemsUpdated.setValue(itemsUpdated.getValue()+1); }

    public void reloadGroupItems() {
        groupItems.clear();
        // groupItems.addAll(CommentSuite.db().getGroupItems(this));
        incrementItemsUpdated();
    }

    public List<GroupItem> getGroupItems() { return groupItems; }

    public String toString() { return name.getValue(); }
    public int hashCode() { return groupId.hashCode(); }
    public boolean equals(Object o) { return o != null && o instanceof Group && o.hashCode() == hashCode(); }
}
