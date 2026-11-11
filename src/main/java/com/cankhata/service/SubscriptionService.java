package com.cankhata.service;

import com.cankhata.exception.BusinessRuleException;
import com.cankhata.exception.ForbiddenException;
import com.cankhata.exception.NotFoundException;
import com.cankhata.exception.ValidationException;
import com.cankhata.model.Role;
import com.cankhata.model.SessionUser;
import com.cankhata.util.Sql;
import com.cankhata.util.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Standing orders ("2 cans daily") and one-day skips. Owner manages all; a customer may skip only their own, from tomorrow on. */
public class SubscriptionService {
    private final AuditService audit;
    private final AccountService accounts;

    public SubscriptionService(AuditService audit, AccountService accounts) {
        this.audit = audit;
        this.accounts = accounts;
    }

    public List<Map<String, Object>> list(SessionUser actor, long customerId) {
        accounts.requireAccess(actor, customerId);
        List<Map<String, Object>> subs = Sql.query("SELECT s.id, s.qty, s.frequency, s.weekday_mask, s.start_date, s.end_date, s.active, t.name AS can_name "
                + "FROM subscriptions s JOIN can_types t ON t.id = s.can_type_id WHERE s.customer_id = ? ORDER BY s.active DESC, s.id DESC", customerId);
        for (Map<String, Object> s : subs) {
            s.put("skips", Sql.query("SELECT id, skip_date FROM skip_days WHERE subscription_id = ? AND skip_date >= CURRENT_DATE ORDER BY skip_date LIMIT 10", s.get("id")));
        }
        return subs;
    }

    public long add(SessionUser actor, long customerId, String canTypeId, String qty, String frequency, String[] days, String start, String end, String ip) {
        AccountService.requireOwner(actor);
        long type = Validator.id(canTypeId);
        int q = Validator.intInRange(qty, "Quantity", 1, 100);
        if (!List.of("DAILY", "ALTERNATE", "WEEKDAYS").contains(frequency)) throw new ValidationException("Choose how often");
        LocalDate from = Validator.date(start, "Start date");
        LocalDate to = end == null || end.isBlank() ? null : Validator.date(end, "End date");
        if (to != null && to.isBefore(from)) throw new ValidationException("End date is before the start date");
        int mask = "WEEKDAYS".equals(frequency) ? Schedule.maskFrom(days) : Schedule.ALL_DAYS;
        long id = Sql.tx(c -> {
            if (Sql.number(c, "SELECT COUNT(*) FROM subscriptions WHERE customer_id = ? AND can_type_id = ? AND active = TRUE", customerId, type) > 0)
                throw new BusinessRuleException("This customer already has an active subscription for this can type. End it first.");
            return Sql.insert(c, "Customer or can type not found", "INSERT INTO subscriptions (customer_id, can_type_id, qty, frequency, weekday_mask, start_date, end_date, active) VALUES (?,?,?,?,?,?,?,TRUE)",
                    customerId, type, q, frequency, mask, from, to);
        });
        audit.log(actor.getId(), "SUBSCRIPTION_CREATED", "subscription", id, "customer=" + customerId + " qty=" + q + " " + frequency, ip);
        return id;
    }

    public void end(SessionUser actor, long customerId, long subscriptionId, String ip) {
        AccountService.requireOwner(actor);
        int n = Sql.update("Could not end the subscription", "UPDATE subscriptions SET active = FALSE, end_date = CURRENT_DATE WHERE id = ? AND customer_id = ?", subscriptionId, customerId);
        if (n == 0) throw new NotFoundException("Subscription not found");
        audit.log(actor.getId(), "SUBSCRIPTION_ENDED", "subscription", subscriptionId, null, ip);
    }

    public void skip(SessionUser actor, long customerId, long subscriptionId, String date, String ip) {
        accounts.requireAccess(actor, customerId);
        LocalDate day = Validator.date(date, "Date");
        LocalDate earliest = actor.getRole() == Role.OWNER ? LocalDate.now() : LocalDate.now().plusDays(1);
        if (day.isBefore(earliest)) throw new BusinessRuleException(actor.getRole() == Role.OWNER ? "Choose today or a later date" : "You can skip from tomorrow onwards");
        if (day.isAfter(LocalDate.now().plusDays(90))) throw new ValidationException("Choose a date within the next 90 days");
        Sql.tx(c -> {
            if (Sql.number(c, "SELECT COUNT(*) FROM subscriptions WHERE id = ? AND customer_id = ? AND active = TRUE", subscriptionId, customerId) == 0)
                throw new NotFoundException("Subscription not found");
            // a sheet that already exists for that day and is CLOSED can no longer change
            if (Sql.number(c, "SELECT COUNT(*) FROM daily_sheets WHERE sheet_date = ? AND status = 'CLOSED'", day) > 0)
                throw new BusinessRuleException("That day is already closed");
            Sql.update(c, "Already skipped for that day", "INSERT INTO skip_days (subscription_id, skip_date, created_by) VALUES (?,?,?)", subscriptionId, day, actor.getId());
            // if today's/that day's sheet was already generated and nothing was delivered yet, remove the planned row
            Sql.update(c, "Could not update the sheet", "DELETE d FROM deliveries d JOIN daily_sheets s ON s.id = d.sheet_id JOIN subscriptions sub ON sub.customer_id = d.customer_id AND sub.can_type_id = d.can_type_id "
                    + "WHERE sub.id = ? AND s.sheet_date = ? AND d.status = 'PLANNED'", subscriptionId, day);
            return null;
        });
        audit.log(actor.getId(), "SKIP_ADDED", "subscription", subscriptionId, day.toString(), ip);
    }

    public void unskip(SessionUser actor, long customerId, long skipId, String ip) {
        accounts.requireAccess(actor, customerId);
        LocalDate earliest = actor.getRole() == Role.OWNER ? LocalDate.now() : LocalDate.now().plusDays(1);
        int n = Sql.update("Could not remove the skip", "DELETE k FROM skip_days k JOIN subscriptions s ON s.id = k.subscription_id WHERE k.id = ? AND s.customer_id = ? AND k.skip_date >= ?",
                skipId, customerId, earliest);
        if (n == 0) throw new ForbiddenException("That skip cannot be removed");
        audit.log(actor.getId(), "SKIP_REMOVED", "skip", skipId, null, ip);
    }
}
