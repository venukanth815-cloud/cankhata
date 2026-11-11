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
import java.util.UUID;

/** Owner-only: one customer's full account (cans, deposit, balance) plus every action on it. POST -> redirect -> GET. */
@WebServlet("/owner/customer")
public class CustomerDetailServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp, Validator.id(req.getParameter("id")));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String ip = RequestUtil.clientIp(req);
        long id = Validator.id(req.getParameter("id"));
        String action = req.getParameter("action") == null ? "" : req.getParameter("action");
        try {
            switch (action) {
                case "addSub" -> AppContext.SUBSCRIPTIONS.add(actor, id, req.getParameter("canTypeId"), req.getParameter("qty"), req.getParameter("frequency"),
                        req.getParameterValues("days"), req.getParameter("start"), req.getParameter("end"), ip);
                case "endSub" -> AppContext.SUBSCRIPTIONS.end(actor, id, Validator.id(req.getParameter("subId")), ip);
                case "skip" -> AppContext.SUBSCRIPTIONS.skip(actor, id, Validator.id(req.getParameter("subId")), req.getParameter("date"), ip);
                case "unskip" -> AppContext.SUBSCRIPTIONS.unskip(actor, id, Validator.id(req.getParameter("skipId")), ip);
                case "deposit" -> AppContext.ACCOUNTS.recordDeposit(actor, id, req.getParameter("type"), req.getParameter("amount"), req.getParameter("cans"), ip);
                case "adjust" -> AppContext.ACCOUNTS.adjustCans(actor, id, Validator.id(req.getParameter("canTypeId")), req.getParameter("entryType"),
                        req.getParameter("qty"), req.getParameter("note"), ip);
                case "payment" -> AppContext.ACCOUNTS.recordPayment(actor, id, req.getParameter("amount"), req.getParameter("method"), req.getParameter("token"), ip);
                case "login" -> AppContext.ACCOUNTS.createCustomerLogin(actor, id, req.getParameter("password"), ip);
                case "resetLogin" -> AppContext.ACCOUNTS.resetCustomerPassword(actor, id, req.getParameter("password"), ip);
                default -> throw new com.cankhata.exception.ValidationException("Unknown action");
            }
            resp.sendRedirect(req.getContextPath() + "/owner/customer?id=" + id + "&saved=1");
        } catch (AppException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 403) throw e;
            req.setAttribute("formError", e.getMessage());
            resp.setStatus(e.getStatus());
            show(req, resp, id);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, long id) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        req.setAttribute("c", AppContext.ACCOUNTS.summary(actor, id));
        req.setAttribute("cans", AppContext.ACCOUNTS.cansHeld(actor, id));
        req.setAttribute("ledger", AppContext.ACCOUNTS.recentLedger(actor, id));
        req.setAttribute("payments", AppContext.ACCOUNTS.recentPayments(actor, id));
        req.setAttribute("deliveries", AppContext.ACCOUNTS.recentDeliveries(actor, id));
        req.setAttribute("invoices", AppContext.ACCOUNTS.invoices(actor, id));
        req.setAttribute("subs", AppContext.SUBSCRIPTIONS.list(actor, id));
        req.setAttribute("canTypes", AppContext.CATALOG.listCanTypes(actor));
        req.setAttribute("token", UUID.randomUUID().toString());   // one per page view: a double-click cannot save a payment twice
        req.setAttribute("today", java.time.LocalDate.now().toString());
        req.setAttribute("saved", "1".equals(req.getParameter("saved")));
        forward(req, resp, "customer-detail.jsp");
    }
}
