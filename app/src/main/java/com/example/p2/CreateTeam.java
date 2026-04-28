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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

        RecyclerView recycler = findViewById(R.id.appRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        List<AppInfo> apps = getInstalledApps();
        appAdapter = new AppAdapter(apps);
        recycler.setAdapter(appAdapter);

        Button sendInvite = findViewById(R.id.SendInvite);
        FloatingActionButton suggestTimeBtn = findViewById(R.id.SuggestedTimeButton);
        FloatingActionButton addAppsBtn = findViewById(R.id.AddAppButton);

        sendInvite.setOnClickListener(View -> {
            suggestTimeBtn.hide();
            addAppsBtn.hide();

            String teamCode = "ABC123";
            String selectedApps = appAdapter.getSelectedPackageNames();
            int suggestedTime = 60;

            String deepLink = "myapp://join"
                    + "?team=" + teamCode
                    + "&apps=" + selectedApps
                    + "&time=" + suggestedTime;

            String inviteMessage ="Hey! Join my team against screen time!\n" + "Tap to join: " + deepLink;


            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, inviteMessage);

            startActivity(Intent.createChooser(intent, "Send invite via"));
        });
    }
    @Override
    protected void onResume(){
        super.onResume();
        FloatingActionButton suggestTimeBtn = findViewById(R.id.SuggestedTimeButton);
        FloatingActionButton addAppsBtn = findViewById(R.id.AddAppButton);
        suggestTimeBtn.show();
        addAppsBtn.show();
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