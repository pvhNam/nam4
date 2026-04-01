package com.example.doanmb;

public class Car {
    private String name;
    private String price;
    private String info;
    private int imageResId;

    public Car(String name, String price, String info, int imageResId) {
        this.name = name;
        this.price = price;
        this.info = info;
        this.imageResId = imageResId;
    }

    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getInfo() { return info; }
    public int getImageResId() { return imageResId; }
}