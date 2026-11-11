package com.cankhata.service;

import com.cankhata.exception.BusinessRuleException;
import com.cankhata.exception.NotFoundException;
import com.cankhata.model.Money;
import com.cankhata.model.SessionUser;
import com.cankhata.util.Sql;
import com.cankhata.util.Validator;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The daily sheet: one row per customer per can type for a date. Generated from subscriptions, closed by the owner. */
public class SheetService {
    private final AuditService audit;

    public SheetService(AuditService audit) {
        this.audit = audit;
    }

    /** Creates the sheet for a day (if missing) and adds a PLANNED row for every subscription due that day. Safe to run again. */
    public long generate(SessionUser actor, LocalDate day, String ip) {
        AccountService.requireOwner(actor);
        if (day.isBefore(LocalDate.now().minusDays(30)) || day.isAfter(LocalDate.now().plusDays(7)))
            throw new BusinessRuleException("Choose a date between the last 30 days and the next 7 days");
        long[] result = Sql.tx(c -> {
            Map<String, Object> sheet = Sql.one(c, "SELECT id, status FROM daily_sheets WHERE sheet_date = ? FOR UPDATE", day);
            long sheetId;
            if (sheet == null) sheetId = Sql.insert(c, "Could not create the sheet", "INSERT INTO daily_sheets (sheet_date) VALUES (?)", day);
            else {
                if ("CLOSED".equals(sheet.get("status"))) throw new BusinessRuleException("This day is already closed");
                sheetId = ((Number) sheet.get("id")).longValue();
            }
            Set<Long> skipped = new HashSet<>();
            for (Map<String, Object> k : Sql.query(c, "SELECT subscription_id FROM skip_days WHERE skip_date = ?", day))
                skipped.add(((Number) k.get("subscription_id")).longValue());
            long added = 0;
            List<Map<String, Object>> subs = Sql.query(c, "SELECT s.id, s.customer_id, s.can_type_id, s.qty, s.frequency, s.weekday_mask, s.start_date, s.end_date, "
                    + "COALESCE(cu.rate_override_paise, ct.price_paise) AS price, r.delivery_boy_id FROM subscriptions s "
                    + "JOIN customers cu ON cu.id = s.customer_id JOIN can_types ct ON ct.id = s.can_type_id LEFT JOIN routes r ON r.id = cu.route_id "
                    + "WHERE s.active = TRUE AND cu.active = TRUE AND s.start_date <= ? AND (s.end_date IS NULL OR s.end_date >= ?)", day, day);
            for (Map<String, Object> s : subs) {
                long subId = ((Number) s.get("id")).longValue();
                LocalDate end = s.get("end_date") == null ? null : ((java.sql.Date) s.get("end_date")).toLocalDate();
                if (skipped.contains(subId)) continue;
                if (!Schedule.isDue((String) s.get("frequency"), ((Number) s.get("weekday_mask")).intValue(),
                        ((java.sql.Date) s.get("start_date")).toLocalDate(), end, day)) continue;
                added += Sql.update(c, "Could not add a delivery", "INSERT IGNORE INTO deliveries (sheet_id, customer_id, can_type_id, delivery_boy_id, planned_qty, unit_price_paise) VALUES (?,?,?,?,?,?)",
                        sheetId, s.get("customer_id"), s.get("can_type_id"), s.get("delivery_boy_id"), s.get("qty"), s.get("price"));
            }
            return new long[]{sheetId, added};
        });
        audit.log(actor.getId(), "SHEET_GENERATED", "daily_sheet", result[0], day + " added=" + result[1], ip);
        return result[0];
    }

    public Map<String, Object> find(SessionUser actor, LocalDate day) {
        AccountService.requireOwner(actor);
        return Sql.one("SELECT id, sheet_date, status, closed_at FROM daily_sheets WHERE sheet_date = ?", day);
    }

    public List<Map<String, Object>> deliveries(SessionUser actor, long sheetId) {
        AccountService.requireOwner(actor);
        List<Map<String, Object>> rows = Sql.query("SELECT d.id, d.status, d.planned_qty, d.delivered_qty, d.empties_collected, d.cash_collected_paise, d.note, "
                + "c.name AS customer_name, c.address, t.name AS can_name, COALESCE(u.name, '-') AS boy_name FROM deliveries d "
                + "JOIN customers c ON c.id = d.customer_id JOIN can_types t ON t.id = d.can_type_id LEFT JOIN users u ON u.id = d.delivery_boy_id "
                + "WHERE d.sheet_id = ? ORDER BY u.name, c.name LIMIT 1000", sheetId);
        for (Map<String, Object> r : rows) r.put("cashText", Money.text(((Number) r.get("cash_collected_paise")).longValue()));
        return rows;
    }

