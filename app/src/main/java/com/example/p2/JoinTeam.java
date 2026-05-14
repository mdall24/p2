package com.example.p2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class JoinTeam extends AppCompatActivity {
    private Button accept;
    private Button decline;
    private Button addApps;

    private String teamCode;
private List<String> suggestedApps;
private int suggestedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_join_team);

        accept = findViewById(R.id.acceptBtn);
        decline = findViewById(R.id.declineBtn);
        addApps = findViewById(R.id.addApps);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNav), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.tvNavHome).setOnClickListener(v -> {
            Intent intent = new Intent(JoinTeam.this, ActivityHome.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavStatistics).setOnClickListener(v -> {
            Intent intent = new Intent(JoinTeam.this, statistics.class);
            startActivity(intent);
        });
        findViewById(R.id.tvNavBlock).setOnClickListener(v -> {
            Intent intent = new Intent(JoinTeam.this, ActivityBlock.class);
            startActivity(intent);
        });
        Uri data = getIntent().getData();
        if (data != null){
            teamCode = data.getQueryParameter("team");
            String appsString = data.getQueryParameter("apps");
            String timeString = data.getQueryParameter("time");

            if (appsString != null) {
                suggestedApps = Arrays.asList(appsString.split(","));
            }
            if (timeString != null) {
                suggestedTime = Integer.parseInt(timeString);
            }
        }

        accept.setOnClickListener(v -> acceptInvite());
        decline.setOnClickListener(v -> finish());
        addApps.setOnClickListener(v -> openAppPicker());
    }
    private void acceptInvite() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("teams").document(teamCode)
                .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                .addOnSuccessListener(a -> {
                    new SessionManager(JoinTeam.this).saveTeamCode(teamCode);
                    startActivity(new Intent(JoinTeam.this, TeamPage.class));
                    finish();
                });
    }
    private void openAppPicker() {
        Intent intent = new Intent(this, AppPicker.class);
        intent.putExtra("teamCode", teamCode);
        startActivity(intent);
    }
}