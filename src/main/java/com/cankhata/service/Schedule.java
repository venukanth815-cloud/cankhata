package com.cankhata.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pure date logic (no database) so it is easy to unit test: is a subscription due to be delivered on a given day?
 *  DAILY     - every day
 *  ALTERNATE - every 2nd day counted from the start date
 *  WEEKDAYS  - only on the days ticked in weekdayMask (bit 0 = Monday ... bit 6 = Sunday)
 */
public final class Schedule {
    public static final int ALL_DAYS = 127;

    private Schedule() {}

    public static boolean isDue(String frequency, int weekdayMask, LocalDate start, LocalDate end, LocalDate day) {
        if (day.isBefore(start)) return false;
        if (end != null && day.isAfter(end)) return false;
        switch (frequency) {
            case "DAILY":
                return true;
            case "ALTERNATE":
                return ChronoUnit.DAYS.between(start, day) % 2 == 0;
            case "WEEKDAYS":
                DayOfWeek dow = day.getDayOfWeek();
                return (weekdayMask & (1 << (dow.getValue() - 1))) != 0;
            default:
                return false;
        }
    }

    /** Builds the mask from checkbox values "1".."7" (1 = Monday). Empty means every day. */
    public static int maskFrom(String[] days) {
        if (days == null || days.length == 0) return ALL_DAYS;
        int mask = 0;
        for (String d : days) {
            try {
                int n = Integer.parseInt(d.trim());
                if (n >= 1 && n <= 7) mask |= 1 << (n - 1);
            } catch (NumberFormatException ignored) {
                // a tampered value is simply ignored
            }
        }
        return mask == 0 ? ALL_DAYS : mask;
    }
}
