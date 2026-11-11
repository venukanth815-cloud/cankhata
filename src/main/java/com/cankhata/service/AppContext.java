package com.cankhata.service;

import com.cankhata.dao.AuditDao;
import com.cankhata.dao.CatalogDao;
import com.cankhata.dao.CustomerDao;
import com.cankhata.dao.UserDao;

/** Creates each service ONCE and shares it (e.g. the login lock counters must be shared by all requests). */
public final class AppContext {
    public static final UserDao USER_DAO = new UserDao();
    public static final AuditService AUDIT = new AuditService(new AuditDao());
    public static final AuthService AUTH = new AuthService(USER_DAO, AUDIT);

    public static final CatalogService CATALOG = new CatalogService(new CatalogDao(), AUDIT);
    public static final CustomerService CUSTOMERS = new CustomerService(new CustomerDao(), AUDIT);

    public static final AccountService ACCOUNTS = new AccountService(AUDIT);
    public static final SubscriptionService SUBSCRIPTIONS = new SubscriptionService(AUDIT, ACCOUNTS);
    public static final SheetService SHEETS = new SheetService(AUDIT);
    public static final DeliveryService DELIVERIES = new DeliveryService(AUDIT);
    public static final BillingService BILLING = new BillingService(AUDIT);
    public static final NotificationService NOTIFICATIONS = new NotificationService();

    private AppContext() {}
}
