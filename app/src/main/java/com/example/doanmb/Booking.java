package com.example.doanmb;

import java.io.Serializable;

public class Booking implements Serializable {
    private String bookingId, carId, carName, renterId, renterPhone,
            renterName, ownerId, startDate, endDate, status;
    private long totalDays;
    private String totalPrice;

    public Booking() {}

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }
    public String getCarName() { return carName; }
    public void setCarName(String carName) { this.carName = carName; }
    public String getRenterId() { return renterId; }
    public void setRenterId(String renterId) { this.renterId = renterId; }
    public String getRenterPhone() { return renterPhone; }
    public void setRenterPhone(String renterPhone) { this.renterPhone = renterPhone; }
    public String getRenterName() { return renterName; }
    public void setRenterName(String renterName) { this.renterName = renterName; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTotalDays() { return totalDays; }
    public void setTotalDays(long totalDays) { this.totalDays = totalDays; }
    public String getTotalPrice() { return totalPrice; }
    public void setTotalPrice(String totalPrice) { this.totalPrice = totalPrice; }
}