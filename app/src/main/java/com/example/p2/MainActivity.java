package com.example.p2;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private Button btnCreateAcc;
    private TextView okay, cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(MainActivity.this, ActivityHome.class));
                finish();
            } else {
                session.logout();
                startActivity(new Intent(MainActivity.this, Login.class));
                finish();
            }
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button loginBtn = findViewById(R.id.Login);
        loginBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Login.class));
        });

        btnCreateAcc = findViewById(R.id.createAcc);
        btnCreateAcc.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Registration.class);
            startActivity(intent);
        });

        //Creates a dialog box (with use of XML) that informs the user that it needs access to system info
        SharedPreferences prefs = getSharedPreferences("appPrefs", MODE_PRIVATE);
        boolean hasSeenIntro = prefs.getBoolean("hasSeenIntro", false);

        if (!hasSeenIntro) {
            //Uses the XML created
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null);
            AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView)
                    .setCancelable(false)
                    .create();

            TextView okayBtn = dialogView.findViewById(R.id.okay_text);
            TextView cancelBtn = dialogView.findViewById(R.id.cancel_text);

            okayBtn.setOnClickListener(v -> {
                prefs.edit().putBoolean("hasSeenIntro", true).apply();
                dialog.dismiss();
            });

            cancelBtn.setOnClickListener(v -> {
                dialog.dismiss();
                finish(); //Closes the app since they declined
            });

            dialog.show();

        }
    }
}
