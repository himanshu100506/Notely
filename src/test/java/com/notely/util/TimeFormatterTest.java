package com.notely.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeFormatterTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2025, 6, 15, 12, 0, 0);

    // ─────────────── Seconds ─────────────────────────────────

    @Test
    void justNowForFewSeconds() {
        LocalDateTime time = NOW.minusSeconds(30);
        assertEquals("Just now", TimeFormatter.formatRelative(time, NOW));
    }

    @Test
    void justNowForZeroSeconds() {
        assertEquals("Just now", TimeFormatter.formatRelative(NOW, NOW));
    }

    // ─────────────── Minutes ─────────────────────────────────

    @Test
    void oneMinuteAgo() {
        LocalDateTime time = NOW.minusMinutes(1);
        assertEquals("1 minute ago", TimeFormatter.formatRelative(time, NOW));
    }

    @Test
    void fiveMinutesAgo() {
        LocalDateTime time = NOW.minusMinutes(5);
        assertEquals("5 minutes ago", TimeFormatter.formatRelative(time, NOW));
    }

    @Test
    void fiftyNineMinutesAgo() {
        LocalDateTime time = NOW.minusMinutes(59);
        assertEquals("59 minutes ago", TimeFormatter.formatRelative(time, NOW));
    }

    // ─────────────── Hours ───────────────────────────────────

    @Test
    void oneHourAgo() {
        LocalDateTime time = NOW.minusHours(1);
        assertEquals("1 hour ago", TimeFormatter.formatRelative(time, NOW));
    }

    @Test
    void threeHoursAgo() {
        LocalDateTime time = NOW.minusHours(3);
        assertEquals("3 hours ago", TimeFormatter.formatRelative(time, NOW));
    }

    @Test
    void twentyThreeHoursAgo() {
        LocalDateTime time = NOW.minusHours(23);
        assertEquals("23 hours ago", TimeFormatter.formatRelative(time, NOW));
    }

    // ─────────────── Days ────────────────────────────────────

    @Test
    void yesterday() {
        LocalDateTime time = NOW.minusDays(1);
        assertEquals("Yesterday", TimeFormatter.formatRelative(time, NOW));
    }

    @Test
    void fiveDaysAgo() {
        LocalDateTime time = NOW.minusDays(5);
        assertEquals("5 days ago", TimeFormatter.formatRelative(time, NOW));
    }

    @Test
    void twentyNineDaysAgo() {
        LocalDateTime time = NOW.minusDays(29);
        assertEquals("29 days ago", TimeFormatter.formatRelative(time, NOW));
    }

    // ─────────────── Months ──────────────────────────────────

    @Test
    void oneMonthAgo() {
        LocalDateTime time = NOW.minusDays(30);
        assertEquals("1 month ago", TimeFormatter.formatRelative(time, NOW));
    }

    @Test
    void sixMonthsAgo() {
        LocalDateTime time = NOW.minusDays(180);
        assertEquals("6 months ago", TimeFormatter.formatRelative(time, NOW));
    }

    // ─────────────── Years ───────────────────────────────────

    @Test
    void oneYearAgo() {
        LocalDateTime time = NOW.minusDays(365);
        assertEquals("1 year ago", TimeFormatter.formatRelative(time, NOW));
    }

    @Test
    void twoYearsAgo() {
        LocalDateTime time = NOW.minusDays(730);
        assertEquals("2 years ago", TimeFormatter.formatRelative(time, NOW));
    }

    // ─────────────── Edge Cases ──────────────────────────────

    @Test
    void nullReturnsEmpty() {
        assertEquals("", TimeFormatter.formatRelative(null, NOW));
    }

    @Test
    void futureReturnsJustNow() {
        LocalDateTime future = NOW.plusHours(1);
        assertEquals("Just now", TimeFormatter.formatRelative(future, NOW));
    }

    @Test
    void publicMethodUsesSystemClock() {
        LocalDateTime recentPast = LocalDateTime.now().minusSeconds(10);
        assertEquals("Just now", TimeFormatter.formatRelative(recentPast));
    }
}
