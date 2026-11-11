package com.cankhata.dao;

import com.cankhata.exception.BusinessRuleException;
import com.cankhata.exception.DataAccessException;
import com.cankhata.exception.NotFoundException;
import com.cankhata.model.Customer;
import com.cankhata.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** SQL for customers. Lists are always paged (LIMIT/OFFSET) so 10,000+ customers stay fast. */
public class CustomerDao {
    private static final String SELECT = "SELECT c.id, c.name, c.phone, c.address, c.landmark, c.route_id, r.name AS route_name, "
            + "c.rate_override_paise, c.credit_limit_paise, c.active FROM customers c LEFT JOIN routes r ON r.id = c.route_id";
    private static final String FILTER = " WHERE (? IS NULL OR c.route_id = ?) AND (? = '' OR c.name LIKE ? OR c.phone LIKE ? OR c.address LIKE ?)";

    public long count(String q, Long routeId) {
        String sql = "SELECT COUNT(*) FROM customers c" + FILTER;
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindFilter(ps, q, routeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("CustomerDao.count", e);
        }
    }

    public List<Customer> search(String q, Long routeId, int limit, int offset) {
        String sql = SELECT + FILTER + " ORDER BY c.id DESC LIMIT ? OFFSET ?";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = bindFilter(ps, q, routeId);
            ps.setInt(i, limit);
            ps.setInt(i + 1, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Customer> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("CustomerDao.search", e);
        }
    }

    public Customer findById(long id) {
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE c.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("Customer not found");
                return map(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("CustomerDao.findById", e);
        }
    }

    public long insert(Customer cu) {
        String sql = "INSERT INTO customers (name, phone, address, landmark, route_id, rate_override_paise, credit_limit_paise, active) VALUES (?,?,?,?,?,?,?,TRUE)";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(ps, cu);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BusinessRuleException("A customer with this phone number already exists, or the route is invalid");
        } catch (SQLException e) {
            throw new DataAccessException("CustomerDao.insert", e);
        }
    }

    public void update(Customer cu) {
        String sql = "UPDATE customers SET name=?, phone=?, address=?, landmark=?, route_id=?, rate_override_paise=?, credit_limit_paise=?, active=? WHERE id=?";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindFields(ps, cu);
            ps.setBoolean(8, cu.isActive());
            ps.setLong(9, cu.getId());
            if (ps.executeUpdate() == 0) throw new NotFoundException("Customer not found");
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BusinessRuleException("A customer with this phone number already exists, or the route is invalid");
        } catch (SQLException e) {
            throw new DataAccessException("CustomerDao.update", e);
        }
    }

    private static void bindFields(PreparedStatement ps, Customer cu) throws SQLException {
        ps.setString(1, cu.getName());
        ps.setString(2, cu.getPhone());
        ps.setString(3, cu.getAddress());
        ps.setString(4, cu.getLandmark());
        if (cu.getRouteId() == null) ps.setNull(5, Types.BIGINT); else ps.setLong(5, cu.getRouteId());
        if (cu.getRateOverridePaise() == null) ps.setNull(6, Types.BIGINT); else ps.setLong(6, cu.getRateOverridePaise());
        ps.setLong(7, cu.getCreditLimitPaise());
    }

    private static int bindFilter(PreparedStatement ps, String q, Long routeId) throws SQLException {
        String text = q == null ? "" : q.trim();
        String like = "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        if (routeId == null) { ps.setNull(1, Types.BIGINT); ps.setNull(2, Types.BIGINT); }
        else { ps.setLong(1, routeId); ps.setLong(2, routeId); }
        ps.setString(3, text);
        ps.setString(4, like);
        ps.setString(5, like);
        ps.setString(6, like);
        return 7;
    }

    private static Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        c.setLandmark(rs.getString("landmark"));
        long route = rs.getLong("route_id");
        c.setRouteId(rs.wasNull() ? null : route);
        c.setRouteName(rs.getString("route_name"));
        long rate = rs.getLong("rate_override_paise");
        c.setRateOverridePaise(rs.wasNull() ? null : rate);
        c.setCreditLimitPaise(rs.getLong("credit_limit_paise"));
        c.setActive(rs.getBoolean("active"));
        return c;
    }
}
