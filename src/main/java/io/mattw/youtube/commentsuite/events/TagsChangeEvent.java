package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.db.YouTubeComment;

import java.util.List;

public class TagsChangeEvent {

    public List<YouTubeComment> comments;

    public TagsChangeEvent(List<YouTubeComment> comments) {
        this.comments = comments;
    }

    public List<YouTubeComment> getComments() {
        return comments;
    }

    public boolean wasChanged(YouTubeComment comment) {
        return comments.stream().anyMatch(changed -> comment.getId().equals(changed.getId()));
    }

    public void updateTags(YouTubeComment comment) {
        comments.stream().filter(changed -> comment.getId().equals(changed.getId()))
                .forEach(changed -> comment.setTags(changed.getTags()));
    }

}
