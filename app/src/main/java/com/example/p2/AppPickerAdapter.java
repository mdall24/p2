package com.example.p2;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppPickerAdapter extends RecyclerView.Adapter<AppPickerAdapter.ViewHolder> {

    private final Context context;
    private final List<ApplicationInfo> apps;
    private final PackageManager pm;

    // Tracks which apps are currently checked
    private final Set<String> selectedPackages;

    public AppPickerAdapter(Context context, List<ApplicationInfo> apps) {
        this.context = context;
        this.apps = apps;
        this.pm = context.getPackageManager();
        // Pre-load any previously saved apps so checkboxes stay ticked
        this.selectedPackages = AppListManager.getSavedApps(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates item_app.xml for each row
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationInfo app = apps.get(position);

        holder.tvAppName.setText(pm.getApplicationLabel(app));
        holder.ivAppIcon.setImageDrawable(pm.getApplicationIcon(app));

        // Clear the listener FIRST before setting checked state
        // otherwise setting checked triggers the old app's listener
        holder.cbApp.setOnCheckedChangeListener(null);
        holder.cbApp.setChecked(selectedPackages.contains(app.packageName));

        // Now set the real listener
        holder.cbApp.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                selectedPackages.add(app.packageName);
            } else {
                selectedPackages.remove(app.packageName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public Set<String> getSelectedPackages() {
        return selectedPackages;
    }

    // ViewHolder holds references to the views in each row
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName;
        CheckBox cbApp;

        ViewHolder(View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            cbApp = itemView.findViewById(R.id.cbApp);
        }
    }
}