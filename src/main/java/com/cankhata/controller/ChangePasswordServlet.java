package com.cankhata.controller;

import com.cankhata.exception.AppException;
import com.cankhata.model.SessionUser;
import com.cankhata.service.AppContext;
import com.cankhata.util.RequestUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Any logged-in user (owner, delivery boy, customer) changes their own password. POST -> redirect -> GET. */
@WebServlet("/account/password")
public class ChangePasswordServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("saved", "1".equals(req.getParameter("saved")));
        forward(req, resp, "change-password.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        try {
            AppContext.AUTH.changeOwnPassword(actor, req.getParameter("oldPassword"), req.getParameter("newPassword"),
                    req.getParameter("confirm"), RequestUtil.clientIp(req));
            resp.sendRedirect(req.getContextPath() + "/account/password?saved=1");
        } catch (AppException e) {
            if (e.getStatus() >= 500) throw e;
            req.setAttribute("formError", e.getMessage());
            resp.setStatus(e.getStatus());
            forward(req, resp, "change-password.jsp");
        }
    }
}
