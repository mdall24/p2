package com.example.p2;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityAppPicker extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_picker);

        RecyclerView rv = findViewById(R.id.rvAppList);
        Button btnSave = findViewById(R.id.btnSaveApps);

        // Get all launchable apps properly
        PackageManager pm = getPackageManager();

        // This gets ALL apps that have a launcher icon
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<android.content.pm.ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, 0);

        // Convert to ApplicationInfo list, filtering out our own app
        List<ApplicationInfo> launchableApps = new ArrayList<>();
        for (android.content.pm.ResolveInfo info : resolveInfoList) {
            if (!info.activityInfo.packageName.equals(getPackageName())) {
                try {
                    launchableApps.add(pm.getApplicationInfo(info.activityInfo.packageName, 0));
                } catch (PackageManager.NameNotFoundException e) {
                    // skip if app info can't be found
                }
            }
        }
        // Remove duplicate packages — same app can appear twice if it has
// multiple launcher activities
        List<ApplicationInfo> deduped = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ApplicationInfo app : launchableApps) {
            if (seen.add(app.packageName)) { // add() returns false if already present
                deduped.add(app);
            }
        }
        launchableApps = deduped;
        // Sort alphabetically by app name
        Collections.sort(launchableApps,
                (a, b) -> pm.getApplicationLabel(a).toString()
                        .compareToIgnoreCase(pm.getApplicationLabel(b).toString()));

        // Set up the RecyclerView with our adapter
        AppPickerAdapter adapter = new AppPickerAdapter(this, launchableApps);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Save button — saves selected apps and goes back to Block screen
        btnSave.setOnClickListener(v -> {
            AppListManager.saveApps(this, adapter.getSelectedPackages());
            finish();
        });
    }
}