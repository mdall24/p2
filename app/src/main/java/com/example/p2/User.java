package com.example.p2;

public class User {
    public String username;
    public String email;
    public String uid;

    public User() {
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
    public User(String username, String email, String uid) {
        this.username = username;
        this.email = email;
        this.uid = uid;
    }
}
