package com.cankhata.controller.api;

import com.cankhata.controller.BaseServlet;
import com.cankhata.model.SessionUser;
import com.cankhata.util.Json;
import com.cankhata.util.RequestUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** GET /api/me : who am I? (used by JavaScript screens later). */
@WebServlet("/api/me")
public class MeApiServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser u = RequestUtil.user(req);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", u.getId());
        body.put("name", u.getName());
        body.put("role", u.getRole().name());
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(Json.write(body));
    }
}
