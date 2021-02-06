package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.fxml.SearchCommentsListItem;

public class ViewTreeEvent {

    private final SearchCommentsListItem commentListItem;

    public ViewTreeEvent(final SearchCommentsListItem comment) {
        this.commentListItem = comment;
    }

    public SearchCommentsListItem getCommentListItem() {
        return commentListItem;
    }

}
