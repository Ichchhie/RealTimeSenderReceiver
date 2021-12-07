package com.example.letscompost.realtime;

public class LocationData {
    public long createdDate;
    public double latitude,longitude;
    public String userId;

    public LocationData(long createdDate, double latitude, double longitude, String userId) {
        this.createdDate = createdDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.userId = userId;
    }

    public LocationData() {
    }
}
