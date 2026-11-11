package com.cankhata.util;

import com.cankhata.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Server-side validation. Browser checks can be bypassed, so every input is checked again here.
 * Each method either returns the cleaned value or throws ValidationException with a friendly message.
 */
public final class Validator {
    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Validator() {}

    public static String required(String value, String field, int maxLen) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) throw new ValidationException(field + " is required");
        if (v.length() > maxLen) throw new ValidationException(field + " must be at most " + maxLen + " characters");
        return v;
    }

    public static String name(String value) {
        String v = required(value, "Name", 100);
        if (v.length() < 2) throw new ValidationException("Name must be at least 2 characters");
        return v;
    }

    /** Accepts 9876543210, +91 98765 43210, 09876543210. Returns the plain 10 digits. */
    public static String phone(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        else if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        if (!PHONE.matcher(digits).matches()) throw new ValidationException("Enter a valid 10-digit mobile number");
        return digits;
    }

    /** Email is optional: blank returns null. */
    public static String optionalEmail(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (v.isEmpty()) return null;
        if (v.length() > 150 || !EMAIL.matcher(v).matches()) throw new ValidationException("Enter a valid email address");
        return v;
    }

    /** 8 to 72 characters (BCrypt ignores anything past 72), with at least one letter and one digit. */
    public static String password(String value) {
        if (value == null || value.length() < 8) throw new ValidationException("Password must be at least 8 characters");
        if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ValidationException("Password is too long (max 72 bytes)");
        boolean letter = false, digit = false;
        for (char c : value.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) throw new ValidationException("Password needs at least one letter and one digit");
        return value;
    }

    public static int page(String value) {
        try {
            int p = Integer.parseInt(value == null ? "1" : value.trim());
            return Math.max(1, Math.min(p, 100_000));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Rupees typed by a person ("40", "40.5", "40.50") to paise. Money is always stored as whole paise (no float errors). */
    public static long rupeesToPaise(String value, String field, boolean allowZero) {
        String v = value == null ? "" : value.trim();
        if (!v.matches("^\\d{1,7}(\\.\\d{1,2})?$")) throw new ValidationException(field + " must be an amount like 40 or 40.50");
        java.math.BigDecimal d = new java.math.BigDecimal(v).movePointRight(2);
        long paise = d.longValueExact();
        if (paise == 0 && !allowZero) throw new ValidationException(field + " must be more than 0");
        return paise;
    }

    /** Optional free text: blank returns null. */
    public static String optionalText(String value, String field, int maxLen) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return null;
        if (v.length() > maxLen) throw new ValidationException(field + " must be at most " + maxLen + " characters");
        return v;
    }

    /** Optional id from a form select: blank returns null. */
    public static Long optionalId(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) throw new ValidationException("Invalid selection");
            return id;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid selection");
        }
    }

    public static long id(String value) {
        Long id = optionalId(value);
        if (id == null) throw new ValidationException("Invalid id");
        return id;
    }

    /** Whole number between min and max (inclusive). */
    public static int intInRange(String value, String field, int min, int max) {
        try {
            int n = Integer.parseInt(value == null ? "" : value.trim());
            if (n < min || n > max) throw new ValidationException(field + " must be between " + min + " and " + max);
            return n;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a whole number");
        }
    }

    /** yyyy-MM-dd from a date input. */
    public static java.time.LocalDate date(String value, String field) {
        try {
            return java.time.LocalDate.parse(value == null ? "" : value.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new ValidationException(field + " must be a valid date");
        }
    }
}
