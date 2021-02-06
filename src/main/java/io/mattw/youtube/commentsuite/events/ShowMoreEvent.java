package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.fxml.SearchCommentsListItem;

public class ShowMoreEvent {

    private final SearchCommentsListItem commentListItem;

    public ShowMoreEvent(final SearchCommentsListItem comment) {
        this.commentListItem = comment;
    }

    public SearchCommentsListItem getCommentListItem() {
        return commentListItem;
    }

}
