package com.example.p2;

public class User {
    public String username;
    public String email;

    public User() {
        // Required for Firestore
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
