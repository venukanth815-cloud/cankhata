package com.cankhata.service;

import com.cankhata.dao.CustomerDao;
import com.cankhata.exception.ForbiddenException;
import com.cankhata.model.Customer;
import com.cankhata.model.Role;
import com.cankhata.model.SessionUser;
import com.cankhata.util.Validator;

import java.util.List;

/** Business rules for customers. Owner only in this release. */
public class CustomerService {
    private final CustomerDao dao;
    private final AuditService audit;

    public CustomerService(CustomerDao dao, AuditService audit) {
        this.dao = dao;
        this.audit = audit;
    }

    private static void requireOwner(SessionUser actor) {
        if (actor == null || actor.getRole() != Role.OWNER) throw new ForbiddenException("Only the owner can manage customers");
    }

    public long count(SessionUser actor, String q, Long routeId) {
        requireOwner(actor);
        return dao.count(q, routeId);
    }

    public List<Customer> list(SessionUser actor, String q, Long routeId, int page, int size) {
        requireOwner(actor);
        return dao.search(q, routeId, size, (page - 1) * size);
    }

    public Customer get(SessionUser actor, long id) {
        requireOwner(actor);
        return dao.findById(id);
    }

    public long create(SessionUser actor, Customer form, String rate, String credit, String ip) {
        requireOwner(actor);
        Customer c = clean(form, rate, credit);
        long id = dao.insert(c);
        audit.log(actor.getId(), "CUSTOMER_CREATED", "customer", id, c.getPhone(), ip);
        return id;
    }

    public void update(SessionUser actor, Customer form, String rate, String credit, String ip) {
        requireOwner(actor);
        Customer c = clean(form, rate, credit);
        c.setId(form.getId());
        c.setActive(form.isActive());
        dao.update(c);
        audit.log(actor.getId(), "CUSTOMER_UPDATED", "customer", c.getId(), c.getPhone() + (c.isActive() ? "" : " INACTIVE"), ip);
    }

    /** Validates every field of the raw form values and returns a clean Customer. */
    static Customer clean(Customer in, String rate, String credit) {
        Customer c = new Customer();
        c.setName(Validator.name(in.getName()));
        c.setPhone(Validator.phone(in.getPhone()));
        c.setAddress(Validator.required(in.getAddress(), "Address", 255));
        c.setLandmark(Validator.optionalText(in.getLandmark(), "Landmark", 150));
        c.setRouteId(in.getRouteId());
        boolean hasRate = rate != null && !rate.isBlank();
        c.setRateOverridePaise(hasRate ? Validator.rupeesToPaise(rate, "Special rate", false) : null);
        c.setCreditLimitPaise(credit == null || credit.isBlank() ? 0 : Validator.rupeesToPaise(credit, "Credit limit", true));
        c.setActive(true);
        return c;
    }
}
