package com.example.p2;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;

public class JoinTeam extends AppCompatActivity {
Button accept = findViewById(R.id.acceptBtn);
Button decline = findViewById(R.id.declineBtn);
Button addApps = findViewById(R.id.addApps);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_join_team);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Uri data = getIntent().getData();
        if (data != null){
            String teamCode = data.getQueryParameter("team");
            String apps = data.getQueryParameter("apps");
            String time = data.getQueryParameter("time");

            suggestedApps = Arrays.asList(appsString.split(","));
            suggestedTime = Integer.parseInt(timeString);

        }

        accept.setOnClickListener(v -> acceptInvite());
        decline.setOnClickListener(v -> finish());
        addApps.setOnClickListener(v -> openAppPicker());

        private void acceptInvite() {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseFirestore.getInstance().collection("teams").document(teamCode).collection("members").document(uid).set(new MemberStatus("accepted", suggestedApps, suggestedTime)).addOnSuccessListener(a -> finish());
        }
    }
}