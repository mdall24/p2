package com.example.p2;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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

import com.google.firebase.auth.FirebaseAuth;
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
        recyclerView = findViewById(R.id.appPickerRecycler);
        doneButton = findViewById(R.id.doneButton);

        teamCode = getIntent().getStringExtra("teamCode");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<AppInfo> apps = getInstalledApps();
        appAdapter = new AppAdapter(apps);
        recyclerView.setAdapter(appAdapter);

        doneButton.setOnClickListener(v -> saveSelectedApps());
    }

    private List<AppInfo> getInstalledApps() {
        List<AppInfo> appList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String name = pm.getApplicationLabel(packageInfo).toString();
                Drawable icon = pm.getApplicationIcon(packageInfo);
                appList.add(new AppInfo(name, packageInfo.packageName, icon));
            }
        }
        return appList;
    }

    private void saveSelectedApps() {
        List<String> selectedApps = appAdapter.getSelectedPackagesList();

        FirebaseFirestore.getInstance().collection("teams").document(teamCode).update("suggestedApps", selectedApps).addOnSuccessListener(a -> {
            Toast.makeText(this, "Apps added!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
