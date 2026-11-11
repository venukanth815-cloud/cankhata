package com.cankhata.service;

import com.cankhata.exception.ValidationException;
import com.cankhata.model.Money;
import com.cankhata.model.SessionUser;
import com.cankhata.util.Sql;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Monthly invoices (one per customer per month, never changed once created) and the "who owes money" list. */
public class BillingService {
    private final AuditService audit;

    public BillingService(AuditService audit) {
        this.audit = audit;
    }

    public static YearMonth parseMonth(String value) {
        try {
            YearMonth m = YearMonth.parse(value == null ? "" : value.trim());
            if (m.isAfter(YearMonth.now())) throw new ValidationException("Choose this month or an earlier month");
            return m;
        } catch (java.time.format.DateTimeParseException e) {
            throw new ValidationException("Choose a valid month");
        }
    }

    /** Creates invoices for customers who had delivered cans that month and do not have one yet. Returns how many were created. */
    public int generate(SessionUser actor, YearMonth month, String ip) {
        AccountService.requireOwner(actor);
        String ym = month.toString();
        int created = Sql.tx(c -> {
            int n = Sql.update(c, "Could not create invoices", "INSERT IGNORE INTO invoices (customer_id, invoice_month, total_paise) "
                    + "SELECT d.customer_id, ?, SUM(d.delivered_qty * d.unit_price_paise) FROM deliveries d JOIN daily_sheets s ON s.id = d.sheet_id "
                    + "WHERE d.status = 'DELIVERED' AND s.sheet_date >= ? AND s.sheet_date < ? GROUP BY d.customer_id HAVING SUM(d.delivered_qty * d.unit_price_paise) > 0",
                    ym, month.atDay(1), month.plusMonths(1).atDay(1));
            for (Map<String, Object> r : Sql.query(c, "SELECT DISTINCT customer_id FROM invoices WHERE invoice_month = ?", ym))
                AccountService.settleInvoices(c, ((Number) r.get("customer_id")).longValue());
            return n;
        });
        audit.log(actor.getId(), "INVOICES_GENERATED", "invoice", null, ym + " created=" + created, ip);
        return created;
    }

    public List<Map<String, Object>> forMonth(SessionUser actor, YearMonth month) {
        AccountService.requireOwner(actor);
        List<Map<String, Object>> rows = Sql.query("SELECT i.id, i.customer_id, i.total_paise, i.status, c.name, c.phone FROM invoices i JOIN customers c ON c.id = i.customer_id "
                + "WHERE i.invoice_month = ? ORDER BY c.name LIMIT 1000", month.toString());
        for (Map<String, Object> r : rows) r.put("totalText", Money.text(((Number) r.get("total_paise")).longValue()));
        return rows;
    }

    /** Customers who owe money, biggest first (top 100). */
    public List<Map<String, Object>> outstanding(SessionUser actor) {
        AccountService.requireOwner(actor);
        List<Map<String, Object>> rows = Sql.query("SELECT c.id, c.name, c.phone, "
                + "COALESCE((SELECT SUM(d.delivered_qty * d.unit_price_paise) FROM deliveries d WHERE d.customer_id = c.id AND d.status = 'DELIVERED'), 0) "
                + "- COALESCE((SELECT SUM(p.amount_paise) FROM payments p WHERE p.customer_id = c.id), 0) AS owed FROM customers c "
                + "HAVING owed > 0 ORDER BY owed DESC LIMIT 100");
        for (Map<String, Object> r : rows) r.put("owedText", Money.text(((Number) r.get("owed")).longValue()));
        return rows;
    }

    /** Today's numbers for the owner's home page. */
    public Map<String, Object> todayStats(SessionUser actor) {
        AccountService.requireOwner(actor);
        Map<String, Object> m = Sql.one("SELECT COALESCE(SUM(d.status = 'PLANNED'), 0) AS pending, COALESCE(SUM(d.status = 'DELIVERED'), 0) AS done, "
                + "COALESCE(SUM(d.status = 'MISSED'), 0) AS missed, COALESCE(SUM(d.cash_collected_paise), 0) AS cash FROM deliveries d "
                + "JOIN daily_sheets s ON s.id = d.sheet_id WHERE s.sheet_date = CURRENT_DATE");
        m.put("cashText", Money.text(((Number) m.get("cash")).longValue()));
        return m;
    }
}
