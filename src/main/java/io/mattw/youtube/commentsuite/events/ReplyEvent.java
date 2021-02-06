package io.mattw.youtube.commentsuite.events;

import io.mattw.youtube.commentsuite.fxml.SearchCommentsListItem;

public class ReplyEvent {

    private final SearchCommentsListItem commentListItem;

    public ReplyEvent(final SearchCommentsListItem comment) {
        this.commentListItem = comment;
    }

    public SearchCommentsListItem getCommentListItem() {
        return commentListItem;
    }

}
