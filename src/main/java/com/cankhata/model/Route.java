package com.cankhata.model;

/** A delivery route (a set of streets) handled by one delivery boy. */
public class Route {
    private long id;
    private String name;
    private Long deliveryBoyId;
    private String deliveryBoyName;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getDeliveryBoyId() { return deliveryBoyId; }
    public void setDeliveryBoyId(Long v) { this.deliveryBoyId = v; }
    public String getDeliveryBoyName() { return deliveryBoyName; }
    public void setDeliveryBoyName(String v) { this.deliveryBoyName = v; }
}
