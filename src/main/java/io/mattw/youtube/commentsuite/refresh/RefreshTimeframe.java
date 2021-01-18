package io.mattw.youtube.commentsuite.refresh;

import java.time.Period;

public enum RefreshTimeframe {
    NONE("None"),
    ALL("All"),
    PAST_7_DAYS("past 7 days", Period.ofDays(7)),
    PAST_30_DAYS("past 30 days", Period.ofDays(30)),
    PAST_3_MONTHS("past 3 months", Period.ofMonths(3)),
    PAST_6_MONTHS("past 6 months", Period.ofMonths(6)),
    PAST_YEAR("past year", Period.ofYears(1)),
    PAST_2_YEARS("past 2 years", Period.ofYears(2)),
    PAST_3_YEARS("past 3 years", Period.ofYears(3)),
    PAST_5_YEARS("past 5 years", Period.ofYears(5)),
    PAST_10_YEARS("past 10 years", Period.ofYears(10)),
    ;

    private String displayText;
    private Period timeframe;

    RefreshTimeframe(String displayText) {
        this.displayText = displayText;
    }

    RefreshTimeframe(String displayText, Period timeframe) {
        this.displayText = displayText;
        this.timeframe = timeframe;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Period getTimeframe() {
        return timeframe;
    }

    public String toString() {
        return getDisplayText();
    }
}
