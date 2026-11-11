package com.cankhata.util;

import com.cankhata.model.Role;
import com.cankhata.model.SessionUser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

/**
 * Login "session" kept in a signed cookie instead of server memory.
 *
 * Token = base64(payload) + "." + base64(HMAC-SHA256(payload, secret))
 * payload = userId | role | expiryEpochSeconds | name
 *
 * Why: any Tomcat instance that knows the secret can verify it, so we can run several
 * instances behind a load balancer without sharing memory. Nobody can edit the cookie
 * (change role to OWNER) without knowing the secret, because the signature would not match.
 */
public final class SessionToken {
    public static final String COOKIE = "ck_session";
    public static final int TTL_SECONDS = 30 * 60;

    private static volatile byte[] key;

    private SessionToken() {}

    /** Called once at startup (and by tests). The secret must be long and random. */
    public static void init(String secret) {
        if (secret == null || secret.length() < 32)
            throw new IllegalStateException("SESSION_SECRET must be at least 32 characters");
        key = secret.getBytes(StandardCharsets.UTF_8);
    }

    public static String create(SessionUser u) {
        return create(u, System.currentTimeMillis() / 1000 + TTL_SECONDS);
    }

    static String create(SessionUser u, long expiryEpochSeconds) {
        String name = u.getName().replace("|", " ");
        String payload = u.getId() + "|" + u.getRole().name() + "|" + expiryEpochSeconds + "|" + name;
        String p = b64(payload.getBytes(StandardCharsets.UTF_8));
        return p + "." + b64(hmac(p));
    }

    public static Optional<SessionUser> parse(String token) {
        try {
            if (token == null || key == null) return Optional.empty();
            int dot = token.indexOf('.');
            if (dot <= 0) return Optional.empty();
            String p = token.substring(0, dot);
            byte[] given = Base64.getUrlDecoder().decode(token.substring(dot + 1));
            // constant-time comparison, so timing cannot reveal the signature
            if (!MessageDigest.isEqual(given, hmac(p))) return Optional.empty();
            String payload = new String(Base64.getUrlDecoder().decode(p), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", 4);
            if (parts.length != 4) return Optional.empty();
            if (Long.parseLong(parts[2]) < System.currentTimeMillis() / 1000) return Optional.empty(); // expired
            return Optional.of(new SessionUser(Long.parseLong(parts[0]), parts[3], Role.valueOf(parts[1])));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // bad base64, bad number or unknown role: treat as logged out
        }
    }

    static byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    static String b64(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** CSRF token tied to this login cookie. A forged form from another site cannot know it. */
    public static String csrfFor(String cookieValue) {
        return b64(hmac("csrf:" + cookieValue));
    }

    public static boolean csrfValid(String cookieValue, String given) {
        if (given == null || key == null) return false;
        return MessageDigest.isEqual(csrfFor(cookieValue).getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }
}
