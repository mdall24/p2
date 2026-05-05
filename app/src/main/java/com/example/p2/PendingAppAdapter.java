package com.example.p2;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class PendingAppAdapter extends RecyclerView.Adapter<PendingAppAdapter.PendingAppViewHolder> {

    public interface PendingAppListener {
        void onApprove(String appName);
        void onReject(String appName);
    }

    private List<Map.Entry<String, Map<String, Object>>> pendingList;
    private PendingAppListener listener;
    private int totalMembers;

    public PendingAppAdapter(List<Map.Entry<String, Map<String, Object>>> pendingList, PendingAppListener listener, int totalMembers) {
        this.pendingList = pendingList;
        this.listener = listener;
        this.totalMembers = totalMembers;
    }

    public static class PendingAppViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppName, tvProposedBy;
        Button btnApprove, btnReject;
        ImageView ivAppIcon;

        public PendingAppViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvProposedBy = itemView.findViewById(R.id.tvProposedBy);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
        }
    }

    @NonNull
    @Override
    public PendingAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_app, parent, false);
        return new PendingAppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingAppViewHolder holder, int position) {
        Map.Entry<String, Map<String, Object>> entry = pendingList.get(position);
        String appName = entry.getKey();
        Map<String, Object> data = entry.getValue();

        String proposedBy = (String) data.get("proposedBy");
        String action = (String) data.get("action");
        String packageName = (String) data.get("packageName");
        List<String> approvals = (List<String>) data.get("approvals");
        List<String> rejections = (List<String>) data.get("rejections");

        int approvalCount = approvals != null ? approvals.size() : 0;
        int rejectionCount = rejections != null ? rejections.size() : 0;
        int totalVotes = approvalCount + rejectionCount;

        holder.tvAppName.setText(action != null && action.equals("remove") ? "Remove: " + appName : appName);
        holder.tvProposedBy.setText("by " + proposedBy + " • ✓" + approvalCount + " ✗" + rejectionCount);

        // Load app icon
        if (packageName != null) {
            try {
                PackageManager pm = holder.itemView.getContext().getPackageManager();
                Drawable icon = pm.getApplicationIcon(packageName);
                holder.ivAppIcon.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                holder.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        } else {
            holder.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        boolean allVoted = totalVotes >= totalMembers;
        holder.btnApprove.setEnabled(!allVoted);
        holder.btnReject.setEnabled(!allVoted);

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(appName));
        holder.btnReject.setOnClickListener(v -> listener.onReject(appName));
    }

    @Override
    public int getItemCount() {
        return pendingList.size();
    }
}