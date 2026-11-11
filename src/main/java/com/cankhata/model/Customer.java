package com.cankhata.model;

public class Customer {
    private long id;
    private String name;
    private String phone;
    private String address;
    private String landmark;
    private Long routeId;
    private String routeName;
    private Long rateOverridePaise;
    private long creditLimitPaise;
    private boolean active;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public Long getRateOverridePaise() { return rateOverridePaise; }
    public void setRateOverridePaise(Long v) { this.rateOverridePaise = v; }
    public long getCreditLimitPaise() { return creditLimitPaise; }
    public void setCreditLimitPaise(long v) { this.creditLimitPaise = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getRateText() { return rateOverridePaise == null ? "" : Money.text(rateOverridePaise); }
    public String getCreditLimitText() { return Money.text(creditLimitPaise); }
}
