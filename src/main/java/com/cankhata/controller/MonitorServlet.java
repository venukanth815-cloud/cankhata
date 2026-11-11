package com.cankhata.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Owner-only: a readable live dashboard. The numbers come from the /metrics JSON, fetched by monitor.js every few seconds. */
@WebServlet("/owner/monitor")
public class MonitorServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        forward(req, resp, "monitor.jsp");
    }
}
