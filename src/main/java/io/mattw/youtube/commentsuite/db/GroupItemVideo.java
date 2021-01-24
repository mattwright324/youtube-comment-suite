package io.mattw.youtube.commentsuite.db;

public class GroupItemVideo {

    private String gitemId;
    private String videoId;

    public GroupItemVideo(String gitemId, String videoId) {
        this.gitemId = gitemId;
        this.videoId = videoId;
    }

    public String getGitemId() {
        return gitemId;
    }

    public String getVideoId() {
        return videoId;
    }

    public boolean equals(Object o) {
        if (o instanceof GroupItemVideo) {
            GroupItemVideo giv = (GroupItemVideo) o;
            return giv.gitemId.equals(gitemId) && giv.videoId.equals(videoId);
        }
        return false;
    }
}
