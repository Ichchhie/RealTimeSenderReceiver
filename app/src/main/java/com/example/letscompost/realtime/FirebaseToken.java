package com.example.letscompost.realtime;

public class FirebaseToken {
    public long createdDate;
    public String token;
    public String userId;

    public FirebaseToken(long createdDate, String token, String userId) {
        this.createdDate = createdDate;
        this.token = token;
        this.userId = userId;
    }

    public FirebaseToken() {
    }
}
