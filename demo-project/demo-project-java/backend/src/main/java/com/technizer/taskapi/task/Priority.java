package com.technizer.taskapi.task;

import java.util.Locale;

public enum Priority {

    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    /**
     * Parses a priority name, ignoring case and surrounding whitespace.
     *
     * @throws IllegalArgumentException if the text is null, blank or not one of Low, Medium, High
     */
    public static Priority parse(String text) {
        if (text != null) {
            String normalized = text.trim().toUpperCase(Locale.ROOT);
            for (Priority priority : values()) {
                if (priority.name().equals(normalized)) {
                    return priority;
                }
            }
        }
        throw new IllegalArgumentException("Priority must be one of: Low, Medium, High");
    }
}
