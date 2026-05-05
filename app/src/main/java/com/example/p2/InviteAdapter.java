package com.example.p2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.InviteViewHolder>{
    public interface InviteListener{
        void onAccept(String teamCode);
        void onDecline(String teamCode);
    }

    private List<Map<String, Object>> inviteList;
    private InviteListener listener;

    public InviteAdapter(List<Map<String, Object>> inviteList, InviteListener listener) {
        this.inviteList = inviteList;
        this.listener = listener;
    }

    public static class InviteViewHolder extends RecyclerView.ViewHolder{
        TextView tvTeamName, tvFrom;
        Button btnAccept, btnDecline;

        public InviteViewHolder(@NonNull View itemView){
            super(itemView);
            tvTeamName = itemView.findViewById(R.id.tvInviteTeamName);
            tvFrom = itemView.findViewById(R.id.tvInviteFrom);
            btnAccept = itemView.findViewById(R.id.btnAcceptInvite);
            btnDecline = itemView.findViewById(R.id.btnDeclineInvite);
        }
    }
    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invite, parent, false);
        return new InviteViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position){
        Map<String, Object> invite = inviteList.get(position);
        String teamCode = (String) invite.get("teamCode");
        String from = (String) invite.get("from");
        String teamName = (String) invite.get("teamName");

        holder.tvTeamName.setText(teamName != null ? teamName : teamCode);
        holder.tvFrom.setText("From: "+from);
        holder.btnAccept.setOnClickListener(v-> listener.onAccept(teamCode));
        holder.btnDecline.setOnClickListener(v-> listener.onDecline(teamCode));
    }
    @Override
    public int getItemCount(){
        return inviteList.size();
    }

}
