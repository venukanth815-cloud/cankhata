package com.cankhata.dao;

import com.cankhata.exception.DataAccessException;
import com.cankhata.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AuditDao {

    public void insert(Long userId, String action, String entity, Long entityId, String detail, String ip) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity, entity_id, detail, ip) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, Types.BIGINT); else ps.setLong(1, userId);
            ps.setString(2, action);
            ps.setString(3, entity);
            if (entityId == null) ps.setNull(4, Types.BIGINT); else ps.setLong(4, entityId);
            ps.setString(5, detail == null ? null : (detail.length() > 500 ? detail.substring(0, 500) : detail));
            ps.setString(6, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("AuditDao.insert", e);
        }
    }
}
