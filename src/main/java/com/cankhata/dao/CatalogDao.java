package com.cankhata.dao;

import com.cankhata.exception.BusinessRuleException;
import com.cankhata.exception.DataAccessException;
import com.cankhata.exception.NotFoundException;
import com.cankhata.model.CanType;
import com.cankhata.model.Route;
import com.cankhata.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL for the two small lookup tables: can_types and routes. They stay tiny, so no paging is needed. */
public class CatalogDao {

    public List<CanType> listCanTypes() {
        try (Connection c = DbConnection.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, price_paise, deposit_paise FROM can_types ORDER BY name")) {
            List<CanType> list = new ArrayList<>();
            while (rs.next()) {
                CanType t = new CanType();
                t.setId(rs.getLong("id"));
                t.setName(rs.getString("name"));
                t.setPricePaise(rs.getLong("price_paise"));
                t.setDepositPaise(rs.getLong("deposit_paise"));
                list.add(t);
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("CatalogDao.listCanTypes", e);
        }
    }

    public long insertCanType(String name, long pricePaise, long depositPaise) {
        String sql = "INSERT INTO can_types (name, price_paise, deposit_paise) VALUES (?, ?, ?)";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setLong(2, pricePaise);
            ps.setLong(3, depositPaise);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BusinessRuleException("A can type with this name already exists");
        } catch (SQLException e) {
            throw new DataAccessException("CatalogDao.insertCanType", e);
        }
    }

    public void updateCanType(long id, String name, long pricePaise, long depositPaise) {
        String sql = "UPDATE can_types SET name = ?, price_paise = ?, deposit_paise = ? WHERE id = ?";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setLong(2, pricePaise);
            ps.setLong(3, depositPaise);
            ps.setLong(4, id);
            if (ps.executeUpdate() == 0) throw new NotFoundException("Can type not found");
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BusinessRuleException("A can type with this name already exists");
        } catch (SQLException e) {
            throw new DataAccessException("CatalogDao.updateCanType", e);
        }
    }

    public List<Route> listRoutes() {
        String sql = "SELECT r.id, r.name, r.delivery_boy_id, u.name AS boy FROM routes r LEFT JOIN users u ON u.id = r.delivery_boy_id ORDER BY r.name";
        try (Connection c = DbConnection.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Route> list = new ArrayList<>();
            while (rs.next()) {
                Route r = new Route();
                r.setId(rs.getLong("id"));
                r.setName(rs.getString("name"));
                long boy = rs.getLong("delivery_boy_id");
                r.setDeliveryBoyId(rs.wasNull() ? null : boy);
                r.setDeliveryBoyName(rs.getString("boy"));
                list.add(r);
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("CatalogDao.listRoutes", e);
        }
    }

    public long insertRoute(String name, Long boyId) {
        String sql = "INSERT INTO routes (name, delivery_boy_id) VALUES (?, ?)";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            if (boyId == null) ps.setNull(2, java.sql.Types.BIGINT); else ps.setLong(2, boyId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BusinessRuleException("A route with this name already exists");
        } catch (SQLException e) {
            throw new DataAccessException("CatalogDao.insertRoute", e);
        }
    }

    public void assignBoy(long routeId, Long boyId) {
        String sql = "UPDATE routes SET delivery_boy_id = ? WHERE id = ?";
        try (Connection c = DbConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (boyId == null) ps.setNull(1, java.sql.Types.BIGINT); else ps.setLong(1, boyId);
            ps.setLong(2, routeId);
            if (ps.executeUpdate() == 0) throw new NotFoundException("Route not found");
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new BusinessRuleException("That delivery boy does not exist");
        } catch (SQLException e) {
            throw new DataAccessException("CatalogDao.assignBoy", e);
        }
    }
}
