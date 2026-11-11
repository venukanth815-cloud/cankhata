package com.cankhata.util;

import com.cankhata.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    @Test
    void phoneAcceptsCommonFormatsAndReturnsTenDigits() {
        assertEquals("9876543210", Validator.phone("9876543210"));
        assertEquals("9876543210", Validator.phone("+91 98765 43210"));
        assertEquals("9876543210", Validator.phone("09876543210"));
    }

    @Test
    void phoneRejectsBadNumbers() {
        assertThrows(ValidationException.class, () -> Validator.phone("12345"));
        assertThrows(ValidationException.class, () -> Validator.phone("5876543210")); // must start with 6-9
        assertThrows(ValidationException.class, () -> Validator.phone(null));
    }

    @Test
    void passwordNeedsLengthLetterAndDigit() {
        assertEquals("abcd1234", Validator.password("abcd1234"));
        assertThrows(ValidationException.class, () -> Validator.password("short1"));
        assertThrows(ValidationException.class, () -> Validator.password("onlyletters"));
        assertThrows(ValidationException.class, () -> Validator.password("12345678"));
        assertThrows(ValidationException.class, () -> Validator.password("a1" + "x".repeat(80)));
    }

    @Test
    void emailIsOptionalButMustBeValidWhenGiven() {
        assertNull(Validator.optionalEmail("  "));
        assertEquals("a@b.com", Validator.optionalEmail(" A@B.com "));
        assertThrows(ValidationException.class, () -> Validator.optionalEmail("not-an-email"));
    }

    @Test
    void pageIsClampedAndDefaultsToOne() {
        assertEquals(1, Validator.page(null));
        assertEquals(1, Validator.page("abc"));
        assertEquals(1, Validator.page("-5"));
        assertEquals(7, Validator.page("7"));
    }
}
