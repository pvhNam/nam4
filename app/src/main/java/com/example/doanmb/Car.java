package com.example.doanmb;
import java.io.Serializable;
public class Car implements Serializable {
    private String name, price, info, type, brand;
    private int imageResId;

    public Car(String name, String price, String info, int imageResId) {
        this.name = name;
        this.price = price;
        this.info = info;
        this.brand = "";
        this.imageResId = imageResId;
    }

    public Car(String name, String price, String info, String type, int imageResId) {
        this(name, price, info, type, "", imageResId);
    }

    public Car(String name, String price, String info, String type, String brand, int imageResId) {
        this.name = name;
        this.price = price;
        this.info = info;
        this.type = type;
        this.brand = brand;
        this.imageResId = imageResId;
    }

    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getInfo() { return info; }
    public String getType() { return type; }
    public String getBrand() { return brand; }
    public int getImageResId() { return imageResId; }
}
