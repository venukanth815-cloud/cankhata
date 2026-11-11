package com.cankhata.util;

import com.cankhata.exception.BusinessRuleException;
import com.cankhata.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Tiny JDBC helper for the accounting screens. Every call uses PreparedStatement (?) so SQL injection is not possible.
 * Rows come back as Map (column label -> value), which JSP can read directly: ${row.name}.
 * tx() runs several statements in ONE transaction: all succeed, or none are saved.
 */
public final class Sql {
    private Sql() {}

    public static List<Map<String, Object>> query(String sql, Object... params) {
        try (Connection c = DbConnection.get()) {
            return query(c, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Sql.query", e);
        }
    }

    public static List<Map<String, Object>> query(Connection c, String sql, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Sql.query", e);
        }
    }

    public static Map<String, Object> one(String sql, Object... params) {
        List<Map<String, Object>> rows = query(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static Map<String, Object> one(Connection c, String sql, Object... params) {
        List<Map<String, Object>> rows = query(c, sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static long number(Connection c, String sql, Object... params) {
        Map<String, Object> r = one(c, sql, params);
        if (r == null) return 0;
        Object v = r.values().iterator().next();
        return v == null ? 0 : ((Number) v).longValue();
    }

    public static long number(String sql, Object... params) {
        try (Connection c = DbConnection.get()) {
            return number(c, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Sql.number", e);
        }
    }

    /** INSERT / UPDATE / DELETE. A duplicate or broken foreign key becomes a friendly 409 with the given message. */
    public static int update(Connection c, String conflictMessage, String sql, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BusinessRuleException(conflictMessage);
        } catch (SQLException e) {
            throw new DataAccessException("Sql.update", e);
        }
    }

    public static int update(String conflictMessage, String sql, Object... params) {
        return tx(c -> update(c, conflictMessage, sql, params));
    }

    public static long insert(Connection c, String conflictMessage, String sql, Object... params) {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BusinessRuleException(conflictMessage);
        } catch (SQLException e) {
            throw new DataAccessException("Sql.insert", e);
        }
    }

    public static long insert(String conflictMessage, String sql, Object... params) {
        return tx(c -> insert(c, conflictMessage, sql, params));
    }

    /** Runs the work in one transaction. Any exception rolls everything back. */
    public static <T> T tx(Function<Connection, T> work) {
        try (Connection c = DbConnection.get()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(c);
                c.commit();
                return result;
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Sql.tx", e);
        }
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) ps.setNull(i + 1, java.sql.Types.NULL);
            else if (p instanceof java.time.LocalDate d) ps.setDate(i + 1, java.sql.Date.valueOf(d));
            else ps.setObject(i + 1, p);
        }
    }
}
