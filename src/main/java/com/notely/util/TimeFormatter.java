package com.notely.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Formats {@link LocalDateTime} values as human-readable relative
 * time strings such as "Just now", "5 minutes ago", or "Yesterday".
 */
public final class TimeFormatter {

    private TimeFormatter() {}

    public static String formatRelative(LocalDateTime dateTime) {
        return formatRelative(dateTime, LocalDateTime.now());
    }

    static String formatRelative(LocalDateTime dateTime, LocalDateTime now) {
        if (dateTime == null) {
            return "";
        }
        if (dateTime.isAfter(now)) {
            return "Just now";
        }

        long seconds = ChronoUnit.SECONDS.between(dateTime, now);
        if (seconds < 60) {
            return "Just now";
        }

        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes == 1) {
            return "1 minute ago";
        }
        if (minutes < 60) {
            return minutes + " minutes ago";
        }

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours == 1) {
            return "1 hour ago";
        }
        if (hours < 24) {
            return hours + " hours ago";
        }

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days == 1) {
            return "Yesterday";
        }
        if (days < 30) {
            return days + " days ago";
        }

        long months = days / 30;
        if (months < 12) {
            return months == 1 ? "1 month ago" : months + " months ago";
        }

        long years = days / 365;
        return years == 1 ? "1 year ago" : years + " years ago";
    }
}
