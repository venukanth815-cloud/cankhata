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

/** Customer's own page: cans held, deposit, balance, recent deliveries, messages, and skipping a delivery day. */
@WebServlet("/customer/home")
public class PortalServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String ip = RequestUtil.clientIp(req);
        try {
            long cid = AppContext.ACCOUNTS.customerIdOf(actor);
            if ("unskip".equals(req.getParameter("action")))
                AppContext.SUBSCRIPTIONS.unskip(actor, cid, Validator.id(req.getParameter("skipId")), ip);
            else
                AppContext.SUBSCRIPTIONS.skip(actor, cid, Validator.id(req.getParameter("subId")), req.getParameter("date"), ip);
            resp.sendRedirect(req.getContextPath() + "/customer/home?saved=1");
        } catch (AppException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 403) throw e;
            req.setAttribute("formError", e.getMessage());
            resp.setStatus(e.getStatus());
            show(req, resp);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        long cid = AppContext.ACCOUNTS.customerIdOf(actor);
        req.setAttribute("c", AppContext.ACCOUNTS.summary(actor, cid));
        req.setAttribute("cans", AppContext.ACCOUNTS.cansHeld(actor, cid));
        req.setAttribute("deliveries", AppContext.ACCOUNTS.recentDeliveries(actor, cid));
        req.setAttribute("payments", AppContext.ACCOUNTS.recentPayments(actor, cid));
        req.setAttribute("invoices", AppContext.ACCOUNTS.invoices(actor, cid));
        req.setAttribute("subs", AppContext.SUBSCRIPTIONS.list(actor, cid));
        req.setAttribute("tomorrow", java.time.LocalDate.now().plusDays(1).toString());
        req.setAttribute("messages", AppContext.NOTIFICATIONS.unread(actor));
        AppContext.NOTIFICATIONS.markAllRead(actor);
        req.setAttribute("saved", "1".equals(req.getParameter("saved")));
        forward(req, resp, "portal.jsp");
    }
}
