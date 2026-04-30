package com.example.p2;

import java.util.List;

public class MemberModel {
    public String username;
    public long timeUsed;
    public List<String> appsUsed;

    public MemberModel(String username, long timeUsed, List<String> appsUsed){
        this.username = username;
        this.timeUsed = timeUsed;
        this.appsUsed = appsUsed;
    }
}
