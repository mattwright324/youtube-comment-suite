package io.mattw.youtube.commentsuite.refresh;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SIMPLE_STYLE;

public class RefreshOptions {

    private RefreshStyle style = RefreshStyle.MODERATE;
    private RefreshTimeframe timeframe = style.getTimeframe();
    private RefreshCommentPages commentPages = style.getCommentPages();
    private RefreshCommentOrder commentOrder = style.getCommentOrder();
    private RefreshCommentPages replyPages = style.getReplyPages();
    private RefreshCommentPages reviewPages = RefreshCommentPages.ALL;

    public RefreshStyle getStyle() {
        return style;
    }

    public void setStyle(RefreshStyle style) {
        this.style = style;
    }

    public RefreshTimeframe getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(RefreshTimeframe timeframe) {
        this.timeframe = timeframe;
    }

    public RefreshCommentPages getCommentPages() {
        return commentPages;
    }

    public void setCommentPages(RefreshCommentPages commentPages) {
        this.commentPages = commentPages;
    }

    public RefreshCommentOrder getCommentOrder() {
        return commentOrder;
    }

    public void setCommentOrder(RefreshCommentOrder commentOrder) {
        this.commentOrder = commentOrder;
    }

    public RefreshCommentPages getReplyPages() {
        return replyPages;
    }

    public void setReplyPages(RefreshCommentPages replyPages) {
        this.replyPages = replyPages;
    }

    public RefreshCommentPages getReviewPages() {
        return reviewPages;
    }

    public void setReviewPages(RefreshCommentPages reviewPages) {
        this.reviewPages = reviewPages;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SIMPLE_STYLE);
    }
}
