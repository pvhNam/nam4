package com.example.doanmb;

import java.io.Serializable;

public class Car implements Serializable {
    private String carId, name, price, info, type, userId, imageUrl;
    private int imageResId;

    public Car() {}

    public Car(String name, String price, String info, int imageResId) {
        this.name = name;
        this.price = price;
        this.info = info;
        this.imageResId = imageResId;
    }

    public Car(String name, String price, String info, String type, int imageResId) {
        this.name = name;
        this.price = price;
        this.info = info;
        this.type = type;
        this.imageResId = imageResId;
    }

    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
}