package com.cankhata.controller.api;

import com.cankhata.controller.BaseServlet;
import com.cankhata.util.DbConnection;
import com.cankhata.util.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** GET /health : used by Render / Docker / uptime monitors. 200 = app and database are fine, 503 = database is down. */
@WebServlet("/health")
public class HealthServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean db = DbConnection.ping();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", db ? "UP" : "DOWN");
        body.put("database", db ? "UP" : "DOWN");
        resp.setStatus(db ? 200 : 503);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(Json.write(body));
    }
}
