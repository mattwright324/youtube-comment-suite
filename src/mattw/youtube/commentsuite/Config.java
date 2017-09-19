package mattw.youtube.commentsuite;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Config {

    public final ObservableList<YouTubeAccount> accountsList = FXCollections.observableArrayList();
    private String file;

    public Config(String file) {
        this.file = file;
    }

}
