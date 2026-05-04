package com.example.p2;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CreateTeam extends AppCompatActivity {
    private final String teamCode = generateTeamCode();

    private List<String> selectedApps = new ArrayList<>();

    private final ActivityResultLauncher<Intent> appPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->{
        if(result.getResultCode() == RESULT_OK && result.getData() != null){
            selectedApps = result.getData().getStringArrayListExtra("selectedApps");
        }
    });


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

        Button sendInvite = findViewById(R.id.SendInvite);
        FloatingActionButton suggestTimeBtn = findViewById(R.id.SuggestedTimeButton);
        FloatingActionButton addAppsBtn = findViewById(R.id.AddAppButton);

        addAppsBtn.setOnClickListener(v-> {
            addAppsBtn.setVisibility(View.GONE);
            suggestTimeBtn.setVisibility(View.GONE);

            Intent intent = new Intent(CreateTeam.this, AppPicker.class);
            intent.putExtra("teamCode", teamCode);
            appPickerLauncher.launch(intent);
        });

        suggestTimeBtn.setOnClickListener(v ->{

            TimePickerDialog timePicker = new TimePickerDialog(
                    CreateTeam.this, (view, hourOfDay, minute) -> {
                        int totalMinutes = hourOfDay * 60 + minute;
                    },
                    0, 0, true
                    );
            timePicker.show();
        });

        sendInvite.setOnClickListener(View -> {
            suggestTimeBtn.hide();
            addAppsBtn.hide();

            String username = new SessionManager(CreateTeam.this).getUsername();
            SessionManager session = new SessionManager(CreateTeam.this);
            session.saveTeamCode(teamCode);

            FirebaseFirestore.getInstance().collection("usernames").document(username)
                    .update("teamCode", teamCode);

            String appsString = String.join(",", selectedApps);
            int suggestedTime = 60;

            String deepLink = "myapp://join"
                    + "?team=" + teamCode
                    + "&apps=" + appsString
                    + "&time=" + suggestedTime;

            String inviteMessage ="Hey! Join my team against screen time!\n" + "Tap to join: " + deepLink;


            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, inviteMessage);

            startActivity(Intent.createChooser(intent, "Send invite via"));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        FloatingActionButton suggestTimeBtn = findViewById(R.id.SuggestedTimeButton);
        FloatingActionButton addAppsBtn = findViewById(R.id.AddAppButton);
        suggestTimeBtn.show();
        addAppsBtn.show();
    }

    private String generateTeamCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }
}