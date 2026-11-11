package com.cankhata.service;

import com.cankhata.exception.BusinessRuleException;
import com.cankhata.exception.ForbiddenException;
import com.cankhata.exception.NotFoundException;
import com.cankhata.model.Money;
import com.cankhata.model.Role;
import com.cankhata.model.SessionUser;
import com.cankhata.util.Sql;
import com.cankhata.util.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * What the delivery boy does on the road. Marking a delivery is ONE transaction:
 * status + can ledger rows + cash payment + customer notification are all saved together or not at all.
 */
public class DeliveryService {
    private final AuditService audit;

    public DeliveryService(AuditService audit) {
        this.audit = audit;
    }

    private static void requireDriver(SessionUser actor) {
        if (actor == null || (actor.getRole() != Role.DELIVERY_BOY && actor.getRole() != Role.OWNER)) throw new ForbiddenException("Delivery staff only");
    }

    /** Today's list for this delivery boy (the owner sees everybody's). */
    public List<Map<String, Object>> today(SessionUser actor) {
        requireDriver(actor);
        boolean owner = actor.getRole() == Role.OWNER;
        List<Map<String, Object>> rows = Sql.query("SELECT d.id, d.status, d.planned_qty, d.delivered_qty, d.empties_collected, d.unit_price_paise, c.name AS customer_name, c.phone, c.address, c.landmark, "
                + "t.name AS can_name, COALESCE((SELECT SUM(l.qty) FROM can_ledger l WHERE l.customer_id = d.customer_id AND l.can_type_id = d.can_type_id), 0) AS held, "
                + "COALESCE((SELECT SUM(amount_paise) FROM payments p WHERE p.customer_id = d.customer_id), 0) AS paid, "
                + "COALESCE((SELECT SUM(x.delivered_qty * x.unit_price_paise) FROM deliveries x WHERE x.customer_id = d.customer_id AND x.status = 'DELIVERED'), 0) AS billed "
                + "FROM deliveries d JOIN daily_sheets s ON s.id = d.sheet_id JOIN customers c ON c.id = d.customer_id JOIN can_types t ON t.id = d.can_type_id "
                + "WHERE s.sheet_date = CURRENT_DATE AND (? OR d.delivery_boy_id = ?) ORDER BY d.status = 'PLANNED' DESC, c.address LIMIT 500", owner, actor.getId());
        for (Map<String, Object> r : rows) {
            long due = ((Number) r.get("billed")).longValue() - ((Number) r.get("paid")).longValue();
            r.put("dueText", Money.text(due));
            r.put("dueNow", due > 0);
        }
        return rows;
    }

