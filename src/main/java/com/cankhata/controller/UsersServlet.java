package com.cankhata.controller;

import com.cankhata.exception.AppException;
import com.cankhata.model.SessionUser;
import com.cankhata.service.AppContext;
import com.cankhata.util.RequestUtil;
import com.cankhata.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Owner-only: list staff (search + pagination) and create a new owner / delivery boy.
 * Pattern used for every form: POST -> redirect -> GET (so Refresh never re-submits the form).
 */
@WebServlet("/owner/users")
public class UsersServlet extends BaseServlet {
    private static final int PAGE_SIZE = 20;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        showList(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        try {
            if ("reset".equals(req.getParameter("action"))) {
                AppContext.AUTH.resetPassword(actor, Validator.id(req.getParameter("userId")), req.getParameter("password"), RequestUtil.clientIp(req));
                resp.sendRedirect(req.getContextPath() + "/owner/users?reset=1");
                return;
            }
            AppContext.AUTH.createUser(actor, req.getParameter("name"), req.getParameter("phone"), req.getParameter("email"),
                    req.getParameter("password"), req.getParameter("role"), RequestUtil.clientIp(req));
            resp.sendRedirect(req.getContextPath() + "/owner/users?created=1");
        } catch (AppException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 403) throw e;
            req.setAttribute("formError", e.getMessage());
            req.setAttribute("form_name", req.getParameter("name"));
            req.setAttribute("form_phone", req.getParameter("phone"));
            req.setAttribute("form_email", req.getParameter("email"));
            req.setAttribute("form_role", req.getParameter("role"));
            resp.setStatus(e.getStatus());
            showList(req, resp);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String q = req.getParameter("q") == null ? "" : req.getParameter("q").trim();
        int page = Validator.page(req.getParameter("page"));

        long total = AppContext.AUTH.countUsers(actor, q);
        int pages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > pages) page = pages;

        req.setAttribute("users", AppContext.AUTH.listUsers(actor, q, page, PAGE_SIZE));
        req.setAttribute("q", q);
        req.setAttribute("page", page);
        req.setAttribute("pages", pages);
        req.setAttribute("total", total);
        req.setAttribute("created", "1".equals(req.getParameter("created")));
        req.setAttribute("resetDone", "1".equals(req.getParameter("reset")));
        forward(req, resp, "users.jsp");
    }
}
