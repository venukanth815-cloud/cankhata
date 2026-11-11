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

/** Owner-only: list routes, add a route, assign a delivery boy to a route. */
@WebServlet("/owner/routes")
public class RoutesServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String ip = RequestUtil.clientIp(req);
        try {
            if ("assign".equals(req.getParameter("action"))) {
                AppContext.CATALOG.assignBoy(actor, req.getParameter("id"), req.getParameter("boyId"), ip);
            } else {
                AppContext.CATALOG.addRoute(actor, req.getParameter("name"), req.getParameter("boyId"), ip);
            }
            resp.sendRedirect(req.getContextPath() + "/owner/routes?saved=1");
        } catch (AppException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 403) throw e;
            req.setAttribute("formError", e.getMessage());
            resp.setStatus(e.getStatus());
            show(req, resp);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        req.setAttribute("routes", AppContext.CATALOG.listRoutes(actor));
        req.setAttribute("boys", AppContext.AUTH.listDeliveryBoys(actor));
        req.setAttribute("saved", "1".equals(req.getParameter("saved")));
        forward(req, resp, "routes.jsp");
    }
}
