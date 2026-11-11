package com.cankhata.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The database connection pool.
 *
 * Opening a MySQL connection takes tens of milliseconds and MySQL allows only ~150 at once.
 * So we open about 20 once, and every request BORROWS one and gives it back (close() returns it to the pool).
 * Always use try-with-resources so the connection is returned even when an error happens.
 */
public final class DbConnection {
    private static final Logger log = LoggerFactory.getLogger(DbConnection.class);
    private static volatile HikariDataSource ds;

    private DbConnection() {}

    public static synchronized void init() {
        if (ds != null) return;
        int size = Math.max(2, Config.getInt("POOL_SIZE", 20));

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("cankhata-pool");
        cfg.setJdbcUrl(Config.require("DB_URL"));
        // Name the driver explicitly. Inside Tomcat, DriverManager does not always find a driver that lives in WEB-INF/lib
        // ("No suitable driver"), but loading the class by name always works.
        cfg.setDriverClassName(Config.get("DB_DRIVER", "com.mysql.cj.jdbc.Driver"));
        cfg.setUsername(Config.require("DB_USER"));
        cfg.setPassword(Config.require("DB_PASSWORD"));
        cfg.setMaximumPoolSize(size);
        cfg.setMinimumIdle(Math.min(5, size));
        cfg.setConnectionTimeout(5_000);          // wait max 5 s for a free connection, then fail fast
        cfg.setMaxLifetime(25 * 60_000L);         // recycle connections before MySQL/Aiven closes them
        cfg.setLeakDetectionThreshold(20_000);    // log a warning if a connection is held over 20 s (forgotten close())
        cfg.setRegisterMbeans(true);              // lets VisualVM/JConsole show pool numbers
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(cfg);
        log.info("Database pool started, max {} connections", size);
    }

    public static Connection get() throws SQLException {
        HikariDataSource d = ds;
        if (d == null) throw new SQLException("Database pool is not started");
        return d.getConnection();
    }

    public static boolean ping() {
        try (Connection c = get()) {
            return c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public static HikariPoolMXBean pool() {
        HikariDataSource d = ds;
        return d == null ? null : d.getHikariPoolMXBean();
    }

    public static int maxPoolSize() {
        HikariDataSource d = ds;
        return d == null ? 0 : d.getMaximumPoolSize();
    }

    public static synchronized void close() {
        if (ds != null) {
            ds.close();
            ds = null;
            log.info("Database pool closed");
        }
    }
}
