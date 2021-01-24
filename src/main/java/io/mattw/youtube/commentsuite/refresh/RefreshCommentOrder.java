package io.mattw.youtube.commentsuite.refresh;

public enum RefreshCommentOrder {
    TIME("newest first"),
    RELEVANCE("top comments")
    ;

    private String displayText;

    RefreshCommentOrder(String displayText) {
        this.displayText = displayText;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String toString() {
        return getDisplayText();
    }

}