    public void complete(SessionUser actor, long deliveryId, String delivered, String empties, String cash, String note, String ip) {
        requireDriver(actor);
        int qty = Validator.intInRange(delivered, "Delivered cans", 0, 100);
        int back = Validator.intInRange(empties == null || empties.isBlank() ? "0" : empties, "Empty cans collected", 0, 100);
        long cashPaise = cash == null || cash.isBlank() ? 0 : Validator.rupeesToPaise(cash, "Cash collected", true);
        String cleanNote = Validator.optionalText(note, "Note", 255);
        Sql.tx(c -> {
            Map<String, Object> d = lockPlanned(c, actor, deliveryId);
            long customerId = ((Number) d.get("customer_id")).longValue();
            long typeId = ((Number) d.get("can_type_id")).longValue();
            long held = Sql.number(c, "SELECT COALESCE(SUM(qty), 0) FROM can_ledger WHERE customer_id = ? AND can_type_id = ?", customerId, typeId);
            if (back > held + qty) throw new BusinessRuleException("Customer holds only " + (held + qty) + " can(s), so " + back + " empties cannot be collected");
            if (qty == 0 && back == 0 && cashPaise == 0) throw new BusinessRuleException("Nothing to save. Use 'Missed' if you could not deliver.");
            Sql.update(c, "Could not save the delivery", "UPDATE deliveries SET status = 'DELIVERED', delivered_qty = ?, empties_collected = ?, cash_collected_paise = ?, note = ?, delivery_boy_id = COALESCE(delivery_boy_id, ?) WHERE id = ?",
                    qty, back, cashPaise, cleanNote, actor.getId(), deliveryId);
            if (qty > 0) Sql.insert(c, "Could not save the can ledger", "INSERT INTO can_ledger (customer_id, can_type_id, entry_type, qty, delivery_id, created_by) VALUES (?,?,?,?,?,?)",
                    customerId, typeId, "DELIVERED", qty, deliveryId, actor.getId());
            if (back > 0) Sql.insert(c, "Could not save the can ledger", "INSERT INTO can_ledger (customer_id, can_type_id, entry_type, qty, delivery_id, created_by) VALUES (?,?,?,?,?,?)",
                    customerId, typeId, "RETURNED", -back, deliveryId, actor.getId());
            if (cashPaise > 0) {
                Sql.insert(c, "This cash was already saved", "INSERT INTO payments (customer_id, amount_paise, method, receipt_no, request_token, received_by) VALUES (?,?,?,?,?,?)",
                        customerId, cashPaise, "CASH", AccountService.newReceiptNo(), "delivery-" + deliveryId, actor.getId());
                AccountService.settleInvoices(c, customerId);
            }
            notifyCustomer(c, customerId, "DELIVERY", qty + " can(s) delivered, " + back + " empty can(s) collected." + (cashPaise > 0 ? " Cash received: Rs " + Money.text(cashPaise) + "." : ""));
            return null;
        });
        audit.log(actor.getId(), "DELIVERY_DONE", "delivery", deliveryId, "qty=" + qty + " empties=" + back + " cash=" + cashPaise, ip);
    }

    public void markMissed(SessionUser actor, long deliveryId, String reason, String ip) {
        requireDriver(actor);
        String why = Validator.required(reason, "Reason", 255);
        Sql.tx(c -> {
            Map<String, Object> d = lockPlanned(c, actor, deliveryId);
            Sql.update(c, "Could not save", "UPDATE deliveries SET status = 'MISSED', note = ?, delivery_boy_id = COALESCE(delivery_boy_id, ?) WHERE id = ?", why, actor.getId(), deliveryId);
            notifyCustomer(c, ((Number) d.get("customer_id")).longValue(), "MISSED", "Today's delivery could not be made: " + why);
            return null;
        });
        audit.log(actor.getId(), "DELIVERY_MISSED", "delivery", deliveryId, why, ip);
    }

    /** Locks the row, checks it belongs to this delivery boy, its sheet is open, and it is still PLANNED (so it can be saved only once). */
    private static Map<String, Object> lockPlanned(java.sql.Connection c, SessionUser actor, long deliveryId) {
        Map<String, Object> d = Sql.one(c, "SELECT d.id, d.customer_id, d.can_type_id, d.delivery_boy_id, d.status, d.sheet_id FROM deliveries d WHERE d.id = ? FOR UPDATE", deliveryId);
        if (d == null) throw new NotFoundException("Delivery not found");
        if (actor.getRole() != Role.OWNER && (d.get("delivery_boy_id") == null || ((Number) d.get("delivery_boy_id")).longValue() != actor.getId()))
            throw new ForbiddenException("This delivery is not on your list");
        SheetService.requireOpen(c, ((Number) d.get("sheet_id")).longValue());
        Map<String, Object> sheet = Sql.one(c, "SELECT sheet_date FROM daily_sheets WHERE id = ?", d.get("sheet_id"));
        if (actor.getRole() != Role.OWNER && !((java.sql.Date) sheet.get("sheet_date")).toLocalDate().equals(LocalDate.now()))
            throw new BusinessRuleException("You can only update today's deliveries");
        if (!"PLANNED".equals(d.get("status"))) throw new BusinessRuleException("This delivery is already saved");
        return d;
    }

    static void notifyCustomer(java.sql.Connection c, long customerId, String type, String message) {
        Map<String, Object> cu = Sql.one(c, "SELECT user_id FROM customers WHERE id = ?", customerId);
        if (cu == null || cu.get("user_id") == null) return;
        Sql.update(c, "Could not save the notification", "INSERT INTO notifications (user_id, message, type) VALUES (?,?,?)", cu.get("user_id"), message, type);
    }
}
