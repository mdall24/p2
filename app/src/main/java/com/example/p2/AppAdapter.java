package com.example.p2;

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
import com.example.p2.AppInfo;
import com.example.p2.R;
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {
    private List<AppInfo> apps;
    public AppAdapter(List<AppInfo> apps){
        this.apps = apps;
    }
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = apps.get(position);

        holder.appName.setText(app.name);
        holder.appIcon.setImageDrawable(app.icon);
        holder.appCheck.setChecked(app.isSelected);

        holder.appCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            app.isSelected = isChecked;
        });
    }
    @Override
    public int getItemCount(){
        return apps.size();
    }

    public List<String> getSelectedPackages() {
        List<String> selected = new ArrayList<>();
        for (AppInfo app : apps) {
            if (app.isSelected){
                selected.add(app.packageName);
            }
        }
        return selected;
    }
    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        CheckBox appCheck;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appCheck = itemView.findViewById(R.id.appCheck);
        }
    }
}
