package com.example.p2;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AppPicker extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AppAdapter appAdapter;
    private Button doneButton;
    private String teamCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_picker);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        recyclerView = findViewById(R.id.rvAppList);
        doneButton = findViewById(R.id.btnSaveApps);

        teamCode = getIntent().getStringExtra("teamCode");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<AppInfo> apps = getInstalledApps();
        appAdapter = new AppAdapter(apps);
        recyclerView.setAdapter(appAdapter);

        doneButton.setOnClickListener(v -> returnSelectedApps());
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

    private void returnSelectedApps() {
        List<String> selectedApps = appAdapter.getSelectedPackagesList();

        FirebaseFirestore.getInstance().collection("teams").document(teamCode).update("suggestedApps", selectedApps).addOnSuccessListener(a -> {
            Toast.makeText(this, "Apps added!", Toast.LENGTH_SHORT).show();

           Intent resultIntent = new Intent();
           resultIntent.putStringArrayListExtra("selectedApps", new ArrayList<>(selectedApps));
           setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
