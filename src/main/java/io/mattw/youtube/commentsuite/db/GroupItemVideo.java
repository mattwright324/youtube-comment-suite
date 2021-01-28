package io.mattw.youtube.commentsuite.db;

public class GroupItemVideo {

    private final String gitemId;
    private final String videoId;

    public GroupItemVideo(final String gitemId, final String videoId) {
        this.gitemId = gitemId;
        this.videoId = videoId;
    }

    public String getGitemId() {
        return gitemId;
    }

    public String getVideoId() {
        return videoId;
    }

    public boolean equals(final Object o) {
        if (o instanceof GroupItemVideo) {
            final GroupItemVideo giv = (GroupItemVideo) o;
            return giv.gitemId.equals(gitemId) && giv.videoId.equals(videoId);
        }
        return false;
    }
}
