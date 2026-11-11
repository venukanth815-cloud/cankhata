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

/** Delivery boy's phone screen: today's stops, big buttons, mark delivered (with empties and cash) or missed. */
@WebServlet("/driver/today")
public class DriverServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String ip = RequestUtil.clientIp(req);
        try {
            long id = Validator.id(req.getParameter("id"));
            if ("missed".equals(req.getParameter("action"))) AppContext.DELIVERIES.markMissed(actor, id, req.getParameter("reason"), ip);
            else AppContext.DELIVERIES.complete(actor, id, req.getParameter("delivered"), req.getParameter("empties"), req.getParameter("cash"), req.getParameter("note"), ip);
            resp.sendRedirect(req.getContextPath() + "/driver/today?saved=1");
        } catch (AppException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 403) throw e;
            req.setAttribute("formError", e.getMessage());
            resp.setStatus(e.getStatus());
            show(req, resp);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("rows", AppContext.DELIVERIES.today(RequestUtil.user(req)));
        req.setAttribute("saved", "1".equals(req.getParameter("saved")));
        forward(req, resp, "driver.jsp");
    }
}
