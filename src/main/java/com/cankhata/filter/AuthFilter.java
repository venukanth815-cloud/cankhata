package com.cankhata.filter;

import com.cankhata.model.SessionUser;
import com.cankhata.util.RequestUtil;
import com.cankhata.util.SessionToken;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Filter 3: "Are you logged in?"
 *  - reads the signed cookie, puts the user into request attributes for servlets and JSPs
 *  - public pages (login, health, css/js) are allowed without login
 *  - everything else: not logged in -> login page (or 401 JSON for API calls)
 *  - every POST/PUT/DELETE must carry a valid CSRF token (stops other websites from submitting our forms for you)
 */
public class AuthFilter implements Filter {
    private static final Set<String> PUBLIC_EXACT = Set.of("/", "/index.jsp", "/login", "/health");
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;
        String path = RequestUtil.path(req);

        String cookie = RequestUtil.cookie(req, SessionToken.COOKIE);
        SessionUser user = SessionToken.parse(cookie).orElse(null);
        if (user != null) {
            req.setAttribute(RequestUtil.ATTR_USER, user);
            req.setAttribute(RequestUtil.ATTR_CSRF, SessionToken.csrfFor(cookie));
        }

        boolean isPublic = PUBLIC_EXACT.contains(path) || path.startsWith("/assets/");
        if (isPublic) {
            chain.doFilter(rq, rs);
            return;
        }

        if (user == null) {
            if (RequestUtil.wantsJson(req)) {
                json(resp, 401, "Please log in");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login");
            }
            return;
        }

        if (!SAFE_METHODS.contains(req.getMethod())) {
            String given = req.getParameter("_csrf");
            if (given == null) given = req.getHeader("X-CSRF-Token");
            if (!SessionToken.csrfValid(cookie, given)) {
                json(resp, 403, "Security check failed. Reload the page and try again.");
                return;
            }
        }
        chain.doFilter(rq, rs);
    }

    static void json(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"error\":\"" + message + "\",\"status\":" + status + "}");
    }
}
