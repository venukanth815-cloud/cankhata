package com.cankhata.util;

import com.cankhata.exception.ValidationException;
import com.cankhata.model.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoneyValidatorTest {

    @Test
    void rupeesBecomePaiseWithoutFloatErrors() {
        assertEquals(4000, Validator.rupeesToPaise("40", "Price", false));
        assertEquals(4050, Validator.rupeesToPaise("40.5", "Price", false));
        assertEquals(4005, Validator.rupeesToPaise(" 40.05 ", "Price", false));
        assertEquals(0, Validator.rupeesToPaise("0", "Deposit", true));
    }

    @Test
    void badAmountsAreRejected() {
        assertThrows(ValidationException.class, () -> Validator.rupeesToPaise("0", "Price", false));
        assertThrows(ValidationException.class, () -> Validator.rupeesToPaise("-5", "Price", true));
        assertThrows(ValidationException.class, () -> Validator.rupeesToPaise("4.555", "Price", true));
        assertThrows(ValidationException.class, () -> Validator.rupeesToPaise("abc", "Price", true));
        assertThrows(ValidationException.class, () -> Validator.rupeesToPaise("", "Price", true));
        assertThrows(ValidationException.class, () -> Validator.rupeesToPaise("99999999", "Price", true));
    }

    @Test
    void optionalIdAndText() {
        assertNull(Validator.optionalId(" "));
        assertEquals(7L, Validator.optionalId("7").longValue());
        assertThrows(ValidationException.class, () -> Validator.optionalId("x"));
        assertThrows(ValidationException.class, () -> Validator.optionalId("-1"));
        assertNull(Validator.optionalText("  ", "Landmark", 10));
        assertThrows(ValidationException.class, () -> Validator.optionalText("x".repeat(11), "Landmark", 10));
    }

    @Test
    void moneyTextShowsRupeesAndPaise() {
        assertEquals("40.00", Money.text(4000));
        assertEquals("0.05", Money.text(5));
        assertEquals("-12.30", Money.text(-1230));
    }

    @Test
    void intRangeAndDate() {
        assertEquals(5, Validator.intInRange(" 5 ", "Qty", 1, 10));
        assertThrows(ValidationException.class, () -> Validator.intInRange("11", "Qty", 1, 10));
        assertThrows(ValidationException.class, () -> Validator.intInRange("x", "Qty", 1, 10));
        assertEquals(java.time.LocalDate.of(2026, 11, 7), Validator.date("2026-11-07", "Date"));
        assertThrows(ValidationException.class, () -> Validator.date("07/11/2026", "Date"));
    }
}
