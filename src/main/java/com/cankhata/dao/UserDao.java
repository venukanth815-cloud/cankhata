package com.cankhata.dao;

import com.cankhata.exception.BusinessRuleException;
import com.cankhata.exception.DataAccessException;
import com.cankhata.model.Role;
import com.cankhata.model.User;
import com.cankhata.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO = Data Access Object. The ONLY place that knows SQL for the users table.
 * Rules we follow in every DAO: PreparedStatement with ? (stops SQL injection),
 * try-with-resources (always returns the connection to the pool), no business logic.
 */
public class UserDao {
    private static final String COLS = "id, role_id, name, phone, email, password_hash, active, created_at";

    /** Login by phone OR email. */
    public Optional<User> findByLogin(String login) {
        String sql = "SELECT " + COLS + " FROM users WHERE phone = ? OR email = ? LIMIT 1";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("UserDao.findByLogin", e);
        }
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT " + COLS + " FROM users WHERE id = ?";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("UserDao.findById", e);
        }
    }

    public void updatePassword(long id, String passwordHash) {
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("UserDao.updatePassword", e);
        }
    }

    public long countAll() {
        try (Connection c = DbConnection.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DataAccessException("UserDao.countAll", e);
        }
    }

    public long count(String q) {
        boolean all = q == null || q.isBlank();
        String sql = "SELECT COUNT(*) FROM users" + (all ? "" : " WHERE name LIKE ? OR phone LIKE ? OR email LIKE ?");
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (!all) bindLike(ps, q, 1);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("UserDao.count", e);
        }
    }

    /** One page of users, newest first. LIMIT/OFFSET keeps each page small even with 10,000 users. */
    public List<User> search(String q, int limit, int offset) {
        boolean all = q == null || q.isBlank();
        String sql = "SELECT " + COLS + " FROM users"
                + (all ? "" : " WHERE name LIKE ? OR phone LIKE ? OR email LIKE ?")
                + " ORDER BY id DESC LIMIT ? OFFSET ?";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = all ? 1 : bindLike(ps, q, 1);
            ps.setInt(i, limit);
            ps.setInt(i + 1, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("UserDao.search", e);
        }
    }

    /** Active delivery boys, for the route assignment drop-down (a handful of rows). */
    public List<User> listActiveDeliveryBoys() {
        String sql = "SELECT " + COLS + " FROM users WHERE role_id = ? AND active = TRUE ORDER BY name";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Role.DELIVERY_BOY.getId());
            try (ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("UserDao.listActiveDeliveryBoys", e);
        }
    }

    public boolean existsByPhoneOrEmail(String phone, String email) {
        String sql = "SELECT 1 FROM users WHERE phone = ? OR (? IS NOT NULL AND email = ?) LIMIT 1";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, email);
            ps.setString(3, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("UserDao.exists", e);
        }
    }

    /** Returns the new id. The UNIQUE keys in MySQL are the final guard against duplicates, even under parallel requests. */
    public long insert(User u) {
        String sql = "INSERT INTO users (role_id, name, phone, email, password_hash, active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, u.getRole().getId());
            ps.setString(2, u.getName());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getEmail());
            ps.setString(5, u.getPasswordHash());
            ps.setBoolean(6, u.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BusinessRuleException("This phone number or email is already registered");
        } catch (SQLException e) {
            throw new DataAccessException("UserDao.insert", e);
        }
    }

    private static int bindLike(PreparedStatement ps, String q, int start) throws SQLException {
        String like = "%" + q.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        ps.setString(start, like);
        ps.setString(start + 1, like);
        ps.setString(start + 2, like);
        return start + 3;
    }

    private static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setRole(Role.fromId(rs.getInt("role_id")));
        u.setName(rs.getString("name"));
        u.setPhone(rs.getString("phone"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return u;
    }
}
