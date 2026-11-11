package com.cankhata.service;

import com.cankhata.exception.BusinessRuleException;
import com.cankhata.exception.ForbiddenException;
import com.cankhata.exception.NotFoundException;
import com.cankhata.model.Money;
import com.cankhata.model.Role;
import com.cankhata.model.SessionUser;
import com.cankhata.util.PasswordUtil;
import com.cankhata.util.Sql;
import com.cankhata.util.Validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One customer's account: cans held (from the append-only can_ledger), deposit held, money owed, payments, invoices.
 * Rules enforced here (not in the screens): a customer can never hold fewer than 0 cans, a refund can never be more than
 * the deposit held, a payment is saved only once per request token (double-click safe), ledger rows are never edited.
 */
public class AccountService {
    private final AuditService audit;

    public AccountService(AuditService audit) {
        this.audit = audit;
    }

    static void requireOwner(SessionUser actor) {
        if (actor == null || actor.getRole() != Role.OWNER) throw new ForbiddenException("Only the owner can do this");
    }

    /** The customer id linked to a logged-in customer user. */
    public long customerIdOf(SessionUser actor) {
        if (actor == null || actor.getRole() != Role.CUSTOMER) throw new ForbiddenException("Customers only");
        Map<String, Object> r = Sql.one("SELECT id FROM customers WHERE user_id = ?", actor.getId());
        if (r == null) throw new NotFoundException("Your customer account is not linked yet. Please ask the owner.");
        return ((Number) r.get("id")).longValue();
    }

    /** Owner may see any customer; a customer only their own. */
    public void requireAccess(SessionUser actor, long customerId) {
        if (actor == null) throw new ForbiddenException("Login required");
        if (actor.getRole() == Role.OWNER) return;
        if (actor.getRole() == Role.CUSTOMER && customerIdOf(actor) == customerId) return;
        throw new ForbiddenException("You cannot see this account");
    }

    public Map<String, Object> summary(SessionUser actor, long customerId) {
        requireAccess(actor, customerId);
        Map<String, Object> cu = Sql.one("SELECT c.id, c.name, c.phone, c.address, c.landmark, c.active, c.user_id, r.name AS route_name "
                + "FROM customers c LEFT JOIN routes r ON r.id = c.route_id WHERE c.id = ?", customerId);
        if (cu == null) throw new NotFoundException("Customer not found");
        long billed = Sql.number("SELECT COALESCE(SUM(delivered_qty * unit_price_paise), 0) FROM deliveries WHERE customer_id = ? AND status = 'DELIVERED'", customerId);
        long paid = Sql.number("SELECT COALESCE(SUM(amount_paise), 0) FROM payments WHERE customer_id = ?", customerId);
        long depIn = Sql.number("SELECT COALESCE(SUM(amount_paise), 0) FROM deposit_txns WHERE customer_id = ? AND txn_type = 'COLLECTED'", customerId);
        long depOut = Sql.number("SELECT COALESCE(SUM(amount_paise), 0) FROM deposit_txns WHERE customer_id = ? AND txn_type = 'REFUNDED'", customerId);
        long balance = billed - paid;
        cu.put("balancePaise", balance);
        cu.put("balanceText", Money.text(balance));
        cu.put("depositPaise", depIn - depOut);
        cu.put("depositText", Money.text(depIn - depOut));
        return cu;
    }

    public List<Map<String, Object>> cansHeld(SessionUser actor, long customerId) {
        requireAccess(actor, customerId);
        return Sql.query("SELECT t.id AS can_type_id, t.name, COALESCE(SUM(l.qty), 0) AS held FROM can_types t "
                + "LEFT JOIN can_ledger l ON l.can_type_id = t.id AND l.customer_id = ? GROUP BY t.id, t.name ORDER BY t.name", customerId);
    }

    public List<Map<String, Object>> recentLedger(SessionUser actor, long customerId) {
        requireAccess(actor, customerId);
        return Sql.query("SELECT l.entry_type, l.qty, l.note, l.created_at, t.name AS can_name FROM can_ledger l "
                + "JOIN can_types t ON t.id = l.can_type_id WHERE l.customer_id = ? ORDER BY l.id DESC LIMIT 15", customerId);
    }

