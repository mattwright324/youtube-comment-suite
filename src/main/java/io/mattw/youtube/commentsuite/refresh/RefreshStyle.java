package io.mattw.youtube.commentsuite.refresh;

public enum RefreshStyle {
    VIDEO_META_ONLY("Video Meta only", RefreshTimeframe.NONE, RefreshCommentPages.NONE, RefreshCommentOrder.TIME, RefreshReplyPages.NONE),
    SKIM("Skim", RefreshTimeframe.ALL, RefreshCommentPages.PAGES_1, RefreshCommentOrder.RELEVANCE, RefreshReplyPages.PAGES_1),
    LIGHT("Light", RefreshTimeframe.ALL, RefreshCommentPages.PAGES_5, RefreshCommentOrder.RELEVANCE, RefreshReplyPages.PAGES_5),
    MODERATE("Moderate", RefreshTimeframe.ALL, RefreshCommentPages.PAGES_25, RefreshCommentOrder.TIME, RefreshReplyPages.PAGES_25),
    HEAVY("Heavy", RefreshTimeframe.ALL, RefreshCommentPages.PAGES_50, RefreshCommentOrder.TIME, RefreshReplyPages.PAGES_50),
    EVERYTHING("Everything", RefreshTimeframe.ALL, RefreshCommentPages.ALL, RefreshCommentOrder.TIME, RefreshReplyPages.ALL),
    CUSTOM("Custom")
    ;

    private String displayText;
    private RefreshTimeframe timeframe;
    private RefreshCommentPages commentPages;
    private RefreshCommentOrder commentOrder;
    private RefreshReplyPages replyPages;

    RefreshStyle(String displayText) {
        this.displayText = displayText;
    }

    RefreshStyle(String displayText, RefreshTimeframe timeframe, RefreshCommentPages commentPages, RefreshCommentOrder commentOrder, RefreshReplyPages replyPages) {
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

    public RefreshReplyPages getReplyPages() {
        return replyPages;
    }

    public boolean matches(RefreshTimeframe timeframe, RefreshCommentPages commentPages, RefreshCommentOrder commentOrder, RefreshReplyPages replyPages) {
        return this.timeframe == timeframe && this.commentPages == commentPages && this.commentOrder == commentOrder && this.replyPages == replyPages;
    }

    public String toString() {
        return getDisplayText();
    }

}
