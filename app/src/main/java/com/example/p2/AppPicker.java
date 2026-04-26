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

import java.util.List;

public class AppPicker extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AppAdapter appAdapter;
    private Button doneButton;

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
    private void saveSelectedApps() {
        List<String> selectedApps = appAdapter.getSelectedPackagesList();

        FirebaseFirestore.getInstance().collection("teams").document(teamCode).update("suggestedApps", selectedApps).addSuccessListener(a -> {
            Toast.makeText(this, "Apps added!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}