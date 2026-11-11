package com.cankhata.service;

import com.cankhata.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleTest {
    private static final LocalDate MON = LocalDate.of(2026, 11, 2); // a Monday

    @Test
    void dailyIsDueEveryDayInsideTheDateRange() {
        assertTrue(Schedule.isDue("DAILY", 127, MON, null, MON));
        assertTrue(Schedule.isDue("DAILY", 127, MON, null, MON.plusDays(40)));
        assertFalse(Schedule.isDue("DAILY", 127, MON, null, MON.minusDays(1)));          // before start
        assertFalse(Schedule.isDue("DAILY", 127, MON, MON.plusDays(5), MON.plusDays(6))); // after end
        assertTrue(Schedule.isDue("DAILY", 127, MON, MON.plusDays(5), MON.plusDays(5)));  // end day included
    }

    @Test
    void alternateIsDueEverySecondDayFromStart() {
        assertTrue(Schedule.isDue("ALTERNATE", 127, MON, null, MON));
        assertFalse(Schedule.isDue("ALTERNATE", 127, MON, null, MON.plusDays(1)));
        assertTrue(Schedule.isDue("ALTERNATE", 127, MON, null, MON.plusDays(2)));
        assertFalse(Schedule.isDue("ALTERNATE", 127, MON, null, MON.plusDays(3)));
    }

    @Test
    void weekdaysUseTheMask() {
        int monAndThu = Schedule.maskFrom(new String[]{"1", "4"});
        assertTrue(Schedule.isDue("WEEKDAYS", monAndThu, MON, null, MON));              // Monday
        assertFalse(Schedule.isDue("WEEKDAYS", monAndThu, MON, null, MON.plusDays(1))); // Tuesday
        assertTrue(Schedule.isDue("WEEKDAYS", monAndThu, MON, null, MON.plusDays(3)));  // Thursday
        assertFalse(Schedule.isDue("WEEKDAYS", monAndThu, MON, null, MON.plusDays(6))); // Sunday
    }

    @Test
    void maskHandlesEmptyAndTamperedInput() {
        assertEquals(127, Schedule.maskFrom(null));
        assertEquals(127, Schedule.maskFrom(new String[0]));
        assertEquals(127, Schedule.maskFrom(new String[]{"x", "99"}));
        assertEquals(64, Schedule.maskFrom(new String[]{"7"}));
    }

    @Test
    void unknownFrequencyIsNeverDue() {
        assertFalse(Schedule.isDue("MONTHLY", 127, MON, null, MON));
    }

    @Test
    void monthParsingRejectsFutureAndGarbage() {
        assertEquals(YearMonth.of(2020, 1), BillingService.parseMonth("2020-01"));
        assertThrows(ValidationException.class, () -> BillingService.parseMonth("2999-01"));
        assertThrows(ValidationException.class, () -> BillingService.parseMonth("abc"));
        assertThrows(ValidationException.class, () -> BillingService.parseMonth(null));
    }
}