    /** Van check per delivery boy: cans that left full, cans that should be back, and empties that should have come in. */
    public List<Map<String, Object>> vanCheck(SessionUser actor, long sheetId) {
        AccountService.requireOwner(actor);
        List<Map<String, Object>> rows = Sql.query("SELECT u.id AS boy_id, u.name AS boy_name, COALESCE(v.full_out, 0) AS full_out, COALESCE(v.full_back, 0) AS full_back, COALESCE(v.empties_in, 0) AS empties_in, "
                + "COALESCE(SUM(d.delivered_qty), 0) AS delivered, COALESCE(SUM(d.empties_collected), 0) AS empties_collected "
                + "FROM users u LEFT JOIN van_loads v ON v.delivery_boy_id = u.id AND v.sheet_id = ? "
                + "LEFT JOIN deliveries d ON d.delivery_boy_id = u.id AND d.sheet_id = ? WHERE u.role_id = 2 AND u.active = TRUE "
                + "GROUP BY u.id, u.name, v.full_out, v.full_back, v.empties_in ORDER BY u.name", sheetId, sheetId);
        for (Map<String, Object> r : rows) {
            long fullOut = ((Number) r.get("full_out")).longValue(), back = ((Number) r.get("full_back")).longValue();
            long delivered = ((Number) r.get("delivered")).longValue();
            long emptiesIn = ((Number) r.get("empties_in")).longValue(), collected = ((Number) r.get("empties_collected")).longValue();
            r.put("fullMissing", fullOut > 0 ? fullOut - delivered - back : 0);
            r.put("emptiesMissing", collected - emptiesIn);
        }
        return rows;
    }

    public void saveVan(SessionUser actor, long sheetId, long boyId, String fullOut, String fullBack, String emptiesIn, String ip) {
        AccountService.requireOwner(actor);
        int out = Validator.intInRange(fullOut, "Full cans out", 0, 5000);
        int back = Validator.intInRange(fullBack, "Full cans back", 0, 5000);
        int in = Validator.intInRange(emptiesIn, "Empties in", 0, 5000);
        Sql.tx(c -> {
            requireOpen(c, sheetId);
            Sql.update(c, "Delivery boy or sheet not found", "INSERT INTO van_loads (sheet_id, delivery_boy_id, full_out, full_back, empties_in) VALUES (?,?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE full_out = VALUES(full_out), full_back = VALUES(full_back), empties_in = VALUES(empties_in)", sheetId, boyId, out, back, in);
            return null;
        });
        audit.log(actor.getId(), "VAN_SAVED", "daily_sheet", sheetId, "boy=" + boyId + " out=" + out + " back=" + back + " empties=" + in, ip);
    }

    public void close(SessionUser actor, long sheetId, String ip) {
        AccountService.requireOwner(actor);
        Sql.tx(c -> {
            requireOpen(c, sheetId);
            long open = Sql.number(c, "SELECT COUNT(*) FROM deliveries WHERE sheet_id = ? AND status = 'PLANNED'", sheetId);
            if (open > 0) throw new BusinessRuleException(open + " delivery(ies) are still not marked. Mark each as delivered or missed first.");
            Sql.update(c, "Could not close the sheet", "UPDATE daily_sheets SET status = 'CLOSED', closed_by = ?, closed_at = CURRENT_TIMESTAMP WHERE id = ?", actor.getId(), sheetId);
            return null;
        });
        audit.log(actor.getId(), "SHEET_CLOSED", "daily_sheet", sheetId, null, ip);
    }

    static void requireOpen(java.sql.Connection c, long sheetId) {
        Map<String, Object> s = Sql.one(c, "SELECT status FROM daily_sheets WHERE id = ? FOR UPDATE", sheetId);
        if (s == null) throw new NotFoundException("Sheet not found");
        if (!"OPEN".equals(s.get("status"))) throw new BusinessRuleException("This day is closed and cannot be changed");
    }
}
