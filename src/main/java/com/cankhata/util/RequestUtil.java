package com.cankhata.util;

import com.cankhata.model.SessionUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Small helpers shared by filters and servlets. */
public final class RequestUtil {
    public static final String ATTR_USER = "user";
    public static final String ATTR_CSRF = "csrf";

    private RequestUtil() {}

    /** Path inside the app, e.g. "/owner/users" (context path and ;params removed). */
    public static String path(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        String p = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
        int semi = p.indexOf(';');
        if (semi >= 0) p = p.substring(0, semi);
        return p.isEmpty() ? "/" : p;
    }

    public static boolean wantsJson(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return path(req).startsWith("/api/") || path(req).equals("/metrics") || path(req).equals("/health")
                || (accept != null && accept.contains("application/json"));
    }

    public static SessionUser user(HttpServletRequest req) {
        return (SessionUser) req.getAttribute(ATTR_USER);
    }

    public static String cookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) return c.getValue();
        }
        return null;
    }

    public static void setSessionCookie(HttpServletRequest req, HttpServletResponse resp, String value, int maxAge) {
        Cookie c = new Cookie(SessionToken.COOKIE, value);
        c.setHttpOnly(true);                                   // JavaScript cannot read it
        c.setSecure(Config.getBool("COOKIE_SECURE", req.isSecure())); // only over HTTPS when true
        c.setPath("/");
        c.setMaxAge(maxAge);
        c.setAttribute("SameSite", "Lax");                     // not sent on cross-site POSTs
        resp.addCookie(c);
    }

    /** Client IP. Behind Render/Nginx the real IP is in X-Forwarded-For. */
    public static String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
