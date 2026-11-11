package com.cankhata.model;

/** Formats paise as rupees for screens. */
public final class Money {
    private Money() {}

    public static String text(long paise) {
        long abs = Math.abs(paise);
        return (paise < 0 ? "-" : "") + (abs / 100) + "." + String.format("%02d", abs % 100);
    }
}
