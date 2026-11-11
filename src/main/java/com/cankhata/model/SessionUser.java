package com.cankhata.model;

/** Who is logged in. Kept small because it is stored inside the signed cookie. */
public class SessionUser {
    private final long id;
    private final String name;
    private final Role role;

    public SessionUser(long id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }

    // Handy for JSP: ${user.owner}
    public boolean isOwner() { return role == Role.OWNER; }
    public boolean isDeliveryBoy() { return role == Role.DELIVERY_BOY; }
    public boolean isCustomer() { return role == Role.CUSTOMER; }
}
