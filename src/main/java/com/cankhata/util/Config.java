package com.cankhata.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads settings. Order: 1) environment variable, 2) db.properties on the classpath (local dev only), 3) default.
 * No password or secret is ever written in the code.
 */
public final class Config {
    private static final Properties FILE = new Properties();

    static {
        try (InputStream in = Config.class.getResourceAsStream("/db.properties")) {
            if (in != null) FILE.load(in);
        } catch (IOException ignored) {
            // no file is fine; environment variables are the normal way
        }
    }

    private Config() {}

    public static String get(String key, String def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = FILE.getProperty(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    public static String require(String key) {
        String v = get(key, null);
        if (v == null) throw new IllegalStateException("Missing required setting: " + key);
        return v;
    }

    public static int getInt(String key, int def) {
        String v = get(key, null);
        if (v == null) return def;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Setting " + key + " must be a number");
        }
    }

    public static boolean getBool(String key, boolean def) {
        String v = get(key, null);
        return v == null ? def : Boolean.parseBoolean(v);
    }
}
