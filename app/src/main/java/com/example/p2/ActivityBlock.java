package com.example.p2;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ActivityBlock extends AppCompatActivity {

    private BlockedAppsAdapter adapter;
    private List<String> blockedAppsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnAddApps), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        // Set up the blocked apps list
        RecyclerView rv = findViewById(R.id.rvBlockedApps);
        blockedAppsList = new ArrayList<>(AppListManager.getSavedApps(this));
        adapter = new BlockedAppsAdapter(this, blockedAppsList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);


        // Toggle to start/stop the overlay service for testing
        androidx.appcompat.widget.SwitchCompat serviceToggle = findViewById(R.id.switchService);
        serviceToggle.setChecked(isServiceRunning());
        serviceToggle.setOnCheckedChangeListener((btn, isChecked) -> {
            Intent service = new Intent(this, OverlayService.class);
            if (isChecked) {
                startForegroundService(service);
            } else {
                stopService(service);
            }
        });
        // Add Apps button
        findViewById(R.id.btnAddApps).setOnClickListener(v -> {
            Intent intent = new Intent(ActivityBlock.this, ActivityAppPicker.class);
            startActivity(intent);
        });

        // Home nav button
        findViewById(R.id.tvNavHome).setOnClickListener(v -> {
            Intent intent = new Intent(ActivityBlock.this, ActivityHome.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list every time we come back from the app picker
        blockedAppsList.clear();
        blockedAppsList.addAll(AppListManager.getSavedApps(this));
        adapter.notifyDataSetChanged();
    }

    private boolean isServiceRunning() {
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (OverlayService.class.getName().equals(s.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}