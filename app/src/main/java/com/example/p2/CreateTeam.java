package com.example.p2;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CreateTeam extends AppCompatActivity {
    private AppAdapter appAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_team);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
            });
        findViewById(R.id.tvNavHome).setOnClickListener(v -> {
            Intent intent = new Intent(CreateTeam.this, ActivityHome.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavStatistics).setOnClickListener(v -> {
            Intent intent = new Intent(CreateTeam.this, statistics.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavBlock).setOnClickListener(v -> {
            Intent intent = new Intent(CreateTeam.this, ActivityBlock.class);
            startActivity(intent);
        });

        RecyclerView recycler = findViewById(R.id.appRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        List<AppInfo> apps = getInstalledApps();
        appAdapter = new AppAdapter(apps);
        recycler.setAdapter(appAdapter);

        Button sendInvite = findViewById(R.id.SendInvite);
        sendInvite.setOnClickListener(View -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            String inviteMessage ="Hey! Join my team against screen time!";

            intent.putExtra(Intent.EXTRA_TEXT, inviteMessage);

            Intent chooser = Intent.createChooser(intent, "Send invite via");
            startActivity(chooser);
        });
    }

    private List<AppInfo> getInstalledApps() {
        List<AppInfo> appList = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(intent, 0);

        for (ResolveInfo info : resolveInfos) {
            String appName = info.loadLabel(getPackageManager()).toString();
            String packageName = info.activityInfo.packageName;
            Drawable icon = info.loadIcon(getPackageManager());

            appList.add(new AppInfo(appName, packageName, icon));
        }
        return appList;
    }
}