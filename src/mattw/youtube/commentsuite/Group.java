package mattw.youtube.commentsuite;

import javafx.beans.property.SimpleBooleanProperty;

public class Group {
    private int id;
    private String name;
    public SimpleBooleanProperty refreshing = new SimpleBooleanProperty(false);

    public Group(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}
