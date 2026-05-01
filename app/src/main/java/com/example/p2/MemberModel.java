package com.example.p2;

import java.util.Map;

public class MemberModel {
    public String username;
    public long timeUsed;
    public Map<String, Map<String, Long>> appsUsed;

    public MemberModel(String username, long timeUsed, Map<String, Map<String, Long>> appsUsed){
        this.username = username;
        this.timeUsed = timeUsed;
        this.appsUsed = appsUsed;
    }
}
