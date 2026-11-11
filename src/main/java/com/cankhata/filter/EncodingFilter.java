package com.cankhata.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Filter 1: UTF-8 for Tamil text, plus security headers on every response.
 * A Filter runs BEFORE the servlet, for every matching URL.
 */
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");                 // nobody can put our pages in an iframe (clickjacking)
        resp.setHeader("Referrer-Policy", "same-origin");
        resp.setHeader("Content-Security-Policy",
                "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'");
        String path = req.getRequestURI();
        if (!path.contains("/assets/")) {
            resp.setHeader("Cache-Control", "no-store");           // pages with personal data must not be cached
        } else {
            resp.setHeader("Cache-Control", "public, max-age=86400"); // CSS/JS cached for a day
        }
        chain.doFilter(rq, rs);
    }
}
