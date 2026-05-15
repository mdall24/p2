package com.example.p2;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class JoinTeam extends AppCompatActivity {
    private Button accept, decline, addApps, changeTime;

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
        changeTime = findViewById(R.id.changeTime);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNav), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
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
        changeTime.setOnClickListener(v -> showTimePickerDialog());
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

    private void showTimePickerDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Suggest Time");

        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 40, 50, 40);

        final NumberPicker hourPicker = new NumberPicker(this);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));

        final NumberPicker minutePicker = new NumberPicker(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(5);
        final String[] displayedValues = {"00", "10", "20", "30", "40", "50"};
        minutePicker.setDisplayedValues(displayedValues);


        // Add a colon text view between pickers
        TextView colon = new TextView(this);
        colon.setText(" : ");
        colon.setTextSize(20);

        layout.addView(hourPicker);
        layout.addView(colon);
        layout.addView(minutePicker);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int hour = hourPicker.getValue();
            String minuteStr = displayedValues[minutePicker.getValue()];
            String time = String.format(Locale.getDefault(), "%02d:%s", hour, minuteStr);
            suggestedTime = (hour * 60) + Integer.parseInt(minuteStr);
            String username = new SessionManager(JoinTeam.this).getTeamCode();
            FirebaseFirestore.getInstance().collection("teams").document(teamCode)
                    .update("time", suggestedTime);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}