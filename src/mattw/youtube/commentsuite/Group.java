package mattw.youtube.commentsuite;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Group {
    public static String NO_GROUP = "G000";

    private String groupId;
    public SimpleStringProperty name = new SimpleStringProperty();
    public SimpleBooleanProperty refreshing = new SimpleBooleanProperty(false);
    public SimpleIntegerProperty itemsUpdatedProperty = new SimpleIntegerProperty(0); // Listen for change to regrab items.

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

    public SimpleBooleanProperty refreshingProperty() { return refreshing; }
    public void setRefreshing(boolean ref) { refreshing.setValue(ref); }

    public String toString() { return name.getValue(); }
    public int hashCode() { return groupId.hashCode(); }
    public boolean equals(Object o) { return o != null && o.hashCode() == hashCode(); }
}
