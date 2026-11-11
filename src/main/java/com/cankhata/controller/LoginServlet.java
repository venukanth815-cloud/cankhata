package com.cankhata.controller;

import com.cankhata.exception.AppException;
import com.cankhata.model.SessionUser;
import com.cankhata.service.AppContext;
import com.cankhata.util.RequestUtil;
import com.cankhata.util.SessionToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /login shows the form. POST /login checks the password, sets the signed cookie, redirects to /home. */
@WebServlet("/login")
public class LoginServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionToken.parse(RequestUtil.cookie(req, SessionToken.COOKIE)).isPresent()) {
            resp.sendRedirect(req.getContextPath() + "/home");
            return;
        }
        forward(req, resp, "login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String login = req.getParameter("login");
        try {
            SessionUser user = AppContext.AUTH.login(login, req.getParameter("password"), RequestUtil.clientIp(req));
            RequestUtil.setSessionCookie(req, resp, SessionToken.create(user), SessionToken.TTL_SECONDS);
            resp.sendRedirect(req.getContextPath() + "/home");
        } catch (AppException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("login", login);          // refill the phone/email box, never the password
            resp.setStatus(e.getStatus());
            forward(req, resp, "login.jsp");
        }
    }
}
