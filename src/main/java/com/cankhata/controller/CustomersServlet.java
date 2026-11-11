package com.cankhata.controller;

import com.cankhata.exception.AppException;
import com.cankhata.model.Customer;
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
 * Owner-only customer screen: GET /owner/customers (list, search, route filter, paging),
 * GET ?edit=ID (same page with the edit form filled), POST (create or update, then redirect).
 */
@WebServlet("/owner/customers")
public class CustomersServlet extends BaseServlet {
    private static final int PAGE_SIZE = 20;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String edit = req.getParameter("edit");
        if (edit != null && !edit.isBlank()) {
            Customer c = AppContext.CUSTOMERS.get(actor, Validator.id(edit));
            req.setAttribute("form", c);
            req.setAttribute("form_rate", c.getRateText());
            req.setAttribute("form_credit", c.getCreditLimitText());
        }
        show(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String ip = RequestUtil.clientIp(req);
        Customer form = new Customer();
        String idText = req.getParameter("id");
        try {
            if (idText != null && !idText.isBlank()) form.setId(Validator.id(idText));
            form.setName(req.getParameter("name"));
            form.setPhone(req.getParameter("phone"));
            form.setAddress(req.getParameter("address"));
            form.setLandmark(req.getParameter("landmark"));
            form.setRouteId(Validator.optionalId(req.getParameter("routeId")));
            form.setActive(req.getParameter("active") != null);
            if (form.getId() > 0) {
                AppContext.CUSTOMERS.update(actor, form, req.getParameter("rate"), req.getParameter("credit"), ip);
            } else {
                AppContext.CUSTOMERS.create(actor, form, req.getParameter("rate"), req.getParameter("credit"), ip);
            }
            resp.sendRedirect(req.getContextPath() + "/owner/customers?saved=1");
        } catch (AppException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 403) throw e;
            req.setAttribute("formError", e.getMessage());
            req.setAttribute("form", form);
            req.setAttribute("form_rate", req.getParameter("rate"));
            req.setAttribute("form_credit", req.getParameter("credit"));
            resp.setStatus(e.getStatus());
            show(req, resp);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String q = req.getParameter("q") == null ? "" : req.getParameter("q").trim();
        Long routeId;
        try {
            routeId = Validator.optionalId(req.getParameter("route"));
        } catch (AppException e) {
            routeId = null;
        }
        int page = Validator.page(req.getParameter("page"));
        long total = AppContext.CUSTOMERS.count(actor, q, routeId);
        int pages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > pages) page = pages;

        req.setAttribute("customers", AppContext.CUSTOMERS.list(actor, q, routeId, page, PAGE_SIZE));
        req.setAttribute("routes", AppContext.CATALOG.listRoutes(actor));
        req.setAttribute("q", q);
        req.setAttribute("routeFilter", routeId);
        req.setAttribute("page", page);
        req.setAttribute("pages", pages);
        req.setAttribute("total", total);
        req.setAttribute("saved", "1".equals(req.getParameter("saved")));
        forward(req, resp, "customers.jsp");
    }
}
