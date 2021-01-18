package io.mattw.youtube.commentsuite.refresh;

public enum RefreshCommentPages {
    NONE("None", 0),
    ALL("All", Integer.MAX_VALUE),
    PAGES_1(1),
    PAGES_2(2),
    PAGES_3(3),
    PAGES_4(4),
    PAGES_5(5),
    PAGES_10(10),
    PAGES_15(15),
    PAGES_20(20),
    PAGES_25(25),
    PAGES_50(50),
    PAGES_75(75),
    PAGES_100(100),
    PAGES_125(125),
    PAGES_150(150),
    PAGES_175(175),
    PAGES_200(200),
    PAGES_250(250),
    PAGES_300(300),
    PAGES_400(400),
    PAGES_500(500)
    ;

    private String displayText;
    private int pageCount;

    RefreshCommentPages(String displayText, int pageCount) {
        this.displayText = displayText;
        this.pageCount = pageCount;
    }

    RefreshCommentPages(int pageCount) {
        this.displayText = String.valueOf(pageCount);
        this.pageCount = pageCount;
    }

    public String getDisplayText() {
        return displayText;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String toString() {
        return getDisplayText();
    }
}
