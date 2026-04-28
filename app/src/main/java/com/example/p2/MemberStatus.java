package com.example.p2;

import java.util.List;

    public class MemberStatus {
        public String status;
        public List<String> apps;
        public int time;

        public MemberStatus(){}

        public MemberStatus(String status, List<String> apps, int time){
            this.status = status;
            this.apps = apps;
            this.time = time;
        }
    }
