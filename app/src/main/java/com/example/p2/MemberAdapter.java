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

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

    private List<MemberModel> members;
    public MemberAdapter(List<MemberModel> newList){
        this.members = members;
    }
    public void updateList(List<MemberModel> newList){
        this.members = newList;
        notifyDataSetChanged();
    }

    @Override
    public MemberAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_member_adapter, parent, false);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position){
        MemberModel m = members.get(position);

        holder.tvName.setText(m.username);
        holder.tvTime.setText("Used: " + m.timeUsed + " min");
        holder.tvApps.setText("Apps " + String.join(",", m.appsUsed));
    }

    @Override
    public int getItemCount(){
        return members.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvApps;

        public ViewHolder(View itemView){
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvTime = itemView.findViewById(R.id.tvMemberTime);
            tvApps = itemView.findViewById(R.id.tvMemberApps);
        }
    }
}