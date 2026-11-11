package com.cankhata.controller;

import com.cankhata.exception.AppException;
import com.cankhata.model.SessionUser;
import com.cankhata.service.AppContext;
import com.cankhata.service.BillingService;
import com.cankhata.util.RequestUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.YearMonth;

/** Owner-only billing: generate a month's invoices, see who owes money. */
@WebServlet("/owner/invoices")
public class InvoicesServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp, month(req));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        YearMonth m = null;
        try {
            m = month(req);
            int n = AppContext.BILLING.generate(actor, m, RequestUtil.clientIp(req));
            resp.sendRedirect(req.getContextPath() + "/owner/invoices?month=" + m + "&created=" + n);
        } catch (AppException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 403) throw e;
            req.setAttribute("formError", e.getMessage());
            resp.setStatus(e.getStatus());
            show(req, resp, m == null ? YearMonth.now() : m);
        }
    }

    private static YearMonth month(HttpServletRequest req) {
        String m = req.getParameter("month");
        return m == null || m.isBlank() ? YearMonth.now() : BillingService.parseMonth(m);
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, YearMonth m) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        req.setAttribute("month", m.toString());
        req.setAttribute("invoices", AppContext.BILLING.forMonth(actor, m));
        req.setAttribute("owing", AppContext.BILLING.outstanding(actor));
        req.setAttribute("created", req.getParameter("created"));
        forward(req, resp, "invoices.jsp");
    }
}
