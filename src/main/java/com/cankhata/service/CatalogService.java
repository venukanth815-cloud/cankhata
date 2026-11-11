package com.cankhata.service;

import com.cankhata.dao.CatalogDao;
import com.cankhata.exception.ForbiddenException;
import com.cankhata.model.CanType;
import com.cankhata.model.Role;
import com.cankhata.model.Route;
import com.cankhata.model.SessionUser;
import com.cankhata.util.Validator;

import java.util.List;

/** Business rules for can types and routes. Owner only. */
public class CatalogService {
    private final CatalogDao dao;
    private final AuditService audit;

    public CatalogService(CatalogDao dao, AuditService audit) {
        this.dao = dao;
        this.audit = audit;
    }

    private static void requireOwner(SessionUser actor) {
        if (actor == null || actor.getRole() != Role.OWNER) throw new ForbiddenException("Only the owner can do this");
    }

    public List<CanType> listCanTypes(SessionUser actor) {
        requireOwner(actor);
        return dao.listCanTypes();
    }

    public long addCanType(SessionUser actor, String name, String price, String deposit, String ip) {
        requireOwner(actor);
        String n = Validator.required(name, "Name", 50);
        long p = Validator.rupeesToPaise(price, "Price", false);
        long d = Validator.rupeesToPaise(deposit, "Deposit", true);
        long id = dao.insertCanType(n, p, d);
        audit.log(actor.getId(), "CAN_TYPE_CREATED", "can_type", id, n + " price=" + p + " deposit=" + d, ip);
        return id;
    }

    public void updateCanType(SessionUser actor, String id, String name, String price, String deposit, String ip) {
        requireOwner(actor);
        long cid = Validator.id(id);
        String n = Validator.required(name, "Name", 50);
        long p = Validator.rupeesToPaise(price, "Price", false);
        long d = Validator.rupeesToPaise(deposit, "Deposit", true);
        dao.updateCanType(cid, n, p, d);
        audit.log(actor.getId(), "CAN_TYPE_UPDATED", "can_type", cid, n + " price=" + p + " deposit=" + d, ip);
    }

    public List<Route> listRoutes(SessionUser actor) {
        requireOwner(actor);
        return dao.listRoutes();
    }

    public long addRoute(SessionUser actor, String name, String boyId, String ip) {
        requireOwner(actor);
        String n = Validator.required(name, "Route name", 100);
        long id = dao.insertRoute(n, Validator.optionalId(boyId));
        audit.log(actor.getId(), "ROUTE_CREATED", "route", id, n, ip);
        return id;
    }

    public void assignBoy(SessionUser actor, String routeId, String boyId, String ip) {
        requireOwner(actor);
        long rid = Validator.id(routeId);
        Long boy = Validator.optionalId(boyId);
        dao.assignBoy(rid, boy);
        audit.log(actor.getId(), "ROUTE_ASSIGNED", "route", rid, "boy=" + boy, ip);
    }
}
