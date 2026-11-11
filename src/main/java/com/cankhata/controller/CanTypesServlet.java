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

/** Owner-only: list can types, add a new one, or change price and deposit. */
@WebServlet("/owner/cantypes")
public class CanTypesServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String ip = RequestUtil.clientIp(req);
        try {
            if ("update".equals(req.getParameter("action"))) {
                AppContext.CATALOG.updateCanType(actor, req.getParameter("id"), req.getParameter("name"),
                        req.getParameter("price"), req.getParameter("deposit"), ip);
            } else {
                AppContext.CATALOG.addCanType(actor, req.getParameter("name"), req.getParameter("price"), req.getParameter("deposit"), ip);
            }
            resp.sendRedirect(req.getContextPath() + "/owner/cantypes?saved=1");
        } catch (AppException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 403) throw e;
            req.setAttribute("formError", e.getMessage());
            resp.setStatus(e.getStatus());
            show(req, resp);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("types", AppContext.CATALOG.listCanTypes(RequestUtil.user(req)));
        req.setAttribute("saved", "1".equals(req.getParameter("saved")));
        forward(req, resp, "cantypes.jsp");
    }
}
