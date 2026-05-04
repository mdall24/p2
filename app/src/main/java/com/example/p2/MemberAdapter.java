package com.example.p2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

    private List<MemberModel> members;
    public MemberAdapter(List<MemberModel> newList){
        this.members = newList;
    }
    public void updateList(List<MemberModel> newList){
        this.members = newList;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_member_adapter, parent, false);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        MemberModel m = members.get(position);

        holder.tvName.setText(m.username);
        holder.tvTime.setText("Used: " + m.timeUsed + " min");

        //Reads daily and weekly usage from appsUsed maps in Firestore
        long totalDaily = 0;
        long totalWeekly = 0;
        StringBuilder appNames = new StringBuilder();

        if (m.appsUsed != null) {
            for (Map.Entry<String, Map<String, Long>> entry : m.appsUsed.entrySet()) {
                String appName = entry.getKey();
                Map<String, Long> times = entry.getValue();
                if (times != null) {
                    totalDaily += times.getOrDefault("daily", 0L);
                    totalWeekly += times.getOrDefault("weekly", 0L);
                }

                if (appName.length() > 0) appNames.append(", ");
                appNames.append(appName);
            }
        }

        holder.tvDaily.setText("Daily: " + totalDaily + " min");
        holder.tvWeekly.setText("Weekly: " + totalWeekly + " min");
        holder.tvApps.setText("Apps: " + (appNames.length() > 0 ? appNames : "None"));
    }

    @Override
    public int getItemCount(){
        return members != null ? members.size() : 0; //Will return 0 if members are null instead of crashing
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvDaily, tvWeekly, tvApps;

        public ViewHolder(View itemView){
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvTime = itemView.findViewById(R.id.tvMemberTime);
            tvDaily = itemView.findViewById(R.id.tvMemberDaily);
            tvWeekly = itemView.findViewById(R.id.tvMemberWeekly);
            tvApps = itemView.findViewById(R.id.tvMemberApps);
        }
    }
}