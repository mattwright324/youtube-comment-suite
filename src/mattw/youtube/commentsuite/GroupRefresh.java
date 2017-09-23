package mattw.youtube.commentsuite;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import mattw.youtube.commentsuite.io.ElapsedTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupRefresh extends Thread {
    private ElapsedTime elapsedTime = new ElapsedTime();
    private SimpleStringProperty refreshStatus = new SimpleStringProperty("Preparing...");
    private SimpleBooleanProperty refreshing = new SimpleBooleanProperty(false);
    private ExecutorService es = Executors.newCachedThreadPool();

    private Set<String> existingVideoIds = new HashSet<>();
    private Set<String> existingCommentIds = new HashSet<>();
    private Set<String> existingChannelIds = new HashSet<>();
    private List<GroupItem> existingGroupItems = new ArrayList<>();
    private List<CommentDatabase.GroupItemVideo> existingGIV = new ArrayList<>();

    public void run() {

    }

    public ElapsedTime getElapsedTime() { return elapsedTime; }
    public SimpleStringProperty refreshStatusProperty() { return refreshStatus; }
    public SimpleBooleanProperty refreshingProperty() { return refreshing; }
}
