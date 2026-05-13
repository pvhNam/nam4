package com.example.doanmb;

import java.io.Serializable;

public class Car implements Serializable {
    private String id;        // Document ID trên Firestore
    private String name;
    private String price;
    private String info;
    private String type;      // "sale" hoặc "rental"
    private String sellerId;  // UID người đăng
    private int imageResId;

    public Car(String name, String price, String info, int imageResId) {
        this.name = name;
        this.price = price;
        this.info = info;
        this.imageResId = imageResId;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getInfo() { return info; }
    public String getType() { return type; }
    public String getSellerId() { return sellerId; }
    public int getImageResId() { return imageResId; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
}