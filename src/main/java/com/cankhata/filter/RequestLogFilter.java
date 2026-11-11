package com.cankhata.filter;

import com.cankhata.model.SessionUser;
import com.cankhata.util.Metrics;
import com.cankhata.util.RequestUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter 2: gives every request an id, times it, logs one line and updates /metrics numbers.
 * Log line: "GET /owner/users 200 35ms user=1". Slow requests (over 1 s) are logged as WARN.
 * We log the path only, never the query string or form values, so passwords and tokens cannot leak into logs.
 */
public class RequestLogFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger("com.cankhata.access");

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;
        String path = RequestUtil.path(req);
        boolean asset = path.startsWith("/assets/");

        String rid = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("rid", rid);                       // every log line in this request now carries [rid]
        resp.setHeader("X-Request-Id", rid);
        long start = System.nanoTime();
        try {
            chain.doFilter(rq, rs);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            int status = resp.getStatus();
            if (!asset) {
                SessionUser u = RequestUtil.user(req);
                String line = req.getMethod() + " " + path + " " + status + " " + ms + "ms user=" + (u == null ? "-" : u.getId());
                if (ms > 1000) log.warn("SLOW {}", line);
                else if (status >= 500) log.error(line);
                else log.info(line);
                Metrics.record(status, ms);
            }
            MDC.remove("rid");
        }
    }
}
