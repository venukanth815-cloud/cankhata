package com.cankhata.controller;

import com.cankhata.service.AppContext;
import com.cankhata.util.Config;
import com.cankhata.util.DbConnection;
import com.cankhata.util.SchemaInitializer;
import com.cankhata.util.SessionToken;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs once when Tomcat starts the app (and once when it stops):
 * start the DB pool, create tables, create the first owner, load the session secret.
 * If a setting is missing the app refuses to start, with a clear message in the log.
 */
@WebListener
public class AppInitListener implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(AppInitListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            SessionToken.init(Config.require("SESSION_SECRET"));
            DbConnection.init();
            SchemaInitializer.run();
            AppContext.AUTH.bootstrapOwner();
            log.info("CanKhata started");
        } catch (RuntimeException e) {
            log.error("CanKhata failed to start: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DbConnection.close();
    }
}