    public List<Map<String, Object>> recentPayments(SessionUser actor, long customerId) {
        requireAccess(actor, customerId);
        List<Map<String, Object>> rows = Sql.query("SELECT amount_paise, method, receipt_no, paid_at FROM payments WHERE customer_id = ? ORDER BY id DESC LIMIT 10", customerId);
        for (Map<String, Object> r : rows) r.put("amountText", Money.text(((Number) r.get("amount_paise")).longValue()));
        return rows;
    }

    public List<Map<String, Object>> recentDeliveries(SessionUser actor, long customerId) {
        requireAccess(actor, customerId);
        List<Map<String, Object>> rows = Sql.query("SELECT s.sheet_date, d.status, d.delivered_qty, d.empties_collected, t.name AS can_name FROM deliveries d "
                + "JOIN daily_sheets s ON s.id = d.sheet_id JOIN can_types t ON t.id = d.can_type_id "
                + "WHERE d.customer_id = ? ORDER BY s.sheet_date DESC, d.id DESC LIMIT 15", customerId);
        return rows;
    }

    public List<Map<String, Object>> invoices(SessionUser actor, long customerId) {
        requireAccess(actor, customerId);
        List<Map<String, Object>> rows = Sql.query("SELECT invoice_month, total_paise, status FROM invoices WHERE customer_id = ? ORDER BY invoice_month DESC LIMIT 12", customerId);
        for (Map<String, Object> r : rows) r.put("totalText", Money.text(((Number) r.get("total_paise")).longValue()));
        return rows;
    }

    public void recordDeposit(SessionUser actor, long customerId, String type, String amount, String cans, String ip) {
        requireOwner(actor);
        boolean refund = "REFUNDED".equals(type);
        if (!refund && !"COLLECTED".equals(type)) throw new com.cankhata.exception.ValidationException("Choose collected or refunded");
        long paise = Validator.rupeesToPaise(amount, "Amount", false);
        int n = Validator.intInRange(cans, "Cans", 0, 1000);
        Sql.tx(c -> {
            if (Sql.number(c, "SELECT COUNT(*) FROM customers WHERE id = ?", customerId) == 0) throw new NotFoundException("Customer not found");
            if (refund) {
                // lock this customer's deposit rows so two refunds at the same time cannot both pass the check
                Sql.query(c, "SELECT id FROM customers WHERE id = ? FOR UPDATE", customerId);
                long held = Sql.number(c, "SELECT COALESCE(SUM(CASE WHEN txn_type = 'COLLECTED' THEN amount_paise ELSE -amount_paise END), 0) FROM deposit_txns WHERE customer_id = ?", customerId);
                if (paise > held) throw new BusinessRuleException("Refund is more than the deposit held (Rs " + Money.text(held) + ")");
            }
            Sql.insert(c, "Could not save the deposit", "INSERT INTO deposit_txns (customer_id, txn_type, amount_paise, cans, created_by) VALUES (?,?,?,?,?)",
                    customerId, type, paise, n, actor.getId());
            return null;
        });
        audit.log(actor.getId(), refund ? "DEPOSIT_REFUNDED" : "DEPOSIT_COLLECTED", "customer", customerId, "paise=" + paise + " cans=" + n, ip);
    }

    /** Owner fixes the can count: LOST / DAMAGED reduce the cans a customer holds; ADJUSTMENT can go either way (with a note). */
    public void adjustCans(SessionUser actor, long customerId, long canTypeId, String entryType, String qty, String note, String ip) {
        requireOwner(actor);
        if (!List.of("LOST", "DAMAGED", "ADJUSTMENT").contains(entryType)) throw new com.cankhata.exception.ValidationException("Choose a valid entry type");
        int q = Validator.intInRange(qty, "Quantity", entryType.equals("ADJUSTMENT") ? -1000 : 1, 1000);
        if (q == 0) throw new com.cankhata.exception.ValidationException("Quantity cannot be 0");
        int signed = entryType.equals("ADJUSTMENT") ? q : -q;
        String clean = Validator.required(note, "Note", 255);
        Sql.tx(c -> {
            Sql.query(c, "SELECT id FROM customers WHERE id = ? FOR UPDATE", customerId);
            long held = Sql.number(c, "SELECT COALESCE(SUM(qty), 0) FROM can_ledger WHERE customer_id = ? AND can_type_id = ?", customerId, canTypeId);
            if (held + signed < 0) throw new BusinessRuleException("The customer holds only " + held + " can(s) of this type");
            Sql.insert(c, "Customer or can type not found", "INSERT INTO can_ledger (customer_id, can_type_id, entry_type, qty, note, created_by) VALUES (?,?,?,?,?,?)",
                    customerId, canTypeId, entryType, signed, clean, actor.getId());
            return null;
        });
        audit.log(actor.getId(), "CAN_" + entryType, "customer", customerId, "qty=" + signed + " " + clean, ip);
    }

