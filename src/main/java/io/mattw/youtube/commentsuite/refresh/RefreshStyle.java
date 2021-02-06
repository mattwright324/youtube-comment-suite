package io.mattw.youtube.commentsuite.refresh;

public enum RefreshStyle {
    VIDEO_META_ONLY("Video Meta only", RefreshTimeframe.NONE, RefreshCommentPages.NONE, RefreshCommentOrder.TIME, RefreshCommentPages.NONE),
    SKIM("Skim", RefreshTimeframe.ALL, RefreshCommentPages.PAGES_1, RefreshCommentOrder.RELEVANCE, RefreshCommentPages.PAGES_1),
    LIGHT("Light", RefreshTimeframe.ALL, RefreshCommentPages.PAGES_5, RefreshCommentOrder.RELEVANCE, RefreshCommentPages.PAGES_5),
    MODERATE("Moderate", RefreshTimeframe.ALL, RefreshCommentPages.PAGES_25, RefreshCommentOrder.TIME, RefreshCommentPages.PAGES_25),
    HEAVY("Heavy", RefreshTimeframe.ALL, RefreshCommentPages.PAGES_50, RefreshCommentOrder.TIME, RefreshCommentPages.PAGES_50),
    EVERYTHING("Everything", RefreshTimeframe.ALL, RefreshCommentPages.ALL, RefreshCommentOrder.TIME, RefreshCommentPages.ALL),
    CUSTOM("Custom")
    ;

    private String displayText;
    private RefreshTimeframe timeframe;
    private RefreshCommentPages commentPages;
    private RefreshCommentOrder commentOrder;
    private RefreshCommentPages replyPages;
    private RefreshCommentPages reviewPages;

    RefreshStyle(String displayText) {
        this.displayText = displayText;
    }

    RefreshStyle(String displayText, RefreshTimeframe timeframe, RefreshCommentPages commentPages, RefreshCommentOrder commentOrder, RefreshCommentPages replyPages) {
        this.displayText = displayText;
        this.timeframe = timeframe;
        this.commentPages = commentPages;
        this.commentOrder = commentOrder;
        this.replyPages = replyPages;
    }

    public String getDisplayText() {
        return displayText;
    }

    public RefreshTimeframe getTimeframe() {
        return timeframe;
    }

    public RefreshCommentPages getCommentPages() {
        return commentPages;
    }

    public RefreshCommentOrder getCommentOrder() {
        return commentOrder;
    }

    public RefreshCommentPages getReplyPages() {
        return replyPages;
    }

    public RefreshCommentPages getReviewPages() {
        return reviewPages;
    }

    public boolean matches(RefreshTimeframe timeframe, RefreshCommentPages commentPages, RefreshCommentOrder commentOrder, RefreshCommentPages replyPages) {
        return this.timeframe == timeframe && this.commentPages == commentPages && this.commentOrder == commentOrder && this.replyPages == replyPages;
    }

    public String toString() {
        return getDisplayText();
    }

}
