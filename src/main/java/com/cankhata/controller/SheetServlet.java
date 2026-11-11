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
import java.time.LocalDate;
import java.util.Map;

/** Owner-only daily sheet: generate from subscriptions, watch progress, record the van check, close the day. */
@WebServlet("/owner/sheet")
public class SheetServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp, day(req));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        String ip = RequestUtil.clientIp(req);
        LocalDate day = day(req);
        try {
            String action = req.getParameter("action") == null ? "" : req.getParameter("action");
            switch (action) {
                case "generate" -> AppContext.SHEETS.generate(actor, day, ip);
                case "close" -> AppContext.SHEETS.close(actor, Validator.id(req.getParameter("sheetId")), ip);
                case "van" -> AppContext.SHEETS.saveVan(actor, Validator.id(req.getParameter("sheetId")), Validator.id(req.getParameter("boyId")),
                        req.getParameter("fullOut"), req.getParameter("fullBack"), req.getParameter("emptiesIn"), ip);
                default -> throw new com.cankhata.exception.ValidationException("Unknown action");
            }
            resp.sendRedirect(req.getContextPath() + "/owner/sheet?date=" + day + "&saved=1");
        } catch (AppException e) {
            if (e.getStatus() >= 500 || e.getStatus() == 403) throw e;
            req.setAttribute("formError", e.getMessage());
            resp.setStatus(e.getStatus());
            show(req, resp, day);
        }
    }

    private static LocalDate day(HttpServletRequest req) {
        String d = req.getParameter("date");
        if (d == null || d.isBlank()) return LocalDate.now();
        return Validator.date(d, "Date");
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, LocalDate day) throws ServletException, IOException {
        SessionUser actor = RequestUtil.user(req);
        Map<String, Object> sheet = AppContext.SHEETS.find(actor, day);
        req.setAttribute("day", day.toString());
        req.setAttribute("sheet", sheet);
        if (sheet != null) {
            long id = ((Number) sheet.get("id")).longValue();
            req.setAttribute("rows", AppContext.SHEETS.deliveries(actor, id));
            req.setAttribute("vans", AppContext.SHEETS.vanCheck(actor, id));
            req.setAttribute("open", "OPEN".equals(sheet.get("status")));
        }
        req.setAttribute("saved", "1".equals(req.getParameter("saved")));
        forward(req, resp, "sheet.jsp");
    }
}
