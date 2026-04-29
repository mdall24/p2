package com.example.p2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private Button btnCreateAcc;
    private Button btnStatistics;
    private Button btnProfile;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if(session.isLoggedIn()){
            startActivity(new Intent(MainActivity.this, ActivityHome.class));
            finish();
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /*
        Button btnGoToHome = findViewById(R.id.btnGoToHome);
        btnGoToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ActivityHome.class);
                startActivity(intent);
            }
        });
        */

        btnCreateAcc = findViewById(R.id.createAcc);
        btnCreateAcc.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Registration.class);
            startActivity(intent);
        });

        btnLogin = findViewById(R.id.Login);
        btnLogin.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
        });

        btnStatistics = findViewById(R.id.statsButton);
        btnStatistics.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, statistics.class);
            startActivity(intent);
        });

        btnProfile = findViewById(R.id.profilebutton);
        btnProfile.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Profile.class);
            startActivity(intent);
        });
    }
}
