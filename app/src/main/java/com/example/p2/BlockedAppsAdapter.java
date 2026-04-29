package com.example.p2;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BlockedAppsAdapter extends RecyclerView.Adapter<BlockedAppsAdapter.ViewHolder> {

    private final Context context;
    private final List<String> packageNames;
    private final PackageManager pm;

    public BlockedAppsAdapter(Context context, List<String> packageNames) {
        this.context = context;
        this.packageNames = packageNames;
        this.pm = context.getPackageManager();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String packageName = packageNames.get(position);

        try {
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            holder.tvAppName.setText(pm.getApplicationLabel(info));
            holder.ivAppIcon.setImageDrawable(pm.getApplicationIcon(info));
        } catch (PackageManager.NameNotFoundException e) {
            holder.tvAppName.setText(packageName);
        }

        // Placeholder avg use — we'll wire this to real data later
        holder.tvAvgUse.setText("Avg. daily use: --");

        // Switch toggles the block on/off — turning off removes from list
        holder.switchBlock.setOnCheckedChangeListener(null);
        holder.switchBlock.setChecked(true);
        holder.switchBlock.setOnCheckedChangeListener((btn, isChecked) -> {
            if (!isChecked) {
                // Remove from list and save
                int pos = holder.getAdapterPosition();
                packageNames.remove(pos);
                AppListManager.saveApps(context, new java.util.HashSet<>(packageNames));
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, packageNames.size());
            }
        });

        // Load saved state for this app
        boolean isSoft = AppListManager.isSoftBlocked(context, packageName);
        if (isSoft) {
            holder.tvSoft.setBackgroundResource(R.drawable.toggle_selected);
            holder.tvSoft.setTextColor(0xFFFFFFFF);
            holder.tvHard.setBackground(null);
            holder.tvHard.setTextColor(0xFFAABBCC);
        } else {
            holder.tvHard.setBackgroundResource(R.drawable.toggle_selected);
            holder.tvHard.setTextColor(0xFFFFFFFF);
            holder.tvSoft.setBackground(null);
            holder.tvSoft.setTextColor(0xFFAABBCC);
        }

// Hard click — save as hard block
        holder.tvHard.setOnClickListener(v -> {
            AppListManager.setSoftBlock(context, packageName, false);
            holder.tvHard.setBackgroundResource(R.drawable.toggle_selected);
            holder.tvHard.setTextColor(0xFFFFFFFF);
            holder.tvSoft.setBackground(null);
            holder.tvSoft.setTextColor(0xFFAABBCC);
        });

// Soft click — save as soft block
        holder.tvSoft.setOnClickListener(v -> {
            AppListManager.setSoftBlock(context, packageName, true);
            holder.tvSoft.setBackgroundResource(R.drawable.toggle_selected);
            holder.tvSoft.setTextColor(0xFFFFFFFF);
            holder.tvHard.setBackground(null);
            holder.tvHard.setTextColor(0xFFAABBCC);
        });
    }

    @Override
    public int getItemCount() {
        return packageNames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName;
        TextView tvAvgUse;
        Switch switchBlock;
        TextView tvHard;
        TextView tvSoft;

        ViewHolder(View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvAvgUse = itemView.findViewById(R.id.tvAvgUse);
            switchBlock = itemView.findViewById(R.id.switchBlock);
            tvHard = itemView.findViewById(R.id.tvHard);
            tvSoft = itemView.findViewById(R.id.tvSoft);
        }
    }
}