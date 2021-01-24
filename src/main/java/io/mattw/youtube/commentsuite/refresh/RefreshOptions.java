package io.mattw.youtube.commentsuite.refresh;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SIMPLE_STYLE;

public class RefreshOptions {

    private RefreshStyle style = RefreshStyle.MODERATE;
    private RefreshTimeframe timeframe = style.getTimeframe();
    private RefreshCommentPages commentPages = style.getCommentPages();
    private RefreshCommentOrder commentOrder = style.getCommentOrder();
    private RefreshReplyPages replyPages = style.getReplyPages();

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

    public RefreshReplyPages getReplyPages() {
        return replyPages;
    }

    public void setReplyPages(RefreshReplyPages replyPages) {
        this.replyPages = replyPages;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SIMPLE_STYLE);
    }
}
