package com.cankhata.util;

import com.cankhata.exception.AppException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ONE place that turns an exception into a response.
 * API/JSON callers get {"error": "...", "status": 409}; browsers get the friendly error page.
 * Unexpected errors are logged with a stack trace but the user only sees a safe message.
 */
public final class ErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    private ErrorHandler() {}

    public static void handle(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException, ServletException {
        int status;
        String message;
        if (e instanceof AppException ae) {
            status = ae.getStatus();
            message = ae.getMessage();
            if (status >= 500) log.error("Request failed: {}", message, e);
        } else {
            status = 500;
            message = "Something went wrong. Please try again.";
            log.error("Unexpected error on {} {}", req.getMethod(), RequestUtil.path(req), e);
        }
        if (resp.isCommitted()) return;

        if (RequestUtil.wantsJson(req)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", message);
            body.put("status", status);
            resp.setStatus(status);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(Json.write(body));
        } else {
            resp.setStatus(status);
            req.setAttribute("errorStatus", status);
            req.setAttribute("errorMessage", message);
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
