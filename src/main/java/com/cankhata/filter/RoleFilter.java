package com.cankhata.filter;

import com.cankhata.model.Role;
import com.cankhata.model.SessionUser;
import com.cankhata.util.RequestUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * Filter 4: "Are you ALLOWED here?" Rules are by URL prefix. The AuthFilter has already checked the login.
 * Services check again (for example a customer may only read their own balance), so a mistake here is not the only line of defence.
 */
public class RoleFilter implements Filter {

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;
        String path = RequestUtil.path(req);

        Set<Role> allowed = allowedRoles(path);
        SessionUser user = RequestUtil.user(req);
        if (allowed != null && (user == null || !allowed.contains(user.getRole()))) {
            if (user == null) {          // public page, nothing to check
                chain.doFilter(rq, rs);
                return;
            }
            if (RequestUtil.wantsJson(req)) {
                AuthFilter.json(resp, 403, "You are not allowed to do this");
            } else {
                req.setAttribute("errorStatus", 403);
                req.setAttribute("errorMessage", "You are not allowed to open this page.");
                resp.setStatus(403);
                req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
            }
            return;
        }
        chain.doFilter(rq, rs);
    }

    /** null = any logged-in role may open it. */
    static Set<Role> allowedRoles(String path) {
        if (path.startsWith("/owner/") || path.equals("/metrics")) return EnumSet.of(Role.OWNER);
        if (path.startsWith("/driver/")) return EnumSet.of(Role.DELIVERY_BOY, Role.OWNER);
        if (path.startsWith("/customer/")) return EnumSet.of(Role.CUSTOMER);
        return null;
    }
}
