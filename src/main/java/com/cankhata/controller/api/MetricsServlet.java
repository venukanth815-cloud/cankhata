package com.cankhata.controller.api;

import com.cankhata.controller.BaseServlet;
import com.cankhata.util.Json;
import com.cankhata.util.Metrics;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /metrics (owner only, enforced by RoleFilter): memory, CPU, threads, GC, DB pool, traffic, p95 latency. */
@WebServlet("/metrics")
public class MetricsServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(Json.write(Metrics.snapshot()));
    }
}
