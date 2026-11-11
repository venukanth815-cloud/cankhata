package com.cankhata.controller;

import com.cankhata.util.ErrorHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Every servlet extends this. service() runs doGet/doPost; anything thrown (our own AppException or a surprise)
 * is caught here and sent to the ONE ErrorHandler, so servlets stay short and never repeat try/catch.
 */
public abstract class BaseServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            super.service(req, resp);
        } catch (RuntimeException e) {
            ErrorHandler.handle(req, resp, e);
        }
    }

    protected void forward(HttpServletRequest req, HttpServletResponse resp, String view) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + view).forward(req, resp);
    }
}
