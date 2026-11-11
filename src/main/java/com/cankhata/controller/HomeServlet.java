package com.cankhata.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** The dashboard. Which cards you see depends on your role (see home.jsp). */
@WebServlet("/home")
public class HomeServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        com.cankhata.model.SessionUser u = com.cankhata.util.RequestUtil.user(req);
        if (u != null && u.isOwner()) req.setAttribute("stats", com.cankhata.service.AppContext.BILLING.todayStats(u));
        forward(req, resp, "home.jsp");
    }
}
