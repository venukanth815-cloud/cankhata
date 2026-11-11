package com.cankhata.model;

/** A kind of water can (e.g. 20 Litre). Prices are in paise. */
public class CanType {
    private long id;
    private String name;
    private long pricePaise;
    private long depositPaise;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getPricePaise() { return pricePaise; }
    public void setPricePaise(long v) { this.pricePaise = v; }
    public long getDepositPaise() { return depositPaise; }
    public void setDepositPaise(long v) { this.depositPaise = v; }
    /** For JSP display: 4000 paise -> 40.00 */
    public String getPriceText() { return Money.text(pricePaise); }
    public String getDepositText() { return Money.text(depositPaise); }
}
