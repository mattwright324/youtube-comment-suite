package mattw.youtube.commentsuite;

import javafx.beans.property.SimpleBooleanProperty;

public class Group {
    public static String NO_GROUP = "G000";

    private String groupId;
    private String name;
    public SimpleBooleanProperty refreshing = new SimpleBooleanProperty(false);

    /**
     * Use for database init.
     */
    public Group(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }

    public String getId() { return groupId; }
    public String getName() { return name; }

    public SimpleBooleanProperty refreshingProperty() { return refreshing; }
    public void setRefreshing(boolean ref) { refreshing.setValue(ref); }

    public String toString() { return name; }
    public int hashCode() { return groupId.hashCode(); }
    public boolean equals(Object o) { return o != null && o.hashCode() == hashCode(); }
}
