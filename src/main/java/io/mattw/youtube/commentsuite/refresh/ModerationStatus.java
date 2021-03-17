package io.mattw.youtube.commentsuite.refresh;

import org.apache.commons.lang3.EnumUtils;

import java.util.Map;

public enum ModerationStatus {
    PUBLISHED("published"),
    HELD_FOR_REVIEW("heldForReview");

    private static final Map<String, ModerationStatus> lookup = EnumUtils.getEnumMap(ModerationStatus.class);

    private final String apiValue;

    ModerationStatus(final String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    public static ModerationStatus fromName(final String name) {
        if (name == null) {
            return null;
        }

        return lookup.getOrDefault(name, null);
    }

    public static ModerationStatus fromApiValue(final String apiValue) {
        if (apiValue == null) {
            return null;
        }

        for (ModerationStatus status : ModerationStatus.values()) {
            if (apiValue.equals(status.getApiValue())) {
                return status;
            }
        }

        return null;
    }

}
