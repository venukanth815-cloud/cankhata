package com.cankhata.model;

/** The three kinds of users. The id matches the roles table. */
public enum Role {
    OWNER(1, "Owner"),
    DELIVERY_BOY(2, "Delivery boy"),
    CUSTOMER(3, "Customer");

    private final int id;
    private final String label;

    Role(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static Role fromId(int id) {
        for (Role r : values()) {
            if (r.id == id) return r;
        }
        throw new IllegalArgumentException("Unknown role id " + id);
    }
}