    /** Saves a payment once per request token; saving the same form twice (double-click) is quietly ignored. */
    public void recordPayment(SessionUser actor, long customerId, String amount, String method, String token, String ip) {
        requireOwner(actor);
        long paise = Validator.rupeesToPaise(amount, "Amount", false);
        if (!"CASH".equals(method) && !"UPI".equals(method)) throw new com.cankhata.exception.ValidationException("Choose cash or UPI");
        if (token == null || token.isBlank() || token.length() > 64) throw new com.cankhata.exception.ValidationException("Please reload the page and try again");
        boolean saved = Sql.tx(c -> {
            if (Sql.number(c, "SELECT COUNT(*) FROM payments WHERE request_token = ?", token) > 0) return false;
            if (Sql.number(c, "SELECT COUNT(*) FROM customers WHERE id = ?", customerId) == 0) throw new NotFoundException("Customer not found");
            Sql.insert(c, "This payment was already saved", "INSERT INTO payments (customer_id, amount_paise, method, receipt_no, request_token, received_by) VALUES (?,?,?,?,?,?)",
                    customerId, paise, method, newReceiptNo(), token, actor.getId());
            settleInvoices(c, customerId);
            return true;
        });
        if (saved) audit.log(actor.getId(), "PAYMENT_RECEIVED", "customer", customerId, method + " paise=" + paise, ip);
    }

    /** Owner gives a customer a login so they can see their own cans and bills. */
    public void createCustomerLogin(SessionUser actor, long customerId, String password, String ip) {
        requireOwner(actor);
        Validator.password(password);
        String hash = PasswordUtil.hash(password);
        long userId = Sql.tx(c -> {
            Map<String, Object> cu = Sql.one(c, "SELECT name, phone, user_id FROM customers WHERE id = ? FOR UPDATE", customerId);
            if (cu == null) throw new NotFoundException("Customer not found");
            if (cu.get("user_id") != null) throw new BusinessRuleException("This customer already has a login");
            long uid = Sql.insert(c, "A user with this phone number already exists", "INSERT INTO users (role_id, name, phone, password_hash, active) VALUES (?,?,?,?,TRUE)",
                    Role.CUSTOMER.getId(), cu.get("name"), cu.get("phone"), hash);
            Sql.update(c, "Could not link the login", "UPDATE customers SET user_id = ? WHERE id = ?", uid, customerId);
            return uid;
        });
        audit.log(actor.getId(), "CUSTOMER_LOGIN_CREATED", "customer", customerId, "user=" + userId, ip);
    }

    /** Owner resets a customer's login password (customer forgot it). */
    public void resetCustomerPassword(SessionUser actor, long customerId, String password, String ip) {
        requireOwner(actor);
        Validator.password(password);
        Map<String, Object> cu = Sql.one("SELECT user_id FROM customers WHERE id = ?", customerId);
        if (cu == null) throw new NotFoundException("Customer not found");
        if (cu.get("user_id") == null) throw new BusinessRuleException("This customer has no login yet");
        Sql.update("Could not reset the password", "UPDATE users SET password_hash = ? WHERE id = ?",
                PasswordUtil.hash(password), cu.get("user_id"));
        audit.log(actor.getId(), "PASSWORD_RESET_BY_OWNER", "customer", customerId, "user=" + cu.get("user_id"), ip);
    }

    public static String newReceiptNo() {
        return "R" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** Marks invoices PAID oldest-first while total payments cover them; later ones stay OPEN. */
    static void settleInvoices(java.sql.Connection c, long customerId) {
        long paid = Sql.number(c, "SELECT COALESCE(SUM(amount_paise), 0) FROM payments WHERE customer_id = ?", customerId);
        long covered = 0;
        for (Map<String, Object> inv : Sql.query(c, "SELECT id, total_paise FROM invoices WHERE customer_id = ? ORDER BY invoice_month", customerId)) {
            covered += ((Number) inv.get("total_paise")).longValue();
            Sql.update(c, "Could not update the invoice", "UPDATE invoices SET status = ? WHERE id = ?", paid >= covered ? "PAID" : "OPEN", inv.get("id"));
        }
    }
}
